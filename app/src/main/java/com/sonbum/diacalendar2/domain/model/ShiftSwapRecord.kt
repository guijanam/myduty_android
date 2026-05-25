package com.sonbum.diacalendar2.domain.model

import java.time.LocalDate

/**
 * 교번교체 레코드 Domain 모델
 */
data class ShiftSwapRecord(
    val id: Long = 0,
    val date: LocalDate,
    val originalShiftName: String,
    val swappedShiftName: String,
    val groupId: String
)
