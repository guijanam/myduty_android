package com.sonbum.diacalendar2.presentation.coworker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.CoworkerGroup
import com.sonbum.diacalendar2.domain.repository.CoworkerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CoworkerGroupState(
    val groups: List<CoworkerGroup> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface CoworkerGroupEvent {
    data class Error(val message: String) : CoworkerGroupEvent
    data object SaveSuccess : CoworkerGroupEvent
}

class CoworkerGroupViewModel(
    private val coworkerRepository: CoworkerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CoworkerGroupState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<CoworkerGroupEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            coworkerRepository.getAllGroups().collect { groups ->
                _state.update { it.copy(groups = groups, isLoading = false) }
            }
        }
    }

    fun saveGroup(name: String, existingId: Long? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                coworkerRepository.saveGroup(
                    CoworkerGroup(
                        id = existingId ?: 0L,
                        name = name.trim()
                    )
                )
                _event.emit(CoworkerGroupEvent.SaveSuccess)
            } catch (e: Exception) {
                _event.emit(CoworkerGroupEvent.Error("저장 실패: ${e.message}"))
            }
        }
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch {
            try {
                coworkerRepository.deleteGroup(id)
            } catch (e: Exception) {
                _event.emit(CoworkerGroupEvent.Error("삭제 실패: ${e.message}"))
            }
        }
    }
}
