package com.sonbum.diacalendar2.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AuthEvent {
    data object NavigateToNicknameSetup : AuthEvent
    data object NavigateToBoard : AuthEvent
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<AuthEvent>()
    val event = _event.asSharedFlow()

    fun setError(message: String) {
        _state.update { it.copy(errorMessage = message) }
    }

    fun signInWithGoogle(googleIdToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signInWithGoogle(googleIdToken)
                .onSuccess { hasNickname ->
                    _state.update { it.copy(isLoading = false) }
                    if (hasNickname) {
                        _event.emit(AuthEvent.NavigateToBoard)
                    } else {
                        _event.emit(AuthEvent.NavigateToNicknameSetup)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "오류가 발생했습니다"
                        )
                    }
                }
        }
    }
}
