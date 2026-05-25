package com.sonbum.diacalendar2.domain.model

import java.time.LocalDate

data class LateWorkRecord(
    val id: Long = 0,
    val date: LocalDate,
    val lateWorkTypeId: Long,
    val lateWorkName: String,
    val shortName: String
)
