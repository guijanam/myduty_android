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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ProfileState(
    /** 지금까지 로드된 메모(최신순). 스크롤에 따라 페이지 단위로 누적된다. */
    val memos: List<Memo> = emptyList(),
    /** 메모가 존재하는 연도 목록(내림차순) */
    val memoYears: List<Int> = emptyList(),
    val selectedYear: Int? = LocalDate.now().year,
    val searchQuery: String = "",
    /** 다음 페이지가 더 있는지 여부 */
    val hasMoreMemos: Boolean = true,
    /** 다음 페이지 로딩 중 여부 */
    val isLoadingMoreMemos: Boolean = false,
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

    /** 메모 페이지 로딩 Job (필터 변경 시 이전 로딩을 취소하기 위함) */
    private var memoLoadJob: Job? = null

    /** 검색어 디바운스 Job */
    private var searchJob: Job? = null

    init {
        observeMemoYears()
        reloadMemos()
        loadVacationRecords()
        loadChatNotes()
    }

    /** 연도 드롭다운 목록 구독 (날짜 문자열만 조회하므로 가볍다) */
    private fun observeMemoYears() {
        viewModelScope.launch {
            memoRepository.getMemoYears().collect { years ->
                _state.update { it.copy(memoYears = years) }
            }
        }
    }

    /** 필터(연도/검색어) 기준으로 첫 페이지부터 다시 로드 */
    private fun reloadMemos() {
        memoLoadJob?.cancel()
        memoLoadJob = viewModelScope.launch {
            val current = _state.value
            _state.update {
                it.copy(
                    memos = emptyList(),
                    hasMoreMemos = true,
                    isLoading = true
                )
            }

            val page = memoRepository.getMemosPaged(
                year = current.selectedYear,
                query = current.searchQuery,
                limit = MEMO_PAGE_SIZE,
                offset = 0
            )

            _state.update {
                it.copy(
                    memos = page,
                    hasMoreMemos = page.size == MEMO_PAGE_SIZE,
                    isLoading = false
                )
            }
        }
    }

    /** 리스트 끝에 도달했을 때 다음 페이지를 이어붙인다 */
    fun loadMoreMemos() {
        val current = _state.value
        if (!current.hasMoreMemos || current.isLoadingMoreMemos || current.isLoading) return

        memoLoadJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMoreMemos = true) }

            val page = memoRepository.getMemosPaged(
                year = current.selectedYear,
                query = current.searchQuery,
                limit = MEMO_PAGE_SIZE,
                offset = current.memos.size
            )

            _state.update {
                it.copy(
                    memos = it.memos + page,
                    hasMoreMemos = page.size == MEMO_PAGE_SIZE,
                    isLoadingMoreMemos = false
                )
            }
        }
    }

    fun onYearSelected(year: Int?) {
        if (_state.value.selectedYear == year) return
        _state.update { it.copy(selectedYear = year) }
        reloadMemos()
    }

    fun onSearchQueryChange(query: String) {
        if (_state.value.searchQuery == query) return
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            reloadMemos()
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
            // 페이징은 Flow 구독이 아니므로 삭제 후 목록에서 직접 제거한다.
            _state.update { current ->
                current.copy(memos = current.memos.filterNot { it.objectId == memo.objectId })
            }
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

    companion object {
        /** 메모 내역 한 페이지 크기 */
        private const val MEMO_PAGE_SIZE = 50

        /** 검색어 입력 디바운스 (ms) */
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
