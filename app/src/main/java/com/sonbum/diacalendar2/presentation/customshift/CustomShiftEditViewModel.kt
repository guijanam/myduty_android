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

data class CustomShiftEditState(
    val shiftId: Long? = null,
    val shiftName: String = "",
    val shiftPattern: String = "",
    val isLoading: Boolean = false
)

sealed interface CustomShiftEditEvent {
    data object SaveSuccess : CustomShiftEditEvent
    data class Error(val message: String) : CustomShiftEditEvent
}

class CustomShiftEditViewModel(
    private val customShiftRepository: CustomShiftRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomShiftEditState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<CustomShiftEditEvent>()
    val event = _event.asSharedFlow()

    fun initialize(shiftId: Long?) {
        if (shiftId == null) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val shift = customShiftRepository.getCustomShiftById(shiftId)
            if (shift != null) {
                _state.update {
                    it.copy(
                        shiftId = shift.id,
                        shiftName = shift.shiftName,
                        shiftPattern = shift.shiftPattern.joinToString(","),
                        isLoading = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onShiftNameChange(value: String) {
        _state.update { it.copy(shiftName = value) }
    }

    fun onShiftPatternChange(value: String) {
        _state.update { it.copy(shiftPattern = value) }
    }

    fun save() {
        val currentState = _state.value
        if (currentState.shiftName.isBlank()) {
            viewModelScope.launch {
                _event.emit(CustomShiftEditEvent.Error("교대근무 이름을 입력해주세요"))
            }
            return
        }
        if (currentState.shiftPattern.isBlank()) {
            viewModelScope.launch {
                _event.emit(CustomShiftEditEvent.Error("교대근무 패턴을 입력해주세요"))
            }
            return
        }

        val patternList = currentState.shiftPattern
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (patternList.isEmpty()) {
            viewModelScope.launch {
                _event.emit(CustomShiftEditEvent.Error("유효한 패턴을 입력해주세요"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val shift = CustomShift(
                    id = currentState.shiftId ?: 0,
                    shiftName = currentState.shiftName.trim(),
                    shiftPattern = patternList
                )
                if (currentState.shiftId != null) {
                    customShiftRepository.updateCustomShift(shift)
                } else {
                    customShiftRepository.insertCustomShift(shift)
                }
                _event.emit(CustomShiftEditEvent.SaveSuccess)
            } catch (e: Exception) {
                _event.emit(CustomShiftEditEvent.Error("저장 실패: ${e.message}"))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun delete() {
        val shiftId = _state.value.shiftId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                customShiftRepository.deleteCustomShift(shiftId)
                _event.emit(CustomShiftEditEvent.SaveSuccess)
            } catch (e: Exception) {
                _event.emit(CustomShiftEditEvent.Error("삭제 실패: ${e.message}"))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
