package com.sonbum.diacalendar2.presentation.textsize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.datastore.CalendarTextSizes
import com.sonbum.diacalendar2.data.local.datastore.TextSizePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TextSizeSettingsViewModel(
    private val textSizePreferences: TextSizePreferences
) : ViewModel() {

    val textSizes: StateFlow<CalendarTextSizes> = textSizePreferences.textSizes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CalendarTextSizes.DEFAULT
        )

    fun updateDateFontSize(size: Float) {
        viewModelScope.launch {
            textSizePreferences.saveDateFontSize(size)
        }
    }

    fun updateShiftFontSize(size: Float) {
        viewModelScope.launch {
            textSizePreferences.saveShiftFontSize(size)
        }
    }

    fun updateEventFontSize(size: Float) {
        viewModelScope.launch {
            textSizePreferences.saveEventFontSize(size)
        }
    }

    fun updateMemoFontSize(size: Float) {
        viewModelScope.launch {
            textSizePreferences.saveMemoFontSize(size)
        }
    }

    fun updateCrewPatternFontSize(size: Float) {
        viewModelScope.launch {
            textSizePreferences.saveCrewPatternFontSize(size)
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            textSizePreferences.resetToDefault()
        }
    }
}
