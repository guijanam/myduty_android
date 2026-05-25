package com.sonbum.diacalendar2.presentation.memo

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.core.notification.AlarmScheduler
import com.sonbum.diacalendar2.core.util.ImageUtils
import com.sonbum.diacalendar2.domain.model.Memo
import com.sonbum.diacalendar2.domain.repository.MemoRepository
import com.sonbum.diacalendar2.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class MemoEditViewModel(
    private val memoRepository: MemoRepository,
    private val alarmScheduler: AlarmScheduler,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(MemoEditState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<MemoEditEvent>()
    val event = _event.asSharedFlow()

    fun initialize(dateString: String, memoId: String?) {
        val date = LocalDate.parse(dateString)
        _state.update { it.copy(startDate = date, endDate = date) }

        if (memoId != null) {
            loadMemo(memoId)
        }
    }

    private fun loadMemo(memoId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val memo = memoRepository.getMemoById(memoId)
            if (memo != null) {
                _state.update {
                    it.copy(
                        memoId = memo.objectId,
                        title = memo.title,
                        content = memo.content,
                        hexColorString = memo.hexColorString,
                        startTime = memo.startTime,
                        endTime = memo.endTime,
                        startDate = memo.date,
                        endDate = memo.date,
                        isMultipleDays = false,  // 편집 시에는 단일 날짜로 표시
                        isAllDay = memo.isAllDay,
                        isLoading = false,
                        reminderEnabled = memo.reminderEnabled,
                        reminderType = if (memo.reminderEnabled && memo.reminderTimeMillis != null) {
                            inferReminderType(memo)
                        } else {
                            ReminderType.RELATIVE_30MIN
                        },
                        customReminderDateTime = if (memo.reminderEnabled && memo.reminderTimeMillis != null) {
                            java.time.Instant.ofEpochMilli(memo.reminderTimeMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                        } else null,
                        imagePath = memo.imagePath
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun inferReminderType(memo: Memo): ReminderType {
        if (memo.reminderTimeMillis == null) return ReminderType.RELATIVE_30MIN

        val memoStartMillis = memo.date.atTime(memo.startTime)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val diffMinutes = (memoStartMillis - memo.reminderTimeMillis) / 60000

        return when (diffMinutes) {
            10L -> ReminderType.RELATIVE_10MIN
            30L -> ReminderType.RELATIVE_30MIN
            60L -> ReminderType.RELATIVE_1HOUR
            1440L -> ReminderType.RELATIVE_1DAY
            else -> ReminderType.CUSTOM
        }
    }

    fun onAction(action: MemoEditAction) {
        when (action) {
            is MemoEditAction.OnTitleChange -> {
                _state.update { it.copy(title = action.title) }
            }
            is MemoEditAction.OnContentChange -> {
                _state.update { it.copy(content = action.content) }
            }
            is MemoEditAction.OnColorChange -> {
                _state.update { it.copy(hexColorString = action.hexColor) }
            }
            is MemoEditAction.OnStartTimeChange -> {
                _state.update { it.copy(startTime = action.time) }
            }
            is MemoEditAction.OnEndTimeChange -> {
                _state.update { it.copy(endTime = action.time) }
            }
            is MemoEditAction.OnEndDateChange -> {
                // 종료일이 시작일보다 이전이면 시작일로 설정
                val newEndDate = if (action.date.isBefore(_state.value.startDate)) {
                    _state.value.startDate
                } else {
                    action.date
                }
                _state.update { it.copy(endDate = newEndDate) }
            }
            is MemoEditAction.OnMultipleDaysToggle -> {
                _state.update {
                    it.copy(
                        isMultipleDays = action.enabled,
                        endDate = if (action.enabled) it.startDate else it.startDate
                    )
                }
            }
            is MemoEditAction.OnAllDayToggle -> {
                _state.update { it.copy(isAllDay = action.enabled) }
            }
            is MemoEditAction.OnReminderToggle -> {
                _state.update { it.copy(reminderEnabled = action.enabled) }
            }
            is MemoEditAction.OnReminderTypeChange -> {
                _state.update { it.copy(reminderType = action.type) }
            }
            is MemoEditAction.OnCustomReminderDateTimeChange -> {
                _state.update { it.copy(customReminderDateTime = action.dateTime) }
            }
            is MemoEditAction.OnImageSelected -> {
                _state.update { it.copy(selectedImageUri = action.uri) }
            }
            MemoEditAction.OnImageRemove -> {
                // 기존 저장된 이미지가 있으면 삭제
                _state.value.imagePath?.let { ImageUtils.deleteImage(it) }
                _state.update { it.copy(imagePath = null, selectedImageUri = null) }
            }
            MemoEditAction.OnSave -> saveMemo()
            MemoEditAction.OnDelete -> deleteMemo()
        }
    }

    private fun calculateReminderTimeMillis(state: MemoEditState, date: LocalDate): Long? {
        if (!state.reminderEnabled) return null

        return when (state.reminderType) {
            ReminderType.CUSTOM -> {
                state.customReminderDateTime?.atZone(ZoneId.systemDefault())
                    ?.toInstant()?.toEpochMilli()
            }
            else -> {
                val memoDateTime = date.atTime(state.startTime)
                val reminderDateTime = memoDateTime.minusMinutes(state.reminderType.minutesBefore)
                reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
    }

    private fun saveMemo() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.title.isBlank()) {
                _event.emit(MemoEditEvent.Error("제목을 입력해주세요"))
                return@launch
            }

            _state.update { it.copy(isLoading = true) }

            try {
                // 이미지 처리: 새로 선택한 URI가 있으면 내부 저장소에 복사
                val imagePath = if (currentState.selectedImageUri != null) {
                    // 기존 이미지 삭제
                    currentState.imagePath?.let { ImageUtils.deleteImage(it) }
                    ImageUtils.copyImageToInternalStorage(appContext, currentState.selectedImageUri)
                } else {
                    currentState.imagePath
                }

                if (currentState.memoId != null) {
                    // 기존 메모 수정 (단일 날짜)
                    val reminderMillis = calculateReminderTimeMillis(currentState, currentState.startDate)
                    val memo = Memo(
                        objectId = currentState.memoId,
                        title = currentState.title,
                        content = currentState.content,
                        hexColorString = currentState.hexColorString,
                        startTime = currentState.startTime,
                        endTime = currentState.endTime,
                        date = currentState.startDate,
                        position = System.currentTimeMillis(),
                        reminderEnabled = currentState.reminderEnabled,
                        reminderTimeMillis = reminderMillis,
                        imagePath = imagePath,
                        isAllDay = currentState.isAllDay
                    )
                    memoRepository.updateMemo(memo)
                    scheduleOrCancelAlarm(memo)
                } else {
                    // 새 메모 생성 (연속 일정 지원)
                    val dates = if (currentState.isMultipleDays) {
                        generateDateRange(currentState.startDate, currentState.endDate)
                    } else {
                        listOf(currentState.startDate)
                    }

                    val basePosition = System.currentTimeMillis()
                    dates.forEachIndexed { index, date ->
                        val reminderMillis = calculateReminderTimeMillis(currentState, date)
                        // 연속 일정: 첫 번째 날짜만 원본 이미지, 나머지는 복사본 생성
                        val memoImagePath = if (index == 0) {
                            imagePath
                        } else if (imagePath != null && currentState.selectedImageUri != null) {
                            ImageUtils.copyImageToInternalStorage(appContext, currentState.selectedImageUri)
                        } else {
                            imagePath
                        }
                        val memo = Memo(
                            objectId = UUID.randomUUID().toString(),
                            title = currentState.title,
                            content = currentState.content,
                            hexColorString = currentState.hexColorString,
                            startTime = currentState.startTime,
                            endTime = currentState.endTime,
                            date = date,
                            position = basePosition + index,
                            reminderEnabled = currentState.reminderEnabled,
                            reminderTimeMillis = reminderMillis,
                            imagePath = memoImagePath,
                            isAllDay = currentState.isAllDay
                        )
                        memoRepository.insertMemo(memo)
                        scheduleOrCancelAlarm(memo)
                    }
                }

                _state.update { it.copy(isLoading = false, isSaved = true) }
                WidgetUpdater.updateAll(appContext)
                _event.emit(MemoEditEvent.SaveSuccess)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _event.emit(MemoEditEvent.Error("저장 실패: ${e.message}"))
            }
        }
    }

    private fun scheduleOrCancelAlarm(memo: Memo) {
        if (memo.reminderEnabled && memo.reminderTimeMillis != null) {
            alarmScheduler.scheduleMemoAlarm(
                memoId = memo.objectId,
                title = memo.title,
                content = memo.content,
                dateString = memo.date.toString(),
                triggerAtMillis = memo.reminderTimeMillis
            )
        } else {
            alarmScheduler.cancelMemoAlarm(memo.objectId)
        }
    }

    private fun generateDateRange(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start
        while (!current.isAfter(end)) {
            dates.add(current)
            current = current.plusDays(1)
        }
        return dates
    }

    private fun deleteMemo() {
        viewModelScope.launch {
            val memoId = _state.value.memoId ?: return@launch
            _state.update { it.copy(isLoading = true) }
            try {
                // 이미지 파일 삭제
                _state.value.imagePath?.let { ImageUtils.deleteImage(it) }
                alarmScheduler.cancelMemoAlarm(memoId)
                memoRepository.deleteMemoById(memoId)
                WidgetUpdater.updateAll(appContext)
                _event.emit(MemoEditEvent.DeleteSuccess)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _event.emit(MemoEditEvent.Error("삭제 실패: ${e.message}"))
            }
        }
    }
}
