package com.sonbum.diacalendar2.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.datastore.AuthPreferences
import com.sonbum.diacalendar2.domain.repository.AuthRepository
import com.sonbum.diacalendar2.domain.repository.BoardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

class MainViewModel(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository,
    private val authPreferences: AuthPreferences
) : ViewModel() {

    private val _hasNewBoardPosts = MutableStateFlow(false)
    val hasNewBoardPosts = _hasNewBoardPosts.asStateFlow()

    fun checkNewPosts() {
        viewModelScope.launch {
            if (!authRepository.isLoggedIn.first()) return@launch
            val lastChecked = authPreferences.lastCheckedBoard.first()
                ?: return@launch
            boardRepository.hasNewPosts(lastChecked)
                .onSuccess { hasNew -> _hasNewBoardPosts.value = hasNew }
        }
    }

    fun markBoardChecked() {
        _hasNewBoardPosts.value = false
        viewModelScope.launch {
            authPreferences.saveLastCheckedBoard(Instant.now().toString())
        }
    }
}
