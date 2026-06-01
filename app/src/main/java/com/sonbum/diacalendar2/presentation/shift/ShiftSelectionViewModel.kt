package com.sonbum.diacalendar2.presentation.shift

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.mapper.toOffice
import com.sonbum.diacalendar2.domain.model.Office
import com.sonbum.diacalendar2.domain.model.UserShiftConfig
import com.sonbum.diacalendar2.domain.repository.DiaRepository
import com.sonbum.diacalendar2.domain.model.CustomShift
import com.sonbum.diacalendar2.domain.repository.CustomShiftRepository
import com.sonbum.diacalendar2.domain.repository.LocalOfficeRepository
import com.sonbum.diacalendar2.domain.repository.OfficeRepository
import com.sonbum.diacalendar2.domain.repository.ShiftRepository
import com.sonbum.diacalendar2.widget.WidgetUpdater
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TAG = "ShiftSelectionVM"

enum class OfficeSource {
    SERVER, LOCAL, CUSTOM_SHIFT
}

data class ShiftSelectionState(
    val officeSource: OfficeSource = OfficeSource.SERVER,
    val offices: List<Office> = emptyList(),
    val localOffices: List<Office> = emptyList(),
    val filteredOffices: List<Office> = emptyList(),
    val selectedOffice: Office? = null,
    val searchQuery: String = "",
    val isDropdownExpanded: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    // 새로 추가된 필드들
    val selectedPosition: UserShiftConfig.Position? = null,
    val startDate: LocalDate = LocalDate.now(),
    val selectedTodayShift: String? = null,
    val selectedTodayShiftIndex: Int? = null,           // 기준 근무의 패턴 내 인덱스 (중복 근무명 구분용)
    val selectedTodayShiftAvailableIndex: Int? = null,  // 기준 근무의 availableShifts 내 인덱스 (UI 표시/선택 강조용)
    val referenceDate: LocalDate = LocalDate.now(),    // 기준교번의 기준 날짜
    val availableShifts: List<String> = emptyList(),   // dia_selects 파싱 결과
    val shiftPattern: List<String> = emptyList(),      // 선택된 포지션의 교번 패턴
    val hasExistingConfig: Boolean = false,             // 기존 설정이 있는지 여부
    val existingOfficeName: String? = null,             // 기존 설정된 승무소 이름
    val showDeleteConfirmDialog: Boolean = false,       // 삭제 확인 다이얼로그
    val isDeleting: Boolean = false,                     // 삭제 진행 중
    // 교대근무자 관련
    val customShifts: List<CustomShift> = emptyList(),
    val selectedCustomShift: CustomShift? = null
)

sealed interface ShiftSelectionEvent {
    data class ShowMessage(val message: String) : ShiftSelectionEvent
    data object SaveSuccess : ShiftSelectionEvent
    data object DeleteSuccess : ShiftSelectionEvent
}

