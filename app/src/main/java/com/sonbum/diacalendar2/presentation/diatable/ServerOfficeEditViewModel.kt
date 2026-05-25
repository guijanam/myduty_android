package com.sonbum.diacalendar2.presentation.diatable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.Office
import com.sonbum.diacalendar2.domain.repository.OfficeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerOfficeEditState(
    val officeCode: Long? = null,
    val officeName: String = "",
    val diaTurns1: String = "",
    val diaTurns2: String = "",
    val subTurns: String = "",
    val diaSelects: String = "",
    val diaTurns3: String = "",
    val adminPassword: String? = null,
    val isLoading: Boolean = false
)

sealed interface ServerOfficeEditEvent {
    data object SaveSuccess : ServerOfficeEditEvent
    data class Error(val message: String) : ServerOfficeEditEvent
}

class ServerOfficeEditViewModel(
    private val officeRepository: OfficeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ServerOfficeEditState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<ServerOfficeEditEvent>()
    val event = _event.asSharedFlow()

    fun initialize(officeCode: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val office = officeRepository.getOfficeByCode(officeCode)
            if (office != null) {
                _state.update {
                    it.copy(
                        officeCode = office.officeCode,
                        officeName = office.officeName,
                        diaTurns1 = office.diaTurns1 ?: "",
                        diaTurns2 = office.diaTurns2 ?: "",
                        subTurns = office.subTurns ?: "",
                        diaSelects = office.diaSelects ?: "",
                        diaTurns3 = office.diaTurns3 ?: "",
                        adminPassword = office.adminPassword,
                        isLoading = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
                _event.emit(ServerOfficeEditEvent.Error("승무소를 찾을 수 없습니다"))
            }
        }
    }

    fun onDiaTurns1Change(value: String) { _state.update { it.copy(diaTurns1 = value) } }
    fun onDiaTurns2Change(value: String) { _state.update { it.copy(diaTurns2 = value) } }
    fun onSubTurnsChange(value: String) { _state.update { it.copy(subTurns = value) } }
    fun onDiaSelectsChange(value: String) { _state.update { it.copy(diaSelects = value) } }
    fun onDiaTurns3Change(value: String) { _state.update { it.copy(diaTurns3 = value) } }

    fun save() {
        val s = _state.value
        if (s.officeCode == null) {
            viewModelScope.launch { _event.emit(ServerOfficeEditEvent.Error("승무소 데이터가 없습니다")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val office = Office(
                    officeCode = s.officeCode,
                    officeName = s.officeName,
                    diaTurns1 = s.diaTurns1.ifBlank { null },
                    diaTurns2 = s.diaTurns2.ifBlank { null },
                    subTurns = s.subTurns.ifBlank { null },
                    diaSelects = s.diaSelects.ifBlank { null },
                    diaTurns3 = s.diaTurns3.ifBlank { null },
                    adminPassword = s.adminPassword
                )
                officeRepository.updateOffice(office)
                _event.emit(ServerOfficeEditEvent.SaveSuccess)
            } catch (e: Exception) {
                _event.emit(ServerOfficeEditEvent.Error("저장 실패: ${e.message}"))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
