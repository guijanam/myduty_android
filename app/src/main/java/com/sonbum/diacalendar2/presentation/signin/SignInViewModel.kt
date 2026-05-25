package com.sonbum.diacalendar2.presentation.signin

import androidx.lifecycle.ViewModel

class SignInViewModel : ViewModel() {
	init {
		println("SignInViewModel created")
	}

	override fun onCleared() {
		super.onCleared()
		println("SignInViewModel cleared")
	}
}