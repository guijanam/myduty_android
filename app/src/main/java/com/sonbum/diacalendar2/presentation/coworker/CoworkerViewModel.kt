package com.sonbum.diacalendar2.presentation.coworker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.datastore.CoworkerPreferences
import com.sonbum.diacalendar2.domain.model.Coworker
import com.sonbum.diacalendar2.domain.model.CoworkerGroup
import com.sonbum.diacalendar2.domain.repository.CoworkerRepository
import com.sonbum.diacalendar2.domain.repository.HolidayRepository
import com.sonbum.diacalendar2.domain.usecase.EffectiveShift
import com.sonbum.diacalendar2.domain.usecase.EffectiveShiftUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class CoworkerTab { CALENDAR, LIST }

data class CoworkerUiState(
    val coworkers: List<Coworker> = emptyList(),
    val groups: List<CoworkerGroup> = emptyList(),
    /** null = 전체 */
    val selectedGroupId: Long? = null,
    val myScheduleMap: Map<LocalDate, EffectiveShift> = emptyMap(),
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
    private val holidayRepository: HolidayRepository,
    private val effectiveShiftUseCase: EffectiveShiftUseCase,
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
                // 유효 교번 우선순위: 근태 > 지휴 > 충당 > 지근 > 교체 > 원래 교번
                effectiveShiftUseCase.observe()
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
