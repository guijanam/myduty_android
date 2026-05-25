package com.sonbum.diacalendar2.presentation.coworker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.Coworker
import com.sonbum.diacalendar2.domain.model.CoworkerGroup
import com.sonbum.diacalendar2.domain.model.CustomShift
import com.sonbum.diacalendar2.domain.model.Office
import com.sonbum.diacalendar2.domain.model.UserShiftConfig
import com.sonbum.diacalendar2.domain.repository.CoworkerRepository
import com.sonbum.diacalendar2.domain.repository.CustomShiftRepository
import com.sonbum.diacalendar2.domain.repository.LocalOfficeRepository
import com.sonbum.diacalendar2.domain.repository.OfficeRepository
import com.sonbum.diacalendar2.data.local.mapper.toOffice
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// 패턴 입력 방식
enum class CoworkerPatternSource {
    MANUAL,      // 직접 입력 (기존)
    OFFICE,      // 서버 승무소에서 가져오기
    LOCAL,       // 로컬(내부) 승무소에서 가져오기
    CUSTOM_SHIFT // 교대근무에서 가져오기
}

data class CoworkerEditState(
    val coworkerId: Long? = null,
    val name: String = "",
    // 패턴 소스
    val patternSource: CoworkerPatternSource = CoworkerPatternSource.MANUAL,
    // 직접입력
    val shiftPatternInput: String = "",   // UI용 CSV 문자열
    // 승무소 선택 (서버/로컬)
    val offices: List<Office> = emptyList(),
    val localOffices: List<Office> = emptyList(),
    val filteredOffices: List<Office> = emptyList(),
    val selectedOffice: Office? = null,
    val searchQuery: String = "",
    val isDropdownExpanded: Boolean = false,
    val isOfficesLoading: Boolean = false,
    // 포지션 선택
    val selectedPosition: UserShiftConfig.Position? = null,
    // 교대근무 선택
    val customShifts: List<CustomShift> = emptyList(),
    val selectedCustomShift: CustomShift? = null,
    // 공통
    val startDate: LocalDate = LocalDate.now(),
    val referenceDate: LocalDate? = null,
    val referenceShift: String = "",
    val referenceShiftIndex: Int? = null,           // parsedPattern 내 인덱스 (저장용)
    val referenceShiftAvailableIndex: Int? = null,  // availableShifts 내 인덱스 (드롭다운 선택 강조용)
    val selectedGroupIds: Set<Long> = emptySet(),
    val allGroups: List<CoworkerGroup> = emptyList(),
    val isLoading: Boolean = false
) {
    // 직접 입력한 패턴 파싱
    val parsedManualPattern: List<String>
        get() = shiftPatternInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    // 승무소/교대근무 패턴 (포지션 선택 후 결정)
    val officeShiftPattern: List<String>
        get() = when {
            patternSource == CoworkerPatternSource.CUSTOM_SHIFT && selectedCustomShift != null ->
                selectedCustomShift.shiftPattern
            (patternSource == CoworkerPatternSource.OFFICE || patternSource == CoworkerPatternSource.LOCAL)
                && selectedOffice != null && selectedPosition != null -> {
                val patternStr = when (selectedPosition) {
                    UserShiftConfig.Position.ENGINEER -> selectedOffice.diaTurns1
                    UserShiftConfig.Position.CONDUCTOR -> selectedOffice.diaTurns2
                    UserShiftConfig.Position.FOUR_SHIFT -> selectedOffice.subTurns
                }
                parseShiftList(patternStr)
            }
            else -> emptyList()
        }

    // 실제 사용할 패턴
    val parsedPattern: List<String>
        get() = when (patternSource) {
            CoworkerPatternSource.MANUAL -> parsedManualPattern
            else -> officeShiftPattern
        }

    // 기준교번 선택 시 사용할 shifts (서버/로컬: diaSelects, 교대근무/직접입력: pattern 자체)
    val availableShifts: List<String>
        get() = when {
            patternSource == CoworkerPatternSource.CUSTOM_SHIFT -> officeShiftPattern
            (patternSource == CoworkerPatternSource.OFFICE || patternSource == CoworkerPatternSource.LOCAL)
                && selectedOffice != null -> parseShiftList(selectedOffice.diaSelects)
            else -> parsedManualPattern
        }
}

