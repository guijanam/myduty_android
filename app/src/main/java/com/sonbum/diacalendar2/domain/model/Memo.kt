package com.sonbum.diacalendar2.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class Memo(
    val objectId: String = UUID.randomUUID().toString(),
    val hexColorString: String = "#4CAF50",
    val title: String = "",
    val content: String = "",
    val startTime: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val endTime: LocalTime = LocalTime.now().plusHours(1).withSecond(0).withNano(0),
    val date: LocalDate = LocalDate.now(),
    val isCompleted: Boolean = false,
    val position: Long = 0L,
    val reminderEnabled: Boolean = false,
    val reminderTimeMillis: Long? = null,
    val imagePath: String? = null,
    val isAllDay: Boolean = false
)
