package com.sonbum.diacalendar2.domain.model

import java.time.LocalDate

data class Holiday(
    val id: String,
    val date: LocalDate,
    val name: String,
    val isHoliday: Boolean = true,
    val isUserCreated: Boolean = false  // 사용자가 직접 추가한 공휴일 여부
)
