package com.sonbum.diacalendar2.presentation.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.BlockedUser
import com.sonbum.diacalendar2.domain.repository.BoardRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedUsersState(
    val blockedUsers: List<BlockedUser> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BlockedUsersViewModel(
    private val boardRepository: BoardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BlockedUsersState())
    val state = _state.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    init {
        loadBlockedUsers()
    }

    fun loadBlockedUsers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            boardRepository.getBlockedUsers()
                .onSuccess { users ->
                    _state.update { it.copy(blockedUsers = users, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun unblockUser(blockedId: String) {
        viewModelScope.launch {
            boardRepository.unblockUser(blockedId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(blockedUsers = state.blockedUsers.filter { it.blockedId != blockedId })
                    }
                    _snackbarMessage.emit("차단이 해제되었습니다")
                }
                .onFailure { error ->
                    _snackbarMessage.emit(error.message ?: "차단 해제에 실패했습니다")
                }
        }
    }
}
