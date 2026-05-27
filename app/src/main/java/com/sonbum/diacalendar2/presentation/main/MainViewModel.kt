package com.sonbum.diacalendar2.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.DrawerWebsiteRegistry
import com.sonbum.diacalendar2.data.local.datastore.AuthPreferences
import com.sonbum.diacalendar2.domain.repository.AuthRepository
import com.sonbum.diacalendar2.domain.repository.BoardRepository
import com.sonbum.diacalendar2.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

class MainViewModel(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository,
    private val authPreferences: AuthPreferences,
    private val shiftRepository: ShiftRepository,
    private val drawerWebsiteRegistry: DrawerWebsiteRegistry
) : ViewModel() {

    private val _hasNewBoardPosts = MutableStateFlow(false)
    val hasNewBoardPosts = _hasNewBoardPosts.asStateFlow()

    sealed class OfficeWebsiteTabState {
        data object Loading : OfficeWebsiteTabState()
        data object Unavailable : OfficeWebsiteTabState()
        data class Available(val url: String, val officeName: String) : OfficeWebsiteTabState()
    }

    private val _officeWebsiteTabState = MutableStateFlow<OfficeWebsiteTabState>(OfficeWebsiteTabState.Loading)
    val officeWebsiteTabState = _officeWebsiteTabState.asStateFlow()

    init {
        observeOfficeWebsite()
    }

    private fun observeOfficeWebsite() {
        viewModelScope.launch {
            shiftRepository.getUserConfig().collect { config ->
                val officeName = config?.officeName
                val isCustomShift = (config?.officeCode ?: 0) <= -10000
                val url = if (!isCustomShift && officeName != null)
                    drawerWebsiteRegistry.getUrl(officeName) else null
                _officeWebsiteTabState.value = if (url != null && officeName != null)
                    OfficeWebsiteTabState.Available(url, officeName)
                else
                    OfficeWebsiteTabState.Unavailable
            }
        }
    }

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
