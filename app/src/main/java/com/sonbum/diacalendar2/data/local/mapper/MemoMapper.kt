package com.sonbum.diacalendar2.data.local.mapper

import com.sonbum.diacalendar2.data.local.entity.MemoEntity
import com.sonbum.diacalendar2.domain.model.Memo
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun MemoEntity.toDomain(): Memo {
    return Memo(
        objectId = objectId,
        hexColorString = hexColorString,
        title = title,
        content = content,
        startTime = LocalTime.parse(startTimeString, timeFormatter),
        endTime = LocalTime.parse(endTimeString, timeFormatter),
        date = LocalDate.parse(dateString),
        isCompleted = isCompleted,
        position = position,
        reminderEnabled = reminderEnabled,
        reminderTimeMillis = reminderTimeMillis,
        imagePath = imagePath,
        isAllDay = isAllDay
    )
}

fun Memo.toEntity(): MemoEntity {
    return MemoEntity(
        objectId = objectId,
        hexColorString = hexColorString,
        title = title,
        content = content,
        startTimeString = startTime.format(timeFormatter),
        endTimeString = endTime.format(timeFormatter),
        dateString = date.toString(),
        isCompleted = isCompleted,
        position = position,
        reminderEnabled = reminderEnabled,
        reminderTimeMillis = reminderTimeMillis,
        imagePath = imagePath,
        isAllDay = isAllDay
    )
}
