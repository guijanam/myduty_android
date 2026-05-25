package com.sonbum.diacalendar2.domain.model

import java.time.LocalDate

data class VacationRecord(
    val id: Long = 0,
    val date: LocalDate,
    val vacationTypeId: Long,
    val vacationName: String,
    val shortName: String
)
