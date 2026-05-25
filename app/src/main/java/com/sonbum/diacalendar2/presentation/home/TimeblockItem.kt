package com.sonbum.diacalendar2.presentation.home

import androidx.compose.ui.graphics.Color
import com.sonbum.diacalendar2.domain.model.CalendarEvent
import com.sonbum.diacalendar2.domain.model.Memo
import java.time.LocalDate

enum class TimeblockItemType { MEMO, CALENDAR_EVENT }

data class TimeblockItem(
    val id: String,
    val title: String,
    val startMinutes: Int,   // 0~1439
    val endMinutes: Int,     // 0~1440
    val color: Color,
    val type: TimeblockItemType,
    val originalMemo: Memo? = null,
    val originalEvent: CalendarEvent? = null
)

fun CalendarEvent.toTimeblockItem(date: LocalDate): TimeblockItem? {
    if (isAllDay) return null

    val startMin = if (startTime.toLocalDate() < date) {
        0
    } else {
        startTime.hour * 60 + startTime.minute
    }

    val endMin = if (endTime.toLocalDate() > date) {
        1440
    } else {
        (endTime.hour * 60 + endTime.minute).coerceAtLeast(startMin + 15)
    }

    return TimeblockItem(
        id = "event_$id",
        title = title,
        startMinutes = startMin,
        endMinutes = endMin.coerceAtMost(1440),
        color = Color(color),
        type = TimeblockItemType.CALENDAR_EVENT,
        originalEvent = this
    )
}

fun Memo.toTimeblockItem(): TimeblockItem? {
    if (isAllDay) return null

    val startMin = startTime.hour * 60 + startTime.minute
    val endMin = (endTime.hour * 60 + endTime.minute).coerceAtLeast(startMin + 15)

    return TimeblockItem(
        id = "memo_$objectId",
        title = title.ifEmpty { content },
        startMinutes = startMin,
        endMinutes = endMin.coerceAtMost(1440),
        color = try {
            Color(android.graphics.Color.parseColor(hexColorString))
        } catch (e: Exception) {
            Color(0xFF4CAF50)
        },
        type = TimeblockItemType.MEMO,
        originalMemo = this
    )
}
