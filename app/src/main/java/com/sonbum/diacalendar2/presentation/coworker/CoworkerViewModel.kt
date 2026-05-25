package com.sonbum.diacalendar2.presentation.coworker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.datastore.CoworkerPreferences
import com.sonbum.diacalendar2.domain.model.Coworker
import com.sonbum.diacalendar2.domain.model.CoworkerGroup
import com.sonbum.diacalendar2.domain.repository.CoworkerRepository
import com.sonbum.diacalendar2.domain.repository.HolidayRepository
import com.sonbum.diacalendar2.domain.repository.LateHolidayRecordRepository
import com.sonbum.diacalendar2.domain.repository.LateWorkRecordRepository
import com.sonbum.diacalendar2.domain.repository.ShiftInputRecordRepository
import com.sonbum.diacalendar2.domain.repository.ShiftRepository
import com.sonbum.diacalendar2.domain.repository.ShiftSwapRecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class CoworkerTab { CALENDAR, LIST }

data class CoworkerUiState(
    val coworkers: List<Coworker> = emptyList(),
    val groups: List<CoworkerGroup> = emptyList(),
    /** null = 전체 */
    val selectedGroupId: Long? = null,
    val myScheduleMap: Map<LocalDate, String> = emptyMap(),
    /** coworkerId → 해당 월 날짜→근무 Map (캐시) */
    val coworkerSchedules: Map<Long, Map<LocalDate, String>> = emptyMap(),
    val holidayMap: Map<LocalDate, String> = emptyMap(),
    val currentYear: Int = LocalDate.now().year,
    val currentMonth: Int = LocalDate.now().monthValue,
    val selectedTab: CoworkerTab = CoworkerTab.CALENDAR,
    val isLoading: Boolean = true
) {
    val filteredCoworkers: List<Coworker>
        get() = if (selectedGroupId == null) coworkers
                else coworkers.filter { selectedGroupId in it.groupIds }
}

class CoworkerViewModel(
    private val coworkerRepository: CoworkerRepository,
    private val shiftRepository: ShiftRepository,
    private val holidayRepository: HolidayRepository,
    private val shiftSwapRecordRepository: ShiftSwapRecordRepository,
    private val shiftInputRecordRepository: ShiftInputRecordRepository,
    private val lateWorkRecordRepository: LateWorkRecordRepository,
    private val lateHolidayRecordRepository: LateHolidayRecordRepository,
    private val coworkerPreferences: CoworkerPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(CoworkerUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            coworkerPreferences.selectedGroupId.collect { savedGroupId ->
                _state.update { it.copy(selectedGroupId = savedGroupId) }
                recalculateSchedules()
            }
        }
        viewModelScope.launch {
            combine(
                coworkerRepository.getAllCoworkers(),
                coworkerRepository.getAllGroups(),
                // 유효 교번 우선순위: 교체 → 지근 → 충당 → 지휴
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
                    val effectiveMap = scheduleMap.toMutableMap()
                    // 1. 근무 교체 적용
                    swapMap.forEach { (date, swappedName) ->
                        effectiveMap[date] = swappedName
                    }
                    // 2. 지근 적용 (교체보다 우선)
                    lateWorkMap.forEach { (date, name) ->
                        effectiveMap[date] = name
                    }
                    // 3. 충당 적용 (지근보다 우선)
                    shiftInputRecords.forEach { record ->
                        effectiveMap[record.date] = record.targetShiftName
                    }
                    // 4. 지휴 적용 (충당보다 우선)
                    lateHolidayMap.forEach { (date, name) ->
                        effectiveMap[date] = name
                    }
                    effectiveMap.toMap()
                }
            ) { coworkers, groups, effectiveMyMap ->
                Triple(coworkers, groups, effectiveMyMap)
            }.collect { (coworkers, groups, myMap) ->
                _state.update {
                    it.copy(
                        coworkers = coworkers,
                        groups = groups,
                        myScheduleMap = myMap,
                        isLoading = false
                    )
                }
                recalculateSchedules()
            }
        }
        viewModelScope.launch {
            holidayRepository.getHolidayMap().collect { map ->
                _state.update { it.copy(holidayMap = map) }
            }
        }
    }

    fun onTabSelected(tab: CoworkerTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun onGroupSelected(groupId: Long?) {
        _state.update { it.copy(selectedGroupId = groupId) }
        recalculateSchedules()
        viewModelScope.launch {
            coworkerPreferences.setSelectedGroupId(groupId)
        }
    }

    fun onMonthChanged(year: Int, month: Int) {
        _state.update { it.copy(currentYear = year, currentMonth = month, coworkerSchedules = emptyMap()) }
        recalculateSchedules()
    }

    fun reorderCoworkers(fromIndex: Int, toIndex: Int) {
        val filtered = _state.value.filteredCoworkers.toMutableList()
        if (fromIndex !in filtered.indices || toIndex !in filtered.indices) return

        // filteredCoworkers 재정렬
        val item = filtered.removeAt(fromIndex)
        filtered.add(toIndex, item)

        // 전체 목록에서 filtered에 포함된 항목의 sortOrder를 새 순서로, 나머지는 뒤에 붙임
        val filteredIds = filtered.map { it.id }
        val others = _state.value.coworkers.filter { it.id !in filteredIds }
        val reordered = filtered.mapIndexed { idx, c -> c.copy(sortOrder = idx) } +
                        others.mapIndexed { idx, c -> c.copy(sortOrder = filtered.size + idx) }

        _state.update { it.copy(coworkers = reordered) }
        viewModelScope.launch {
            coworkerRepository.updateCoworkerSortOrders(reordered)
        }
    }

    private fun recalculateSchedules() {
        val s = _state.value
        val schedules = mutableMapOf<Long, Map<LocalDate, String>>()
        s.filteredCoworkers.forEach { coworker ->
            schedules[coworker.id] = coworkerRepository.calculateScheduleForMonth(
                coworker, s.currentYear, s.currentMonth
            )
        }
        _state.update { it.copy(coworkerSchedules = schedules) }
    }
}
