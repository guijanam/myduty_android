package com.sonbum.diacalendar2.presentation.home

import java.time.LocalDate

// 사용자의 동작
sealed interface HomeAction {
	data class OnDateClick(val date: LocalDate) : HomeAction
}