private fun parseShiftList(input: String?): List<String> {
    if (input.isNullOrBlank()) return emptyList()
    return input
        .replace("[", "").replace("]", "")
        .replace("{", "").replace("}", "")
        .replace("\"", "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

sealed interface CoworkerEditEvent {
    data object SaveSuccess : CoworkerEditEvent
    data object DeleteSuccess : CoworkerEditEvent
    data class Error(val message: String) : CoworkerEditEvent
}

class CoworkerEditViewModel(
    private val coworkerRepository: CoworkerRepository,
    private val officeRepository: OfficeRepository,
    private val localOfficeRepository: LocalOfficeRepository,
    private val customShiftRepository: CustomShiftRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CoworkerEditState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<CoworkerEditEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            coworkerRepository.getAllGroups().collect { groups ->
                _state.update { it.copy(allGroups = groups) }
            }
        }
        viewModelScope.launch {
            _state.update { it.copy(isOfficesLoading = true) }
            officeRepository.getAllOffices().collect { offices ->
                _state.update { it.copy(offices = offices, isOfficesLoading = false) }
            }
        }
        viewModelScope.launch {
            localOfficeRepository.getAllLocalOffices().collect { list ->
                val converted = list.map { it.toOffice() }
                _state.update { it.copy(localOffices = converted) }
            }
        }
        viewModelScope.launch {
            customShiftRepository.getAllCustomShifts().collect { shifts ->
                _state.update { it.copy(customShifts = shifts) }
            }
        }
    }

    fun initialize(coworkerId: Long?) {
        if (coworkerId == null) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val coworker = coworkerRepository.getCoworkerById(coworkerId)
            if (coworker != null) {
                _state.update {
                    it.copy(
                        coworkerId = coworker.id,
                        name = coworker.name,
                        patternSource = CoworkerPatternSource.MANUAL,
                        shiftPatternInput = coworker.shiftPattern.joinToString(","),
                        startDate = coworker.referenceDate ?: LocalDate.now(),
                        referenceDate = coworker.referenceDate,
                        referenceShift = coworker.referenceShift,
                        referenceShiftIndex = coworker.referenceShiftIndex,
                        referenceShiftAvailableIndex = coworker.referenceShiftIndex,
                        selectedGroupIds = coworker.groupIds.toSet(),
                        isLoading = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }

    fun onPatternSourceChange(source: CoworkerPatternSource) {
        _state.update {
            it.copy(
                patternSource = source,
                selectedOffice = null,
                selectedCustomShift = null,
                selectedPosition = null,
                searchQuery = "",
                isDropdownExpanded = false,
                filteredOffices = when (source) {
                    CoworkerPatternSource.OFFICE -> it.offices
                    CoworkerPatternSource.LOCAL -> it.localOffices
                    else -> emptyList()
                },
                referenceShift = "",
                referenceShiftIndex = null,
                referenceShiftAvailableIndex = null
            )
        }
    }

    fun onPatternChange(value: String) = _state.update {
        it.copy(shiftPatternInput = value, referenceShift = "", referenceShiftIndex = null, referenceShiftAvailableIndex = null)
    }

    // 승무소 검색/선택
    fun onSearchQueryChange(query: String) {
        _state.update {
            val sourceList = if (it.patternSource == CoworkerPatternSource.OFFICE) it.offices else it.localOffices
            it.copy(
                searchQuery = query,
                filteredOffices = filterOffices(sourceList, query),
                isDropdownExpanded = query.isNotEmpty() || it.isDropdownExpanded
            )
        }
    }

    fun onDropdownExpandedChange(expanded: Boolean) {
        _state.update { it.copy(isDropdownExpanded = expanded) }
        if (expanded && _state.value.patternSource == CoworkerPatternSource.OFFICE) {
            viewModelScope.launch {
                try { officeRepository.refreshOffices() } catch (_: Exception) {}
            }
        }
    }

    fun onOfficeSelected(office: Office) {
        _state.update {
            it.copy(
                selectedOffice = office,
                searchQuery = office.officeName,
                isDropdownExpanded = false,
                selectedPosition = null,
                referenceShift = "",
                referenceShiftIndex = null,
                referenceShiftAvailableIndex = null
            )
        }
    }

    fun clearOfficeSelection() {
        _state.update {
            val sourceList = if (it.patternSource == CoworkerPatternSource.OFFICE) it.offices else it.localOffices
            it.copy(
                selectedOffice = null,
                searchQuery = "",
                filteredOffices = sourceList,
                selectedPosition = null,
                referenceShift = "",
                referenceShiftIndex = null,
                referenceShiftAvailableIndex = null
            )
        }
    }

    fun onPositionSelected(position: UserShiftConfig.Position) {
        _state.update {
            it.copy(
                selectedPosition = position,
                referenceShift = "",
                referenceShiftIndex = null,
                referenceShiftAvailableIndex = null
            )
        }
    }

    // 교대근무 선택
    fun onCustomShiftSelected(shift: CustomShift) {
        _state.update {
            it.copy(
                selectedCustomShift = shift,
                selectedOffice = null,
                searchQuery = "",
                referenceShift = "",
                referenceShiftIndex = null,
                referenceShiftAvailableIndex = null
            )
        }
    }

    fun onStartDateChange(date: LocalDate) = _state.update { it.copy(startDate = date) }

    fun onReferenceDateChange(date: LocalDate?) = _state.update { it.copy(referenceDate = date) }

    fun onReferenceShiftChange(shift: String, index: Int) = _state.update {
        // 서버/로컬: availableShifts(diaSelects)와 parsedPattern(diaTurns)이 다르므로
        //           availableShifts[index]의 occurrence(N번째 등장)를 구해 parsedPattern에서 같은 occurrence 위치로 매칭
        // 교대근무/직접입력: availableShifts == parsedPattern이므로 index 그대로 사용
        val patternIndex = when (it.patternSource) {
            CoworkerPatternSource.OFFICE,
            CoworkerPatternSource.LOCAL -> {
                val available = it.availableShifts
                val pattern = it.parsedPattern
                val occurrence = available.take(index + 1).count { s -> s == shift }
                var found = -1
                var seen = 0
                for (i in pattern.indices) {
                    if (pattern[i] == shift) {
                        seen++
                        if (seen == occurrence) {
                            found = i
                            break
                        }
                    }
                }
                if (found >= 0) found else pattern.indexOf(shift).takeIf { i -> i >= 0 }
            }
            else -> index
        }
        it.copy(referenceShift = shift, referenceShiftIndex = patternIndex, referenceShiftAvailableIndex = index)
    }

    fun onGroupToggle(groupId: Long) = _state.update {
        val newSet = it.selectedGroupIds.toMutableSet()
        if (newSet.contains(groupId)) newSet.remove(groupId) else newSet.add(groupId)
        it.copy(selectedGroupIds = newSet)
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            viewModelScope.launch { _event.emit(CoworkerEditEvent.Error("이름을 입력해주세요")) }
            return
        }
        if (s.parsedPattern.isEmpty()) {
            viewModelScope.launch { _event.emit(CoworkerEditEvent.Error("근무 패턴을 입력/선택해주세요")) }
            return
        }
        if (s.referenceShift.isBlank()) {
            viewModelScope.launch { _event.emit(CoworkerEditEvent.Error("기준 근무를 선택해주세요")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                coworkerRepository.saveCoworker(
                    Coworker(
                        id = s.coworkerId ?: 0L,
                        name = s.name.trim(),
                        groupIds = s.selectedGroupIds.toList(),
                        shiftPattern = s.parsedPattern,
                        referenceDate = s.referenceDate ?: LocalDate.now(),
                        referenceShift = s.referenceShift,
                        referenceShiftIndex = s.referenceShiftIndex
                    )
                )
                _event.emit(CoworkerEditEvent.SaveSuccess)
            } catch (e: Exception) {
                _event.emit(CoworkerEditEvent.Error("저장 실패: ${e.message}"))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun delete() {
        val id = _state.value.coworkerId ?: return
        viewModelScope.launch {
            try {
                coworkerRepository.deleteCoworker(id)
                _event.emit(CoworkerEditEvent.DeleteSuccess)
            } catch (e: Exception) {
                _event.emit(CoworkerEditEvent.Error("삭제 실패: ${e.message}"))
            }
        }
    }

    private fun filterOffices(offices: List<Office>, query: String): List<Office> {
        if (query.isBlank()) return offices
        return offices.filter { it.officeName.contains(query, ignoreCase = true) }
    }
}
