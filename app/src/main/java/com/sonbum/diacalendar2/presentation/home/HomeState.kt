package com.sonbum.diacalendar2.presentation.home

import java.time.LocalDate

// 화면 상태 (필요 시 확장 가능)
data class HomeState(
	val selectedDate: LocalDate? = null
)