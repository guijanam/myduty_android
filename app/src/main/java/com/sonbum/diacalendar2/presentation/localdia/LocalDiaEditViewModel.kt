package com.sonbum.diacalendar2.presentation.localdia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.LocalDia
import com.sonbum.diacalendar2.domain.repository.LocalDiaRepository
import com.sonbum.diacalendar2.domain.repository.LocalOfficeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocalDiaEditState(
    val diaDbId: Long? = null,
    val localOfficeId: Long = 0,
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

sealed interface LocalDiaEditEvent {
    data object SaveSuccess : LocalDiaEditEvent
    data object DeleteSuccess : LocalDiaEditEvent
    data class Error(val message: String) : LocalDiaEditEvent
}

class LocalDiaEditViewModel(
    private val localDiaRepository: LocalDiaRepository,
    private val localOfficeRepository: LocalOfficeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LocalDiaEditState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<LocalDiaEditEvent>()
    val event = _event.asSharedFlow()

    fun initialize(officeId: Long, diaId: Long?) {
        _state.update { it.copy(localOfficeId = officeId) }
        viewModelScope.launch {
            val office = localOfficeRepository.getLocalOfficeById(officeId)
            if (office != null) {
                _state.update { it.copy(officeName = office.officeName) }
            }
        }
        if (diaId != null) {
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true) }
                val dia = localDiaRepository.getLocalDiaById(diaId)
                if (dia != null) {
                    _state.update {
                        it.copy(
                            diaDbId = dia.id,
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
                }
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
        if (s.diaId.isBlank()) {
            viewModelScope.launch { _event.emit(LocalDiaEditEvent.Error("교번 ID를 입력해주세요")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val dia = LocalDia(
                    id = s.diaDbId ?: 0,
                    diaId = s.diaId.trim(),
                    localOfficeId = s.localOfficeId,
                    officeName = s.officeName,
                    typeName = s.typeName.ifBlank { null },
                    firstTime = s.firstTime.ifBlank { null },
                    numTr1 = s.numTr1.ifBlank { null },
                    numTr2 = s.numTr2.ifBlank { null },
                    secondTime = s.secondTime.ifBlank { null },
                    thirdTime = s.thirdTime.ifBlank { null },
                    totalTime = s.totalTime.ifBlank { null },
                    workTime = s.workTime.ifBlank { null }
                )
                if (s.diaDbId != null) {
                    localDiaRepository.updateLocalDia(dia)
                } else {
                    localDiaRepository.insertLocalDia(dia)
                }
                _event.emit(LocalDiaEditEvent.SaveSuccess)
            } catch (e: Exception) {
                _event.emit(LocalDiaEditEvent.Error("저장 실패: ${e.message}"))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun delete() {
        val diaDbId = _state.value.diaDbId ?: return
        viewModelScope.launch {
            try {
                localDiaRepository.deleteLocalDia(diaDbId)
                _event.emit(LocalDiaEditEvent.DeleteSuccess)
            } catch (e: Exception) {
                _event.emit(LocalDiaEditEvent.Error("삭제 실패: ${e.message}"))
            }
        }
    }
}
