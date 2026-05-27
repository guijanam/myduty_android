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

    fun addVacationType(
        name: String,
        shortName: String,
        annualQuota: Int,
        resetMonthDay: String,
        grantDate: String,
        expiryDate: String
    ) {
        if (name.isBlank() || shortName.isBlank()) return
        val grantYear = grantDate.take(4).toIntOrNull() ?: 0
        val expiryYear = expiryDate.take(4).toIntOrNull() ?: 0
        viewModelScope.launch {
            vacationTypeRepository.addVacationType(
                name = name.trim(),
                shortName = shortName.trim(),
                annualQuota = annualQuota.coerceAtLeast(0),
                resetMonthDay = resetMonthDay,
                grantYear = grantYear,
                expiryYear = expiryYear,
                grantDate = grantDate,
                expiryDate = expiryDate
            )
        }
    }

    fun updateVacationType(
        id: Long,
        name: String,
        shortName: String,
        annualQuota: Int,
        resetMonthDay: String,
        grantDate: String,
        expiryDate: String
    ) {
        if (name.isBlank() || shortName.isBlank()) return
        val grantYear = grantDate.take(4).toIntOrNull() ?: 0
        val expiryYear = expiryDate.take(4).toIntOrNull() ?: 0
        viewModelScope.launch {
            vacationTypeRepository.updateVacationType(
                id = id,
                name = name.trim(),
                shortName = shortName.trim(),
                annualQuota = annualQuota.coerceAtLeast(0),
                resetMonthDay = resetMonthDay,
                grantYear = grantYear,
                expiryYear = expiryYear,
                grantDate = grantDate,
                expiryDate = expiryDate
            )
        }
    }

    fun deleteVacationType(id: Long) {
        viewModelScope.launch {
            vacationTypeRepository.deleteVacationType(id)
        }
    }
}