class ShiftSelectionViewModel(
    private val officeRepository: OfficeRepository,
    private val diaRepository: DiaRepository,
    private val shiftRepository: ShiftRepository,
    private val localOfficeRepository: LocalOfficeRepository,
    private val customShiftRepository: CustomShiftRepository,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ShiftSelectionState())
    val state: StateFlow<ShiftSelectionState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<ShiftSelectionEvent>()
    val event: SharedFlow<ShiftSelectionEvent> = _event.asSharedFlow()

    init {
        loadOffices()
        loadLocalOffices()
        loadCustomShifts()
        loadExistingConfig()
    }

    private fun loadOffices() {
        viewModelScope.launch {
            Log.d(TAG, "loadOffices() - 승무소 목록 로드 시작")
            _state.update { it.copy(isLoading = true, error = null) }

            // 먼저 로컬 DB에서 불러오기
            officeRepository.getAllOffices().collect { offices ->
                Log.d(TAG, "loadOffices() - 로컬 DB에서 ${offices.size}개 승무소 로드")
                _state.update {
                    it.copy(
                        offices = offices,
                        filteredOffices = filterOffices(offices, it.searchQuery),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadExistingConfig() {
        viewModelScope.launch {
            val existingConfig = shiftRepository.getUserConfigOnce()
            if (existingConfig != null) {
                Log.d(TAG, "loadExistingConfig() - 기존 설정 발견: ${existingConfig.officeName}")
                // 기존 설정이 있으면 UI에 반영
                val office = _state.value.offices.find { it.officeCode == existingConfig.officeCode }
                if (office != null) {
                    val availableShifts = parseShiftList(office.diaSelects)
                    val availableIndex = mapPatternIndexToAvailableIndex(
                        shift = existingConfig.todayShift,
                        patternIndex = existingConfig.todayShiftIndex,
                        shiftPattern = existingConfig.shiftPattern,
                        availableShifts = availableShifts
                    )
                    _state.update {
                        it.copy(
                            selectedOffice = office,
                            searchQuery = office.officeName,
                            selectedPosition = existingConfig.position,
                            startDate = existingConfig.startDate,
                            selectedTodayShift = existingConfig.todayShift,
                            selectedTodayShiftIndex = existingConfig.todayShiftIndex,
                            selectedTodayShiftAvailableIndex = availableIndex,
                            referenceDate = existingConfig.referenceDate,
                            availableShifts = availableShifts,
                            shiftPattern = existingConfig.shiftPattern,
                            hasExistingConfig = true,
                            existingOfficeName = existingConfig.officeName
                        )
                    }
                } else {
                    // office를 찾지 못해도 기존 설정이 있음을 표시
                    _state.update {
                        it.copy(
                            hasExistingConfig = true,
                            existingOfficeName = existingConfig.officeName
                        )
                    }
                }
            }
        }
    }

    private fun loadLocalOffices() {
        viewModelScope.launch {
            localOfficeRepository.getAllLocalOffices().collect { localOfficeList ->
                val officesConverted = localOfficeList.map { it.toOffice() }
                _state.update { state ->
                    state.copy(
                        localOffices = officesConverted,
                        // 현재 로컬 소스면 필터링도 갱신
                        filteredOffices = if (state.officeSource == OfficeSource.LOCAL) {
                            filterOffices(officesConverted, state.searchQuery)
                        } else {
                            state.filteredOffices
                        }
                    )
                }
            }
        }
    }

    private fun loadCustomShifts() {
        viewModelScope.launch {
            customShiftRepository.getAllCustomShifts().collect { shifts ->
                _state.update { it.copy(customShifts = shifts) }
            }
        }
    }

    fun onCustomShiftSelected(shift: CustomShift) {
        Log.d(TAG, "onCustomShiftSelected() - 교대근무 선택: ${shift.shiftName}")
        Log.d(TAG, "교대근무 패턴: ${shift.shiftPattern}")

        _state.update {
            it.copy(
                selectedCustomShift = shift,
                selectedOffice = null,
                searchQuery = "",
                isDropdownExpanded = false,
                shiftPattern = shift.shiftPattern,
                availableShifts = shift.shiftPattern,
                selectedPosition = null,
                selectedTodayShift = null,
                selectedTodayShiftIndex = null,
                selectedTodayShiftAvailableIndex = null,
                referenceDate = LocalDate.now()
            )
        }
    }

    fun onOfficeSourceChange(source: OfficeSource) {
        val currentSource = _state.value.officeSource
        if (currentSource == source) return

        val targetList = when (source) {
            OfficeSource.SERVER -> _state.value.offices
            OfficeSource.LOCAL -> _state.value.localOffices
            OfficeSource.CUSTOM_SHIFT -> emptyList()
        }

        _state.update {
            it.copy(
                officeSource = source,
                filteredOffices = filterOffices(targetList, ""),
                // 소스 전환 시 선택 초기화
                selectedOffice = null,
                selectedCustomShift = null,
                searchQuery = "",
                isDropdownExpanded = false,
                selectedPosition = null,
                selectedTodayShift = null,
                selectedTodayShiftIndex = null,
                selectedTodayShiftAvailableIndex = null,
                referenceDate = LocalDate.now(),
                availableShifts = emptyList(),
                shiftPattern = emptyList(),
                startDate = LocalDate.now()
            )
        }
    }

    fun refreshOfficesFromServer() {
        viewModelScope.launch {
            Log.d(TAG, "refreshOfficesFromServer() - 서버에서 승무소 목록 새로고침 시작")
            _state.update { it.copy(isLoading = true, error = null) }

            val result = officeRepository.refreshOffices()
            result.fold(
                onSuccess = { count ->
                    Log.d(TAG, "refreshOfficesFromServer() - 서버에서 $count 개 승무소 불러오기 성공")
                    _event.emit(ShiftSelectionEvent.ShowMessage("$count 개의 승무소를 불러왔습니다."))
                },
                onFailure = { error ->
                    Log.e(TAG, "refreshOfficesFromServer() - 오류: ${error.message}", error)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "승무소 목록을 불러오는데 실패했습니다: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update {
            val sourceList = when (it.officeSource) {
                OfficeSource.SERVER -> it.offices
                OfficeSource.LOCAL -> it.localOffices
                OfficeSource.CUSTOM_SHIFT -> emptyList()
            }
            it.copy(
                searchQuery = query,
                filteredOffices = filterOffices(sourceList, query),
                isDropdownExpanded = query.isNotEmpty() || it.isDropdownExpanded
            )
        }
    }

    fun onDropdownExpandedChange(expanded: Boolean) {
        _state.update { it.copy(isDropdownExpanded = expanded) }
        // 드롭다운 열 때 서버 승무소면 백그라운드로 최신 데이터 갱신
        if (expanded && _state.value.officeSource == OfficeSource.SERVER) {
            viewModelScope.launch {
                try {
                    officeRepository.refreshOffices()
                } catch (_: Exception) {
                    // 무음 실패 - 기존 캐시 사용
                }
            }
        }
    }

    fun onOfficeSelected(office: Office) {
        Log.d(TAG, "onOfficeSelected() - 승무소 선택: ${office.officeName} (코드: ${office.officeCode})")
        Log.d(TAG, "dia_selects: ${office.diaSelects}")
        Log.d(TAG, "dia_turns1: ${office.diaTurns1}")
        Log.d(TAG, "dia_turns2: ${office.diaTurns2}")
        Log.d(TAG, "sub_turns: ${office.subTurns}")

        val availableShifts = parseShiftList(office.diaSelects)
        Log.d(TAG, "파싱된 교번 목록: $availableShifts")

        // 기존 설정과 다른 승무소를 선택한 경우 모든 설정 초기화
        val currentOffice = _state.value.selectedOffice
        val isNewOffice = currentOffice == null || currentOffice.officeCode != office.officeCode

        _state.update {
            it.copy(
                selectedOffice = office,
                searchQuery = office.officeName,
                isDropdownExpanded = false,
                availableShifts = availableShifts,
                // 승무소 변경시 포지션, 오늘근무 초기화, 시작날짜를 오늘로 리셋
                selectedPosition = if (isNewOffice) null else it.selectedPosition,
                selectedTodayShift = if (isNewOffice) null else it.selectedTodayShift,
                selectedTodayShiftIndex = if (isNewOffice) null else it.selectedTodayShiftIndex,
                selectedTodayShiftAvailableIndex = if (isNewOffice) null else it.selectedTodayShiftAvailableIndex,
                referenceDate = if (isNewOffice) LocalDate.now() else it.referenceDate,
                shiftPattern = if (isNewOffice) emptyList() else it.shiftPattern,
                startDate = if (isNewOffice) LocalDate.now() else it.startDate
            )
        }
    }

    fun onPositionSelected(position: UserShiftConfig.Position) {
        val office = _state.value.selectedOffice ?: return

        Log.d(TAG, "onPositionSelected() - 포지션 선택: ${position.displayName}")

        // 선택된 포지션에 따라 해당 교번 패턴 가져오기
        val patternString = when (position) {
            UserShiftConfig.Position.ENGINEER -> office.diaTurns1
            UserShiftConfig.Position.CONDUCTOR -> office.diaTurns2
            UserShiftConfig.Position.FOUR_SHIFT -> office.subTurns
        }

        val pattern = parseShiftList(patternString)
        Log.d(TAG, "교번 패턴: $pattern")

        _state.update {
            it.copy(
                selectedPosition = position,
                shiftPattern = pattern,
                // 포지션 변경시 오늘근무 초기화
                selectedTodayShift = null,
                selectedTodayShiftIndex = null,
                selectedTodayShiftAvailableIndex = null
            )
        }
    }

    fun onStartDateSelected(date: LocalDate) {
        Log.d(TAG, "onStartDateSelected() - 시작 날짜 선택: $date")
        _state.update { it.copy(startDate = date) }
    }

    fun onReferenceDateSelected(date: LocalDate) {
        Log.d(TAG, "onReferenceDateSelected() - 기준 날짜 선택: $date")
        _state.update { it.copy(referenceDate = date) }
    }

    fun onTodayShiftSelected(shift: String, index: Int) {
        // 교대근무자: availableShifts == shiftPattern이므로 index가 패턴 내 인덱스와 동일
        // 서버/로컬: availableShifts는 diaSelects, 중복이 있을 수 있으므로 occurrence(N번째 등장)로 shiftPattern 인덱스 매칭
        val patternIndex = if (_state.value.officeSource == OfficeSource.CUSTOM_SHIFT) {
            index
        } else {
            val availableShifts = _state.value.availableShifts
            val shiftPattern = _state.value.shiftPattern
            // availableShifts[index]까지 동일 근무명이 몇 번째 등장했는지 계산
            val occurrence = availableShifts.take(index + 1).count { it == shift }
            // shiftPattern에서 동일한 occurrence(N번째 등장) 위치 찾기
            var found = -1
            var seen = 0
            for (i in shiftPattern.indices) {
                if (shiftPattern[i] == shift) {
                    seen++
                    if (seen == occurrence) {
                        found = i
                        break
                    }
                }
            }
            // 매칭 실패 시 기존 동작(첫 번째 매칭)으로 fallback
            if (found >= 0) found else shiftPattern.indexOf(shift)
        }
        Log.d(TAG, "onTodayShiftSelected() - 기준 근무 선택: $shift (availableShifts 인덱스: $index, 패턴 인덱스: $patternIndex)")
        _state.update {
            it.copy(
                selectedTodayShift = shift,
                selectedTodayShiftIndex = patternIndex,
                selectedTodayShiftAvailableIndex = index
            )
        }
    }

    fun clearSelection() {
        _state.update {
            val sourceList = when (it.officeSource) {
                OfficeSource.SERVER -> it.offices
                OfficeSource.LOCAL -> it.localOffices
                OfficeSource.CUSTOM_SHIFT -> emptyList()
            }
            it.copy(
                selectedOffice = null,
                selectedCustomShift = null,
                searchQuery = "",
                filteredOffices = sourceList,
                selectedPosition = null,
                selectedTodayShift = null,
                selectedTodayShiftIndex = null,
                selectedTodayShiftAvailableIndex = null,
                referenceDate = LocalDate.now(),
                availableShifts = emptyList(),
                shiftPattern = emptyList(),
                startDate = LocalDate.now()
            )
        }
    }

    fun saveSelectedOfficeData() {
        val isCustomShift = _state.value.officeSource == OfficeSource.CUSTOM_SHIFT
        val selectedCustomShift = _state.value.selectedCustomShift
        val selectedOffice = _state.value.selectedOffice

        // 교대근무자인 경우 CustomShift가 선택되어야 함
        if (isCustomShift && selectedCustomShift == null) {
            viewModelScope.launch {
                _event.emit(ShiftSelectionEvent.ShowMessage("교대근무를 선택해주세요"))
            }
            return
        }
        // 서버/로컬인 경우 Office가 선택되어야 함
        if (!isCustomShift && selectedOffice == null) return

        val selectedPosition = if (isCustomShift) {
            UserShiftConfig.Position.ENGINEER // 교대근무자는 포지션 불필요, 기본값 사용
        } else {
            _state.value.selectedPosition ?: run {
                viewModelScope.launch {
                    _event.emit(ShiftSelectionEvent.ShowMessage("포지션을 선택해주세요"))
                }
                return
            }
        }

        val todayShift = _state.value.selectedTodayShift ?: run {
            viewModelScope.launch {
                _event.emit(ShiftSelectionEvent.ShowMessage("기준 근무를 선택해주세요"))
            }
            return
        }
        val shiftPattern = _state.value.shiftPattern
        if (shiftPattern.isEmpty()) {
            viewModelScope.launch {
                _event.emit(ShiftSelectionEvent.ShowMessage("교번 패턴이 없습니다"))
            }
            return
        }

        // 교대근무자용 officeCode와 officeName 결정
        val officeCode = if (isCustomShift) {
            -(10000 + selectedCustomShift!!.id)
        } else {
            selectedOffice!!.officeCode
        }
        val officeName = if (isCustomShift) {
            selectedCustomShift!!.shiftName
        } else {
            selectedOffice!!.officeName
        }

        viewModelScope.launch {
            Log.d(TAG, "========================================")
            Log.d(TAG, "saveSelectedOfficeData() - 근무표 저장 시작")
            Log.d(TAG, "소스: ${_state.value.officeSource}")
            Log.d(TAG, "이름: $officeName")
            Log.d(TAG, "코드: $officeCode")
            Log.d(TAG, "포지션: ${selectedPosition.displayName}")
            Log.d(TAG, "시작 날짜: ${_state.value.startDate}")
            Log.d(TAG, "기준 날짜: ${_state.value.referenceDate}")
            Log.d(TAG, "기준 근무: $todayShift")
            Log.d(TAG, "교번 패턴: $shiftPattern")
            Log.d(TAG, "========================================")

            _state.update { it.copy(isSaving = true) }

            try {
                // 1. 시작 날짜 이후 기존 스케줄만 삭제 (이전 근무 순서는 보존)
                val existingScheduleCount = shiftRepository.getScheduleCount()
                Log.d(TAG, "기존 저장된 스케줄 개수: $existingScheduleCount")
                if (existingScheduleCount > 0) {
                    Log.d(TAG, "시작 날짜(${_state.value.startDate}) 이후 스케줄 삭제 중...")
                    shiftRepository.deleteSchedulesFromDate(_state.value.startDate)
                    Log.d(TAG, "시작 날짜 이후 스케줄 삭제 완료 (이전 스케줄 보존)")
                }

                // 2. 서버 승무소만 Supabase에서 데이터 가져오기
                if (!isCustomShift && officeCode >= 0) {
                    val existingDiaCount = diaRepository.getDiaCount()
                    Log.d(TAG, "기존 저장된 근무표 개수: $existingDiaCount")

                    val diaResult = diaRepository.refreshDiasByOfficeId(officeCode.toInt())

                    diaResult.fold(
                        onSuccess = { count ->
                            Log.d(TAG, "근무표 $count 개 저장 완료")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "근무표 저장 실패: ${error.message}", error)
                        }
                    )
                } else {
                    Log.d(TAG, "로컬/교대근무자 - 서버 데이터 가져오기 건너뜀")
                }

                // 3. 사용자 설정 저장
                val todayShiftIndex = _state.value.selectedTodayShiftIndex
                val userConfig = UserShiftConfig(
                    officeCode = officeCode,
                    officeName = officeName,
                    position = selectedPosition,
                    shiftPattern = shiftPattern,
                    startDate = _state.value.startDate,
                    todayShift = todayShift,
                    todayShiftIndex = todayShiftIndex,
                    referenceDate = _state.value.referenceDate
                )
                shiftRepository.saveUserConfig(userConfig)
                Log.d(TAG, "사용자 설정 저장 완료")

                // 4. 3년치 스케줄 생성
                val scheduleCount = shiftRepository.generateAndSaveSchedules(
                    shiftPattern = shiftPattern,
                    startDate = _state.value.startDate,
                    todayShift = todayShift,
                    referenceDate = _state.value.referenceDate,
                    years = 3,
                    todayShiftIndex = todayShiftIndex
                )
                Log.d(TAG, "3년치 스케줄 $scheduleCount 개 생성 완료")

                _state.update { it.copy(isSaving = false) }
                WidgetUpdater.updateAll(appContext)
                com.sonbum.diacalendar2.core.notification.ShiftReminderWorker.enqueue(appContext)
                _event.emit(ShiftSelectionEvent.ShowMessage("${officeName}의 근무 설정이 완료되었습니다. (${scheduleCount}일)"))
                _event.emit(ShiftSelectionEvent.SaveSuccess)

            } catch (e: Exception) {
                Log.e(TAG, "saveSelectedOfficeData() - 저장 실패: ${e.message}", e)
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = "근무표 저장에 실패했습니다: ${e.message}"
                    )
                }
            }
        }
    }

    fun showDeleteConfirmDialog() {
        _state.update { it.copy(showDeleteConfirmDialog = true) }
    }

    fun dismissDeleteConfirmDialog() {
        _state.update { it.copy(showDeleteConfirmDialog = false) }
    }

    fun deleteAllShiftData() {
        viewModelScope.launch {
            _state.update { it.copy(showDeleteConfirmDialog = false, isDeleting = true) }
            try {
                shiftRepository.deleteAllSchedules()
                shiftRepository.deleteUserConfig()
                Log.d(TAG, "deleteAllShiftData() - 근무 스케줄 및 설정 삭제 완료")

                _state.update {
                    it.copy(
                        isDeleting = false,
                        hasExistingConfig = false,
                        existingOfficeName = null,
                        selectedOffice = null,
                        selectedCustomShift = null,
                        searchQuery = "",
                        selectedPosition = null,
                        selectedTodayShift = null,
                        selectedTodayShiftIndex = null,
                        selectedTodayShiftAvailableIndex = null,
                        referenceDate = LocalDate.now(),
                        availableShifts = emptyList(),
                        shiftPattern = emptyList(),
                        startDate = LocalDate.now()
                    )
                }
                WidgetUpdater.updateAll(appContext)
                _event.emit(ShiftSelectionEvent.ShowMessage("생성된 근무가 삭제되었습니다."))
                _event.emit(ShiftSelectionEvent.DeleteSuccess)
            } catch (e: Exception) {
                Log.e(TAG, "deleteAllShiftData() - 삭제 실패: ${e.message}", e)
                _state.update {
                    it.copy(
                        isDeleting = false,
                        error = "근무 삭제에 실패했습니다: ${e.message}"
                    )
                }
            }
        }
    }

    private fun filterOffices(offices: List<Office>, query: String): List<Office> {
        if (query.isBlank()) return offices
        return offices.filter {
            it.officeName.contains(query, ignoreCase = true)
        }
    }

    /**
     * 쉼표로 구분된 문자열을 List<String>으로 파싱
     * JSON 배열 형식도 지원: ["1","2","비"] → ["1", "2", "비"]
     * 일반 형식도 지원: "1,2,3,비,주" → ["1", "2", "3", "비", "주"]
     */
    /**
     * 저장된 패턴 인덱스를 availableShifts(드롭다운 표시 목록) 내의 인덱스로 변환.
     * shiftPattern에서 해당 shift의 occurrence(N번째 등장)를 구한 뒤, availableShifts에서
     * 같은 occurrence를 찾는다. 매칭 실패 시 첫 번째 등장 위치로 fallback.
     */
    private fun mapPatternIndexToAvailableIndex(
        shift: String,
        patternIndex: Int?,
        shiftPattern: List<String>,
        availableShifts: List<String>
    ): Int? {
        if (availableShifts.isEmpty()) return null
        if (patternIndex == null || patternIndex !in shiftPattern.indices) {
            val first = availableShifts.indexOf(shift)
            return if (first >= 0) first else null
        }
        // 패턴 내에서 patternIndex가 shift의 몇 번째 등장인지 계산
        val occurrence = shiftPattern.take(patternIndex + 1).count { it == shift }
        var seen = 0
        for (i in availableShifts.indices) {
            if (availableShifts[i] == shift) {
                seen++
                if (seen == occurrence) return i
            }
        }
        val fallback = availableShifts.indexOf(shift)
        return if (fallback >= 0) fallback else null
    }

    private fun parseShiftList(input: String?): List<String> {
        if (input.isNullOrBlank()) return emptyList()
        return input
            .replace("[", "")
            .replace("]", "")
            .replace("{", "")
            .replace("}", "")
            .replace("\"", "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
