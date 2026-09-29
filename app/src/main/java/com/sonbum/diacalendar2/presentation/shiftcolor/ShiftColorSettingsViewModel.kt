package com.sonbum.diacalendar2.presentation.shiftcolor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.datastore.ShiftColorPreferences
import com.sonbum.diacalendar2.data.local.datastore.ShiftDisplayColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShiftColorSettingsViewModel(
    private val shiftColorPreferences: ShiftColorPreferences
) : ViewModel() {

    val colors: StateFlow<ShiftDisplayColors> = shiftColorPreferences.colors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ShiftDisplayColors.DEFAULT
        )

    fun updateDayShiftColor(hex: String) {
        viewModelScope.launch {
            shiftColorPreferences.saveDayShiftColor(hex)
        }
    }

    fun updateNightShiftColor(hex: String) {
        viewModelScope.launch {
            shiftColorPreferences.saveNightShiftColor(hex)
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            shiftColorPreferences.resetToDefault()
        }
    }
}
