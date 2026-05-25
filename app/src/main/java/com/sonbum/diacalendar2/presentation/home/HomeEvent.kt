package com.sonbum.diacalendar2.presentation.home

// ViewModel에서 화면으로 보내는 일회성 이벤트
sealed interface HomeEvent {
	data class NavigateToDateDetail(val date: String) : HomeEvent
	data class ShowMessage(val message: String) : HomeEvent
	data object BackupRestored : HomeEvent
}