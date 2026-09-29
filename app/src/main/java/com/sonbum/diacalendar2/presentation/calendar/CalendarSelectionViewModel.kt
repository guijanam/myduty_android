package com.sonbum.diacalendar2.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.datastore.CalendarPreferences
import com.sonbum.diacalendar2.domain.model.DeviceCalendar
import com.sonbum.diacalendar2.domain.repository.DeviceCalendarRepository
import com.sonbum.diacalendar2.domain.usecase.ShiftCalendarSyncUseCase
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
    val error: String? = null,
    // 근무 동기화
    val shiftSyncEnabled: Boolean = false,
    val shiftSyncCalendarId: Long = -1L,
    val shiftSyncLabel: String = "",
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
) {
    /** 근무를 등록할 수 있는 캘린더 후보 (쓰기 가능한 Google 캘린더 우선) */
    val syncableCalendars: List<DeviceCalendar>
        get() = calendars.filter { it.isWritable }
            .sortedWith(compareByDescending<DeviceCalendar> { it.isGoogle }.thenByDescending { it.isPrimary })
}

class CalendarSelectionViewModel(
    private val deviceCalendarRepository: DeviceCalendarRepository,
    private val calendarPreferences: CalendarPreferences,
    private val shiftCalendarSyncUseCase: ShiftCalendarSyncUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarSelectionState())
    val state: StateFlow<CalendarSelectionState> = _state.asStateFlow()

    init {
        loadCalendars()
        observeSelectedCalendars()
        observeShiftSync()
    }

    private fun observeShiftSync() {
        viewModelScope.launch {
            calendarPreferences.shiftSyncEnabled.collect { enabled ->
                _state.update { it.copy(shiftSyncEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            calendarPreferences.shiftSyncCalendarId.collect { id ->
                _state.update { it.copy(shiftSyncCalendarId = id) }
            }
        }
        viewModelScope.launch {
            // 라벨 변경(또는 승무소명 변경) 시 입력란 값 갱신
            calendarPreferences.shiftSyncLabel.collect {
                _state.update { it.copy(shiftSyncLabel = shiftCalendarSyncUseCase.getCurrentLabel()) }
            }
        }
    }

    /**
     * 이벤트 제목 괄호 라벨 변경. 저장 후 동기화가 켜져 있으면 재동기화.
     */
    fun setSyncLabel(label: String) {
        viewModelScope.launch {
            calendarPreferences.setShiftSyncLabel(label)
            if (_state.value.shiftSyncEnabled && _state.value.shiftSyncCalendarId > 0) {
                runSync()
            }
        }
    }

    /**
     * 근무를 등록할 대상 캘린더 선택. 동기화가 켜져 있으면 즉시 재동기화.
     */
    fun selectSyncCalendar(calendarId: Long) {
        viewModelScope.launch {
            // 캘린더를 바꾸면 이전 캘린더의 우리 이벤트는 정리
            val prev = _state.value.shiftSyncCalendarId
            if (prev > 0 && prev != calendarId) {
                shiftCalendarSyncUseCase.clearSyncedEvents()
            }
            calendarPreferences.setShiftSyncCalendarId(calendarId)
            if (_state.value.shiftSyncEnabled) {
                runSync()
            }
        }
    }

    /**
     * 근무 동기화 토글. 켜면 선택된 캘린더에 동기화하고,
     * 끄면 우리가 등록한 근무 이벤트만 제거한다.
     */
    fun toggleShiftSync(enabled: Boolean) {
        viewModelScope.launch {
            calendarPreferences.setShiftSyncEnabled(enabled)
            if (enabled) {
                if (_state.value.shiftSyncCalendarId <= 0) {
                    _state.update { it.copy(syncMessage = "근무를 등록할 캘린더를 먼저 선택하세요.") }
                } else {
                    runSync()
                }
            } else {
                shiftCalendarSyncUseCase.clearSyncedEvents()
                _state.update { it.copy(syncMessage = "근무 동기화를 껐습니다.") }
            }
        }
    }

    /**
     * 수동 재동기화.
     */
    fun syncNow() {
        viewModelScope.launch { runSync() }
    }

    private suspend fun runSync() {
        _state.update { it.copy(isSyncing = true, syncMessage = null) }
        val result = shiftCalendarSyncUseCase.sync()
        _state.update {
            it.copy(
                isSyncing = false,
                syncMessage = result.fold(
                    onSuccess = { r -> "${r.eventCount}건의 근무를 캘린더에 등록했습니다." },
                    onFailure = { e -> "동기화 실패: ${e.message}" }
                )
            )
        }
    }

    fun consumeSyncMessage() {
        _state.update { it.copy(syncMessage = null) }
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
