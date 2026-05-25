package com.sonbum.diacalendar2.presentation.diatable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.Dia
import com.sonbum.diacalendar2.domain.repository.DiaRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerDiaEditState(
    val diaDbId: Long? = null,
    val officeId: Int? = null,
    val officeName: String = "",
    val diaId: String = "",
    val typeName: String = "",
    val firstTime: String = "",
    val numTr1: String = "",
    val numTr2: String = "",
    val secondTime: String = "",
    val thirdTime: String = "",
    val totalTime: String = "",
    val workTime: String = "",
    val isLoading: Boolean = false
)

sealed interface ServerDiaEditEvent {
    data object SaveSuccess : ServerDiaEditEvent
    data class Error(val message: String) : ServerDiaEditEvent
}

class ServerDiaEditViewModel(
    private val diaRepository: DiaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ServerDiaEditState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<ServerDiaEditEvent>()
    val event = _event.asSharedFlow()

    fun initialize(diaId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val dia = diaRepository.getDiaById(diaId)
            if (dia != null) {
                _state.update {
                    it.copy(
                        diaDbId = dia.id,
                        officeId = dia.officeId,
                        officeName = dia.officeName,
                        diaId = dia.diaId,
                        typeName = dia.typeName ?: "",
                        firstTime = dia.firstTime ?: "",
                        numTr1 = dia.numTr1 ?: "",
                        numTr2 = dia.numTr2 ?: "",
                        secondTime = dia.secondTime ?: "",
                        thirdTime = dia.thirdTime ?: "",
                        totalTime = dia.totalTime ?: "",
                        workTime = dia.workTime ?: "",
                        isLoading = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
                _event.emit(ServerDiaEditEvent.Error("근무표를 찾을 수 없습니다"))
            }
        }
    }

    fun onDiaIdChange(value: String) { _state.update { it.copy(diaId = value) } }
    fun onTypeNameChange(value: String) { _state.update { it.copy(typeName = value) } }
    fun onFirstTimeChange(value: String) { _state.update { it.copy(firstTime = value) } }
    fun onNumTr1Change(value: String) { _state.update { it.copy(numTr1 = value) } }
    fun onNumTr2Change(value: String) { _state.update { it.copy(numTr2 = value) } }
    fun onSecondTimeChange(value: String) { _state.update { it.copy(secondTime = value) } }
    fun onThirdTimeChange(value: String) { _state.update { it.copy(thirdTime = value) } }
    fun onTotalTimeChange(value: String) { _state.update { it.copy(totalTime = value) } }
    fun onWorkTimeChange(value: String) { _state.update { it.copy(workTime = value) } }

    fun save() {
        val s = _state.value
        if (s.diaDbId == null) {
            viewModelScope.launch { _event.emit(ServerDiaEditEvent.Error("근무표 데이터가 없습니다")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val dia = Dia(
                    id = s.diaDbId,
                    diaId = s.diaId.trim(),
                    officeName = s.officeName,
                    officeId = s.officeId,
                    typeName = s.typeName.ifBlank { null },
                    firstTime = s.firstTime.ifBlank { null },
                    numTr1 = s.numTr1.ifBlank { null },
                    numTr2 = s.numTr2.ifBlank { null },
                    secondTime = s.secondTime.ifBlank { null },
                    thirdTime = s.thirdTime.ifBlank { null },
                    totalTime = s.totalTime.ifBlank { null },
                    workTime = s.workTime.ifBlank { null }
                )
                diaRepository.updateDia(dia)
                _event.emit(ServerDiaEditEvent.SaveSuccess)
            } catch (e: Exception) {
                _event.emit(ServerDiaEditEvent.Error("저장 실패: ${e.message}"))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
