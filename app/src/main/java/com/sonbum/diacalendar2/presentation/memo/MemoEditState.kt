package com.sonbum.diacalendar2.presentation.memo

import android.net.Uri
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class ReminderType(val label: String, val minutesBefore: Long) {
    RELATIVE_10MIN("10분 전", 10),
    RELATIVE_30MIN("30분 전", 30),
    RELATIVE_1HOUR("1시간 전", 60),
    RELATIVE_1DAY("1일 전", 1440),
    CUSTOM("직접 설정", -1)
}

data class MemoEditState(
    val memoId: String? = null,
    val title: String = "",
    val content: String = "",
    val hexColorString: String = "#4CAF50",
    val startTime: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val endTime: LocalTime = LocalTime.now().plusHours(1).withSecond(0).withNano(0),
    val startDate: LocalDate = LocalDate.now(),  // 시작일
    val endDate: LocalDate = LocalDate.now(),    // 종료일 (연속 일정용)
    val isMultipleDays: Boolean = false,         // 연속 일정 여부
    val isAllDay: Boolean = false,               // 종일 여부
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderType: ReminderType = ReminderType.RELATIVE_30MIN,
    val customReminderDateTime: LocalDateTime? = null,
    val imagePath: String? = null,
    val selectedImageUri: Uri? = null
) {
    // 선택된 날짜 수
    val dayCount: Int
        get() = if (isMultipleDays) {
            (endDate.toEpochDay() - startDate.toEpochDay() + 1).toInt()
        } else {
            1
        }
}

sealed interface MemoEditAction {
    data class OnTitleChange(val title: String) : MemoEditAction
    data class OnContentChange(val content: String) : MemoEditAction
    data class OnColorChange(val hexColor: String) : MemoEditAction
    data class OnStartTimeChange(val time: LocalTime) : MemoEditAction
    data class OnEndTimeChange(val time: LocalTime) : MemoEditAction
    data class OnEndDateChange(val date: LocalDate) : MemoEditAction
    data class OnMultipleDaysToggle(val enabled: Boolean) : MemoEditAction
    data class OnAllDayToggle(val enabled: Boolean) : MemoEditAction
    data class OnReminderToggle(val enabled: Boolean) : MemoEditAction
    data class OnReminderTypeChange(val type: ReminderType) : MemoEditAction
    data class OnCustomReminderDateTimeChange(val dateTime: LocalDateTime) : MemoEditAction
    data class OnImageSelected(val uri: Uri) : MemoEditAction
    data object OnImageRemove : MemoEditAction
    data object OnSave : MemoEditAction
    data object OnDelete : MemoEditAction
}

sealed interface MemoEditEvent {
    data object SaveSuccess : MemoEditEvent
    data object DeleteSuccess : MemoEditEvent
    data class Error(val message: String) : MemoEditEvent
}
