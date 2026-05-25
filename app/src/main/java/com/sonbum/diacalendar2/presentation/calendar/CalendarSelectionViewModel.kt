package com.sonbum.diacalendar2.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.DeviceCalendar
import com.sonbum.diacalendar2.domain.repository.DeviceCalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarSelectionState(
    val calendars: List<DeviceCalendar> = emptyList(),
    val selectedCalendarIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val hasPermission: Boolean = true,
    val error: String? = null
)

class CalendarSelectionViewModel(
    private val deviceCalendarRepository: DeviceCalendarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarSelectionState())
    val state: StateFlow<CalendarSelectionState> = _state.asStateFlow()

    init {
        loadCalendars()
        observeSelectedCalendars()
    }

    private fun loadCalendars() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val calendars = deviceCalendarRepository.getCalendars()
                _state.update {
                    it.copy(
                        calendars = calendars,
                        isLoading = false
                    )
                }
            } catch (e: SecurityException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        hasPermission = false,
                        error = "캘린더 접근 권한이 필요합니다."
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "캘린더를 불러오는데 실패했습니다: ${e.message}"
                    )
                }
            }
        }
    }

    private fun observeSelectedCalendars() {
        viewModelScope.launch {
            deviceCalendarRepository.getSelectedCalendarIds().collect { ids ->
                _state.update { it.copy(selectedCalendarIds = ids) }
            }
        }
    }

    fun toggleCalendarSelection(calendarId: Long) {
        viewModelScope.launch {
            deviceCalendarRepository.toggleCalendarSelection(calendarId)
        }
    }

    fun selectAllCalendars() {
        viewModelScope.launch {
            val allIds = _state.value.calendars.map { it.id }.toSet()
            deviceCalendarRepository.saveSelectedCalendarIds(allIds)
        }
    }

    fun deselectAllCalendars() {
        viewModelScope.launch {
            deviceCalendarRepository.saveSelectedCalendarIds(emptySet())
        }
    }

    fun refreshCalendars() {
        loadCalendars()
    }

    fun setPermissionGranted() {
        _state.update { it.copy(hasPermission = true) }
        loadCalendars()
    }
}
