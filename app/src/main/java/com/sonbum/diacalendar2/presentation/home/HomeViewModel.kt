package com.sonbum.diacalendar2.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.datastore.CalendarTextSizes
import com.sonbum.diacalendar2.data.local.datastore.CrewPatternPreferences
import com.sonbum.diacalendar2.data.local.datastore.TextSizePreferences
import com.sonbum.diacalendar2.data.local.datastore.ThemeMode
import com.sonbum.diacalendar2.data.local.datastore.ThemePreferences
import com.sonbum.diacalendar2.domain.model.CalendarEvent
import com.sonbum.diacalendar2.domain.model.Memo
import com.sonbum.diacalendar2.domain.repository.DeviceCalendarRepository
import com.sonbum.diacalendar2.domain.repository.HolidayRepository
import com.sonbum.diacalendar2.domain.repository.MemoRepository
import com.sonbum.diacalendar2.domain.repository.ShiftRepository
import com.sonbum.diacalendar2.domain.repository.SubShiftRepository
import com.sonbum.diacalendar2.domain.repository.ShiftSwapRecordRepository
import com.sonbum.diacalendar2.domain.repository.VacationRecordRepository
import com.sonbum.diacalendar2.domain.repository.LateWorkRecordRepository
import com.sonbum.diacalendar2.domain.repository.LateHolidayRecordRepository
import com.sonbum.diacalendar2.domain.repository.OfficeRepository
import com.sonbum.diacalendar2.domain.repository.ShiftInputRecordRepository
import com.sonbum.diacalendar2.domain.repository.BackupRepository
import com.sonbum.diacalendar2.domain.repository.AnniversaryRepository
import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 충당 정보를 담는 데이터 클래스
 */
data class ShiftInputInfo(
    val shortName: String,
    val colorHex: String,
    val groupId: String
)

data class HomeCalendarState(
    val memosByDate: Map<LocalDate, List<Memo>> = emptyMap(),
    val eventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val holidayMap: Map<LocalDate, String> = emptyMap(),
    val shiftScheduleMap: Map<LocalDate, String> = emptyMap(),
    val vacationMap: Map<LocalDate, String> = emptyMap(),
    val isRefreshingHolidays: Boolean = false,
    val shiftPattern: List<String> = emptyList(),
    val subShiftScheduleMap: Map<LocalDate, String> = emptyMap(),
    val swapDates: Set<LocalDate> = emptySet(),
    val shiftInputMap: Map<LocalDate, ShiftInputInfo> = emptyMap(), // date -> ShiftInputInfo
    val holidayWorkShifts: List<String> = emptyList(),
    val isCustomShift: Boolean = false,
    val officeName: String? = null,
    val anniversaryMap: Map<LocalDate, String> = emptyMap()
)

