package com.sonbum.diacalendar2.presentation.localoffice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.LocalOffice
import com.sonbum.diacalendar2.domain.repository.LocalOfficeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocalOfficeEditState(
    val officeId: Long? = null,
    val officeName: String = "",
    val diaTurns1: String = "",
    val diaTurns2: String = "",
    val subTurns: String = "",
    val diaSelects: String = "",
    val diaTurns3: String = "",  // 운휴 근무 (공휴일/일요일 포함 시 휴무 계산에 사용)
    val isLoading: Boolean = false
)

sealed interface LocalOfficeEditEvent {
    data object SaveSuccess : LocalOfficeEditEvent
    data class Error(val message: String) : LocalOfficeEditEvent
}

class LocalOfficeEditViewModel(
    private val localOfficeRepository: LocalOfficeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LocalOfficeEditState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<LocalOfficeEditEvent>()
    val event = _event.asSharedFlow()

    fun initialize(officeId: Long?) {
        if (officeId == null) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val office = localOfficeRepository.getLocalOfficeById(officeId)
            if (office != null) {
                _state.update {
                    it.copy(
                        officeId = office.id,
                        officeName = office.officeName,
                        diaTurns1 = office.diaTurns1 ?: "",
                        diaTurns2 = office.diaTurns2 ?: "",
                        subTurns = office.subTurns ?: "",
                        diaSelects = office.diaSelects ?: "",
                        diaTurns3 = office.diaTurns3 ?: "",
                        isLoading = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onOfficeNameChange(value: String) {
        _state.update { it.copy(officeName = value) }
    }

    fun onDiaTurns1Change(value: String) {
        _state.update { it.copy(diaTurns1 = value) }
    }

    fun onDiaTurns2Change(value: String) {
        _state.update { it.copy(diaTurns2 = value) }
    }

    fun onSubTurnsChange(value: String) {
        _state.update { it.copy(subTurns = value) }
    }

    fun onDiaSelectsChange(value: String) {
        _state.update { it.copy(diaSelects = value) }
    }

    fun onDiaTurns3Change(value: String) {
        _state.update { it.copy(diaTurns3 = value) }
    }

    fun save() {
        val currentState = _state.value
        if (currentState.officeName.isBlank()) {
            viewModelScope.launch {
                _event.emit(LocalOfficeEditEvent.Error("승무소 이름을 입력해주세요"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val office = LocalOffice(
                    id = currentState.officeId ?: 0,
                    officeName = currentState.officeName.trim(),
                    diaTurns1 = currentState.diaTurns1.ifBlank { null },
                    diaTurns2 = currentState.diaTurns2.ifBlank { null },
                    subTurns = currentState.subTurns.ifBlank { null },
                    diaSelects = currentState.diaSelects.ifBlank { null },
                    diaTurns3 = currentState.diaTurns3.ifBlank { null }
                )
                if (currentState.officeId != null) {
                    localOfficeRepository.updateLocalOffice(office)
                } else {
                    localOfficeRepository.insertLocalOffice(office)
                }
                _event.emit(LocalOfficeEditEvent.SaveSuccess)
            } catch (e: Exception) {
                _event.emit(LocalOfficeEditEvent.Error("저장 실패: ${e.message}"))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
