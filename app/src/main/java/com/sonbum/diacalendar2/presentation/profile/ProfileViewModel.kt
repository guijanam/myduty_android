package com.sonbum.diacalendar2.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.core.util.DeviceIdProvider
import com.sonbum.diacalendar2.core.util.ImageUtils
import com.sonbum.diacalendar2.domain.model.ChatNote
import com.sonbum.diacalendar2.domain.model.Memo
import com.sonbum.diacalendar2.domain.model.VacationRecord
import com.sonbum.diacalendar2.domain.model.VacationType
import com.sonbum.diacalendar2.domain.repository.ChatNoteRepository
import com.sonbum.diacalendar2.domain.repository.MemoRepository
import com.sonbum.diacalendar2.domain.repository.SubscriptionRepository
import com.sonbum.diacalendar2.domain.repository.VacationRecordRepository
import com.sonbum.diacalendar2.domain.repository.VacationTypeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProfileState(
    val memosByDate: Map<LocalDate, List<Memo>> = emptyMap(),
    val vacationsByType: Map<String, List<VacationRecord>> = emptyMap(),
    val vacationTypesByName: Map<String, VacationType> = emptyMap(),
    /** 다년도 근태의 전체 기간 누적 사용량 (typeName → totalUsedCount) */
    val vacationTotalUsed: Map<String, Int> = emptyMap(),
    val chatNotes: List<ChatNote> = emptyList(),
    val isLoading: Boolean = true,
    val isVacationLoading: Boolean = true,
    val isChatNotesLoading: Boolean = true,
    val isVipRefreshing: Boolean = false
)

sealed interface ProfileEvent {
    data class VipRefreshResult(val isVip: Boolean) : ProfileEvent
}

class ProfileViewModel(
    private val memoRepository: MemoRepository,
    private val vacationRecordRepository: VacationRecordRepository,
    private val vacationTypeRepository: VacationTypeRepository,
    private val chatNoteRepository: ChatNoteRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadAllMemos()
        loadVacationRecords()
        loadChatNotes()
    }

    private fun loadAllMemos() {
        viewModelScope.launch {
            memoRepository.getAllMemos().collect { memos ->
                // 날짜별로 그룹화하고, 날짜 내림차순으로 정렬
                val grouped = memos
                    .groupBy { it.date }
                    .toSortedMap(compareByDescending { it })

                _state.update {
                    it.copy(
                        memosByDate = grouped,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadVacationRecords() {
        viewModelScope.launch {
            combine(
                vacationRecordRepository.getAllRecords(),
                vacationTypeRepository.getAllVacationTypes()
            ) { records, types -> records to types }
                .collect { (records, types) ->
                    val grouped = records
                        .sortedByDescending { it.date }
                        .groupBy { it.vacationName }
                    val typesByName = types.associateBy { it.name }

                    // 다년도 근태: 전체 기간(발생일~소멸일)의 누적 사용량 계산
                    val totalUsed = grouped.mapValues { (typeName, allRecords) ->
                        val type = typesByName[typeName]
                        if (type != null && type.isMultiYear) {
                            // 발생일~소멸일 사이의 모든 레코드 카운트 (날짜 문자열 비교)
                            allRecords.count { record ->
                                val dateStr = record.date.toString() // "YYYY-MM-DD"
                                dateStr >= type.grantDate && dateStr <= type.expiryDate
                            }
                        } else {
                            0 // 일반 근태는 연도별 필터를 UI에서 처리
                        }
                    }

                    _state.update {
                        it.copy(
                            vacationsByType = grouped,
                            vacationTypesByName = typesByName,
                            vacationTotalUsed = totalUsed,
                            isVacationLoading = false
                        )
                    }
                }
        }
    }

    fun deleteMemo(memo: Memo) {
        viewModelScope.launch {
            memoRepository.deleteMemo(memo)
        }
    }

    fun deleteVacationRecord(record: VacationRecord) {
        viewModelScope.launch {
            vacationRecordRepository.deleteByDate(record.date)
        }
    }

    private fun loadChatNotes() {
        viewModelScope.launch {
            chatNoteRepository.getAllNotes().collect { notes ->
                _state.update {
                    it.copy(chatNotes = notes, isChatNotesLoading = false)
                }
            }
        }
    }

    fun sendChatNote(content: String, imageUri: Uri? = null) {
        if (content.isBlank() && imageUri == null) return
        viewModelScope.launch {
            val imagePath = imageUri?.let {
                ImageUtils.copyImageToInternalStorage(appContext, it)
            }
            chatNoteRepository.insertNote(
                ChatNote(content = content.trim(), imagePath = imagePath)
            )
        }
    }

    fun updateChatNote(note: ChatNote, newContent: String) {
        if (newContent.isBlank()) return
        viewModelScope.launch {
            chatNoteRepository.updateNote(note.copy(content = newContent.trim()))
        }
    }

    fun deleteChatNote(note: ChatNote) {
        viewModelScope.launch {
            // 이미지 파일 삭제
            note.imagePath?.let { ImageUtils.deleteImage(it) }
            chatNoteRepository.deleteNote(note)
        }
    }

    fun onRefreshVipStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isVipRefreshing = true) }
            val ssaid = DeviceIdProvider.getSsaid(appContext)
            val isVip = subscriptionRepository.refreshVipStatus(ssaid)
            _state.update { it.copy(isVipRefreshing = false) }
            _events.send(ProfileEvent.VipRefreshResult(isVip))
        }
    }
}
