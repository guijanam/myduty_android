package com.sonbum.diacalendar2.presentation.customshift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.CustomShift
import com.sonbum.diacalendar2.domain.repository.CustomShiftRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomShiftListState(
    val shifts: List<CustomShift> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface CustomShiftListEvent {
    data class Error(val message: String) : CustomShiftListEvent
}

class CustomShiftListViewModel(
    private val customShiftRepository: CustomShiftRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomShiftListState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<CustomShiftListEvent>()
    val event = _event.asSharedFlow()

    init {
        observeShifts()
    }

    private fun observeShifts() {
        viewModelScope.launch {
            customShiftRepository.getAllCustomShifts().collect { shifts ->
                _state.update { it.copy(shifts = shifts, isLoading = false) }
            }
        }
    }

    fun deleteShift(id: Long) {
        viewModelScope.launch {
            try {
                customShiftRepository.deleteCustomShift(id)
            } catch (e: Exception) {
                _event.emit(CustomShiftListEvent.Error("삭제 실패: ${e.message}"))
            }
        }
    }
}
