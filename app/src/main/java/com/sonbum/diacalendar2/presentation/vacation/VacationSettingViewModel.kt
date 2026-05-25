package com.sonbum.diacalendar2.presentation.vacation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.VacationType
import com.sonbum.diacalendar2.domain.repository.VacationTypeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VacationSettingState(
    val vacationTypes: List<VacationType> = emptyList(),
    val isLoading: Boolean = true
)

class VacationSettingViewModel(
    private val vacationTypeRepository: VacationTypeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VacationSettingState())
    val state: StateFlow<VacationSettingState> = _state.asStateFlow()

    init {
        initDefaults()
        observeVacationTypes()
    }

    private fun initDefaults() {
        viewModelScope.launch {
            vacationTypeRepository.ensureDefaultsExist()
        }
    }

    private fun observeVacationTypes() {
        viewModelScope.launch {
            vacationTypeRepository.getAllVacationTypes().collect { types ->
                _state.update { it.copy(vacationTypes = types, isLoading = false) }
            }
        }
    }

    fun addVacationType(name: String, shortName: String) {
        if (name.isBlank() || shortName.isBlank()) return
        viewModelScope.launch {
            vacationTypeRepository.addVacationType(name.trim(), shortName.trim())
        }
    }

    fun updateVacationType(id: Long, name: String, shortName: String) {
        if (name.isBlank() || shortName.isBlank()) return
        viewModelScope.launch {
            vacationTypeRepository.updateVacationType(id, name.trim(), shortName.trim())
        }
    }

    fun deleteVacationType(id: Long) {
        viewModelScope.launch {
            vacationTypeRepository.deleteVacationType(id)
        }
    }
}