class HomeViewModel(
    private val memoRepository: MemoRepository,
    private val deviceCalendarRepository: DeviceCalendarRepository,
    private val themePreferences: ThemePreferences,
    private val textSizePreferences: TextSizePreferences,
    private val holidayRepository: HolidayRepository,
    private val shiftRepository: ShiftRepository,
    private val subShiftRepository: SubShiftRepository,
    private val vacationRecordRepository: VacationRecordRepository,
    private val shiftSwapRecordRepository: ShiftSwapRecordRepository,
    private val lateWorkRecordRepository: LateWorkRecordRepository,
    private val lateHolidayRecordRepository: LateHolidayRecordRepository,
    private val shiftInputRecordRepository: ShiftInputRecordRepository,
    private val officeRepository: OfficeRepository,
    private val backupRepository: BackupRepository,
    private val crewPatternPreferences: CrewPatternPreferences,
    private val anniversaryRepository: AnniversaryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeCalendarState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<HomeEvent>()
    val event = _event.asSharedFlow()

    val themeMode = themePreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val textSizes = textSizePreferences.textSizes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarTextSizes.DEFAULT)

    val showCrewPattern = crewPatternPreferences.showCrewPattern
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val crewPattern = crewPatternPreferences.crewPattern
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("AD", "BA", "CB", "DC"))

    val crewPatternStartDate = crewPatternPreferences.crewPatternStartDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), java.time.LocalDate.of(2026, 2, 1))

    val showSubShift = crewPatternPreferences.showSubShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // 현재 캘린더에 표시 중인 연도 (HomeScreen에서 갱신) - init 이전에 초기화 필요
    private val _visibleYear = MutableStateFlow(LocalDate.now().year)

    init {
        loadAllMemos()
        observeSelectedCalendars()
        observeEventChanges()
        observeHolidays()
        observeShiftSchedules()
        observeVacationRecords()
        observeSubShiftSchedules()
        loadShiftPattern()
        observeAnniversaries()
    }

    private fun observeSubShiftSchedules() {
        viewModelScope.launch {
            subShiftRepository.getScheduleMap().collect { map ->
                _state.update { it.copy(subShiftScheduleMap = map) }
            }
        }
    }

    fun toggleShowSubShift(show: Boolean) {
        viewModelScope.launch {
            crewPatternPreferences.saveShowSubShift(show)
        }
    }

    private fun loadAllMemos() {
        viewModelScope.launch {
            memoRepository.getAllMemos().collect { memos ->
                val grouped = memos.groupBy { it.date }
                _state.update { it.copy(memosByDate = grouped) }
            }
        }
    }

    private fun observeSelectedCalendars() {
        viewModelScope.launch {
            // 선택된 캘린더 ID가 변경될 때마다 이벤트 다시 로드
            deviceCalendarRepository.getSelectedCalendarIds().collectLatest { selectedIds ->
                if (selectedIds.isNotEmpty()) {
                    loadCalendarEvents()
                } else {
                    _state.update { it.copy(eventsByDate = emptyMap()) }
                }
            }
        }
    }

    private fun observeEventChanges() {
        viewModelScope.launch {
            // CRUD 작업 후 이벤트 변경 알림 수신
            deviceCalendarRepository.eventChanges.collect {
                loadCalendarEvents()
            }
        }
    }

    private fun observeHolidays() {
        viewModelScope.launch {
            holidayRepository.getHolidayMap().collect { map ->
                _state.update { it.copy(holidayMap = map) }
            }
        }
    }

    private fun observeShiftSchedules() {
        viewModelScope.launch {
            combine(
                shiftRepository.getScheduleMap(),
                shiftSwapRecordRepository.getAllRecords().map { records ->
                    records.associate { it.date to it.swappedShiftName }
                },
                shiftInputRecordRepository.getAllRecords(),
                lateWorkRecordRepository.getAllRecords().map { records ->
                    records.associate { it.date to it.shortName }
                },
                lateHolidayRecordRepository.getAllRecords().map { records ->
                    records.associate { it.date to it.shortName }
                }
            ) { scheduleMap, swapMap, shiftInputRecords, lateWorkMap, lateHolidayMap ->
                // 교체 레코드가 있으면 원래 스케줄을 오버라이드
                val effectiveMap = scheduleMap.toMutableMap()
                val shiftInputDisplayMap = mutableMapOf<LocalDate, ShiftInputInfo>()

                // 1. 근무 교체 적용
                swapMap.forEach { (date, swappedName) ->
                    effectiveMap[date] = swappedName
                }

                // 2. 지근 적용 (교체보다 우선)
                lateWorkMap.forEach { (date, name) ->
                    effectiveMap[date] = name
                }

                // 3. 충당 적용 (지근보다 우선 - 지근충당의 경우 지근을 덮어씀)
                shiftInputRecords.forEach { record ->
                    effectiveMap[record.date] = record.targetShiftName
                    shiftInputDisplayMap[record.date] = ShiftInputInfo(
                        shortName = record.shortName,
                        colorHex = record.colorHex,
                        groupId = record.groupId
                    )
                }

                // 4. 지휴 적용 (충당보다 우선)
                lateHolidayMap.forEach { (date, name) ->
                    effectiveMap[date] = name
                }

                data class ShiftScheduleResult(
                    val effectiveMap: Map<LocalDate, String>,
                    val swapDates: Set<LocalDate>,
                    val shiftInputDisplayMap: Map<LocalDate, ShiftInputInfo>
                )
                ShiftScheduleResult(effectiveMap.toMap(), swapMap.keys, shiftInputDisplayMap.toMap())
            }.collect { result ->
                _state.update {
                    it.copy(
                        shiftScheduleMap = result.effectiveMap,
                        swapDates = result.swapDates,
                        shiftInputMap = result.shiftInputDisplayMap
                    )
                }
            }
        }
    }

    private fun observeVacationRecords() {
        viewModelScope.launch {
            vacationRecordRepository.getAllRecords().collect { records ->
                val map = records.associate { it.date to it.shortName }
                _state.update { it.copy(vacationMap = map) }
            }
        }
    }

    private fun loadCalendarEvents() {
        viewModelScope.launch {
            try {
                // 현재 날짜 기준 앞뒤 2년 범위의 이벤트 로드 (매년 반복 일정 포함)
                val today = LocalDate.now()
                val startDate = today.minusMonths(6)
                val endDate = today.plusMonths(24)

                val events = deviceCalendarRepository.getEvents(startDate, endDate)

                // 날짜별로 그룹화 (여러 날에 걸친 이벤트는 각 날짜에 추가)
                val eventsByDate = mutableMapOf<LocalDate, MutableList<CalendarEvent>>()

                events.forEach { event ->
                    var currentDate = event.startDate
                    val eventEndDate = event.endDate

                    while (!currentDate.isAfter(eventEndDate)) {
                        eventsByDate.getOrPut(currentDate) { mutableListOf() }.add(event)
                        currentDate = currentDate.plusDays(1)
                    }
                }

                _state.update { it.copy(eventsByDate = eventsByDate) }
            } catch (e: SecurityException) {
                // 권한이 없는 경우
                _state.update { it.copy(eventsByDate = emptyMap()) }
            }
        }
    }

    fun refreshCalendarEvents() {
        loadCalendarEvents()
    }

    fun refreshHolidays() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingHolidays = true) }
            val result = holidayRepository.refreshHolidays()
            _state.update { it.copy(isRefreshingHolidays = false) }

            if (result.isSuccess) {
                _event.emit(HomeEvent.ShowMessage("공휴일 정보가 갱신되었습니다 (${result.getOrNull()}개)"))
            } else {
                _event.emit(HomeEvent.ShowMessage("공휴일 갱신 실패: ${result.exceptionOrNull()?.message}"))
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnDateClick -> {
                viewModelScope.launch {
                    _event.emit(HomeEvent.NavigateToDateDetail(action.date.toString()))
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.saveThemeMode(mode)
        }
    }

    fun toggleShowCrewPattern(show: Boolean) {
        viewModelScope.launch {
            crewPatternPreferences.saveShowCrewPattern(show)
        }
    }

    private fun observeAnniversaries() {
        viewModelScope.launch {
            // 기념일 목록 변화 또는 표시 연도 변화 시 모두 재계산
            combine(
                anniversaryRepository.getAll(),
                _visibleYear
            ) { list, year -> list to year }
                .collect { (list, year) ->
                    val map = buildAnniversaryMap(year)
                    _state.update { it.copy(anniversaryMap = map) }
                }
        }
    }

    fun onVisibleYearChanged(year: Int) {
        if (_visibleYear.value != year) {
            _visibleYear.value = year
        }
    }

    private suspend fun buildAnniversaryMap(year: Int): Map<LocalDate, String> {
        val raw = anniversaryRepository.getAnniversaryMapForYear(year)
        return raw.mapNotNull { (key, value) ->
            try { LocalDate.parse(key) to value } catch (e: Exception) { null }
        }.toMap()
    }

    private fun loadShiftPattern() {
        viewModelScope.launch {
            // Flow로 구독하여 config 변경 시 자동 갱신
            shiftRepository.getUserConfig().collect { config ->
                if (config == null) {
                    _state.update {
                        it.copy(
                            shiftPattern = emptyList(),
                            holidayWorkShifts = emptyList(),
                            isCustomShift = false,
                            officeName = null
                        )
                    }
                    return@collect
                }

                val isCustomShiftConfig = config.officeCode <= -10000
                val pattern = config.shiftPattern

                // 승무소 정보 가져와서 diaTurns3 파싱
                val office = officeRepository.getOfficeByCode(config.officeCode)
                val holidayWorkShifts = office?.diaTurns3?.let { raw ->
                    try {
                        raw.replace("[", "")
                            .replace("]", "")
                            .replace("{", "")
                            .replace("}", "")
                            .replace("\"", "")
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()

                _state.update {
                    it.copy(
                        shiftPattern = pattern,
                        holidayWorkShifts = holidayWorkShifts,
                        isCustomShift = isCustomShiftConfig,
                        officeName = config.officeName
                    )
                }
            }
        }
    }

    // ===== 백업 및 복원 =====

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.exportToFile(uri)
            if (result.isSuccess) {
                _event.emit(HomeEvent.ShowMessage("백업이 완료되었습니다"))
            } else {
                _event.emit(HomeEvent.ShowMessage("백업 실패: ${result.exceptionOrNull()?.message}"))
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val readResult = backupRepository.readFromFile(uri)
            if (readResult.isFailure) {
                _event.emit(HomeEvent.ShowMessage("백업 파일을 읽을 수 없습니다: ${readResult.exceptionOrNull()?.message}"))
                return@launch
            }

            val backupData = readResult.getOrNull() ?: return@launch
            val restoreResult = backupRepository.restoreFromBackup(backupData, clearExisting = true)

            if (restoreResult.isSuccess) {
                val count = restoreResult.getOrNull() ?: 0
                _event.emit(HomeEvent.ShowMessage("복원이 완료되었습니다 (${count}개 항목)"))
                _event.emit(HomeEvent.BackupRestored)
            } else {
                _event.emit(HomeEvent.ShowMessage("복원 실패: ${restoreResult.exceptionOrNull()?.message}"))
            }
        }
    }
}
