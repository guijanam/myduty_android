package com.sonbum.diacalendar2.domain.model

import java.time.LocalDate

data class LateHolidayRecord(
    val id: Long = 0,
    val date: LocalDate,
    val lateHolidayTypeId: Long,
    val lateHolidayName: String,
    val shortName: String
)
