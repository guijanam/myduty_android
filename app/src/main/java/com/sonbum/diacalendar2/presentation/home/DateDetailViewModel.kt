package com.sonbum.diacalendar2.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.CalendarEvent
import com.sonbum.diacalendar2.domain.model.DeviceCalendar
import com.sonbum.diacalendar2.domain.model.Dia
import com.sonbum.diacalendar2.domain.model.Memo
import com.sonbum.diacalendar2.domain.model.ShiftSwapRecord
import com.sonbum.diacalendar2.domain.model.VacationRecord
import com.sonbum.diacalendar2.domain.model.VacationType
import com.sonbum.diacalendar2.domain.model.LateWorkRecord
import com.sonbum.diacalendar2.domain.model.LateWorkType
import com.sonbum.diacalendar2.domain.model.LateHolidayRecord
import com.sonbum.diacalendar2.domain.model.LateHolidayType
import com.sonbum.diacalendar2.domain.model.LocalDia
import com.sonbum.diacalendar2.domain.model.ShiftInputRecord
import com.sonbum.diacalendar2.domain.model.ShiftInputType
import com.sonbum.diacalendar2.domain.repository.DeviceCalendarRepository
import com.sonbum.diacalendar2.domain.repository.DiaRepository
import com.sonbum.diacalendar2.domain.repository.HolidayRepository
import com.sonbum.diacalendar2.domain.repository.LocalDiaRepository
import com.sonbum.diacalendar2.domain.repository.LocalOfficeRepository
import com.sonbum.diacalendar2.domain.repository.MemoRepository
import com.sonbum.diacalendar2.domain.repository.OfficeRepository
import com.sonbum.diacalendar2.domain.repository.ShiftRepository
import com.sonbum.diacalendar2.domain.repository.ShiftSwapRecordRepository
import com.sonbum.diacalendar2.domain.repository.VacationRecordRepository
import com.sonbum.diacalendar2.domain.repository.VacationTypeRepository
import com.sonbum.diacalendar2.domain.repository.LateWorkRecordRepository
import com.sonbum.diacalendar2.domain.repository.LateWorkTypeRepository
import com.sonbum.diacalendar2.domain.repository.LateHolidayRecordRepository
import com.sonbum.diacalendar2.domain.repository.LateHolidayTypeRepository
import com.sonbum.diacalendar2.domain.repository.ShiftInputRecordRepository
import com.sonbum.diacalendar2.domain.repository.ShiftInputTypeRepository
import com.sonbum.diacalendar2.domain.repository.AnniversaryRepository
import com.sonbum.diacalendar2.data.local.OfficeWebsiteRegistry
import com.sonbum.diacalendar2.domain.util.DayTypeResolver
import com.sonbum.diacalendar2.widget.WidgetUpdater
import com.sonbum.diacalendar2.core.notification.ShiftReminderWorker
import android.content.Context
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class DayViewMode { LIST, TIMEBLOCK }

data class DateDetailState(
    val date: LocalDate = LocalDate.now(),
    val memos: List<Memo> = emptyList(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val availableCalendars: List<DeviceCalendar> = emptyList(),
    val selectedCalendarIds: Set<Long> = emptySet(),
    val holidayName: String? = null,
    val anniversaryName: String? = null,
    val holidayId: String? = null,
    val isUserCreatedHoliday: Boolean = false,
    val isLoading: Boolean = false,
    val shiftName: String? = null,
    val effectiveShiftName: String? = null,
    val shiftDia: Dia? = null,
    val vacationRecord: VacationRecord? = null,
    val vacationTypes: List<VacationType> = emptyList(),
    val shiftSwapRecord: ShiftSwapRecord? = null,
    val shiftPattern: List<String> = emptyList(),
    val availableShifts: List<String> = emptyList(),
    val lateWorkRecord: LateWorkRecord? = null,
    val lateWorkTypes: List<LateWorkType> = emptyList(),
    val lateHolidayRecord: LateHolidayRecord? = null,
    val lateHolidayTypes: List<LateHolidayType> = emptyList(),
    val holidayWorkShifts: List<String> = emptyList(),
    val shiftInputRecord: ShiftInputRecord? = null,
    val shiftInputTypes: List<ShiftInputType> = emptyList(),
    val isCustomShift: Boolean = false,
    val officeName: String? = null,
    val officeWebsiteUrl: String? = null,
    val viewMode: DayViewMode = DayViewMode.LIST
)

class DateDetailViewModel(
    private val memoRepository: MemoRepository,
    private val deviceCalendarRepository: DeviceCalendarRepository,
    private val holidayRepository: HolidayRepository,
    private val shiftRepository: ShiftRepository,
    private val diaRepository: DiaRepository,
    private val localDiaRepository: LocalDiaRepository,
    private val vacationRecordRepository: VacationRecordRepository,
    private val vacationTypeRepository: VacationTypeRepository,
    private val shiftSwapRecordRepository: ShiftSwapRecordRepository,
    private val lateWorkRecordRepository: LateWorkRecordRepository,
    private val lateWorkTypeRepository: LateWorkTypeRepository,
    private val lateHolidayRecordRepository: LateHolidayRecordRepository,
    private val lateHolidayTypeRepository: LateHolidayTypeRepository,
    private val shiftInputRecordRepository: ShiftInputRecordRepository,
    private val shiftInputTypeRepository: ShiftInputTypeRepository,
    private val officeRepository: OfficeRepository,
    private val localOfficeRepository: LocalOfficeRepository,
    private val officeWebsiteRegistry: OfficeWebsiteRegistry,
    private val anniversaryRepository: AnniversaryRepository,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(DateDetailState())
    val state = _state.asStateFlow()

    // 동시에 여러 Flow에서 updateEffectiveShift()가 호출될 때
    // 이전 Job을 취소하고 최신 상태로만 계산하기 위한 Job 추적
    private var effectiveShiftJob: Job? = null

    fun toggleViewMode() {
        _state.update {
            it.copy(
                viewMode = if (it.viewMode == DayViewMode.LIST) DayViewMode.TIMEBLOCK else DayViewMode.LIST
            )
        }
    }

    fun initialize(dateString: String) {
        val date = LocalDate.parse(dateString)
        _state.update { it.copy(date = date) }
        loadMemos(date)
        loadCalendarEvents(date)
        loadAvailableCalendars()
        loadHolidayInfo(date)
        loadShiftInfo(date)
        loadVacationInfo(date)
        loadVacationTypes()
        loadLateWorkInfo(date)
        loadLateWorkTypes()
        loadLateHolidayInfo(date)
        loadLateHolidayTypes()
        loadShiftSwapInfo(date)
        loadShiftInputInfo(date)
        loadShiftInputTypes()
        loadAnniversaryInfo(date)
    }

    private fun loadAnniversaryInfo(date: LocalDate) {
        viewModelScope.launch {
            anniversaryRepository.getAll().collect { list ->
                val map = anniversaryRepository.getAnniversaryMapForYear(date.year)
                _state.update { it.copy(anniversaryName = map[date.toString()]) }
            }
        }
    }

    private fun loadHolidayInfo(date: LocalDate) {
        viewModelScope.launch {
            holidayRepository.getAllHolidays().collect { holidays ->
                val holiday = holidays.find { it.date == date }
                _state.update {
                    it.copy(
                        holidayName = holiday?.name,
                        holidayId = holiday?.id,
                        isUserCreatedHoliday = holiday?.isUserCreated ?: false
                    )
                }
            }
        }
    }

    private fun loadShiftInfo(date: LocalDate) {
        // 설정 정보는 1회만 로드 (변경 가능성 낮음)
        viewModelScope.launch {
            val config = shiftRepository.getUserConfigOnce()
            if (config != null) {
                val isCustomShiftConfig = config.officeCode <= -10000
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

                val websiteUrl = if (!isCustomShiftConfig) {
                    officeWebsiteRegistry.getUrl(config.officeName)
                } else null

                _state.update {
                    it.copy(
                        isCustomShift = isCustomShiftConfig,
                        shiftPattern = config.shiftPattern,
                        holidayWorkShifts = holidayWorkShifts,
                        officeName = config.officeName,
                        officeWebsiteUrl = websiteUrl
                    )
                }
                loadAvailableShifts(config)
            }
        }

        // shiftName을 Flow로 observe → swap/input/lateWork 등과 동일한 타이밍에 effectiveShift 재계산
        viewModelScope.launch {
            shiftRepository.observeScheduleByDate(date).collect { schedule ->
                _state.update { it.copy(shiftName = schedule?.shiftName) }
                updateEffectiveShift()
            }
        }
    }

    private suspend fun loadDiaForShift(
        shiftName: String,
        date: LocalDate,
        config: com.sonbum.diacalendar2.domain.model.UserShiftConfig? = null
    ) {
        val effectiveConfig = config ?: shiftRepository.getUserConfigOnce() ?: return
        val isLocalOffice = effectiveConfig.officeCode < 0

        val holidayDates = holidayRepository.getHolidayDates().first()
        val typeName = DayTypeResolver.resolveTypeName(date, holidayDates)
        val fallbackTypes = DayTypeResolver.getFallbackTypeNames(typeName)

        val dia = if (isLocalOffice) {
            var localDia: LocalDia? = null
            for (type in fallbackTypes) {
                localDia = localDiaRepository.getLocalDiaByDiaIdAndOfficeAndType(
                    shiftName, effectiveConfig.officeName, type
                )
                if (localDia != null) break
            }
            localDia?.toDia()
        } else {
            var result: Dia? = null
            for (type in fallbackTypes) {
                result = diaRepository.getDiaByDiaIdAndOfficeAndType(
                    shiftName, effectiveConfig.officeName, type
                )
                if (result != null) break
            }
            result
        }

        _state.update { it.copy(shiftDia = dia) }
    }

    private fun LocalDia.toDia(): Dia = Dia(
        id = id,
        diaId = diaId,
        officeName = officeName,
        officeId = null,
        typeName = typeName,
        firstTime = firstTime,
        numTr1 = numTr1,
        numTr2 = numTr2,
        secondTime = secondTime,
        thirdTime = thirdTime,
        totalTime = totalTime,
        workTime = workTime
    )

    private suspend fun loadAvailableShifts(config: com.sonbum.diacalendar2.domain.model.UserShiftConfig) {
        val isLocalOffice = config.officeCode < 0
        val diaSelects = if (isLocalOffice) {
            val localOffice = localOfficeRepository.getLocalOfficeById(-config.officeCode)
            localOffice?.diaSelects
        } else {
            val office = officeRepository.getOfficeByCode(config.officeCode)
            office?.diaSelects
        }
        val shifts = parseShiftList(diaSelects)
        _state.update { it.copy(availableShifts = shifts) }
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

    private fun loadMemos(date: LocalDate) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            memoRepository.getMemosByDate(date).collect { memos ->
                _state.update { it.copy(memos = memos, isLoading = false) }
            }
        }
    }

    private fun loadCalendarEvents(date: LocalDate) {
        viewModelScope.launch {
            try {
                val events = deviceCalendarRepository.getEventsForDate(date)
                _state.update { it.copy(calendarEvents = events) }
            } catch (e: SecurityException) {
                // 권한이 없는 경우
                _state.update { it.copy(calendarEvents = emptyList()) }
            }
        }
    }

    private fun loadAvailableCalendars() {
        viewModelScope.launch {
            try {
                val calendars = deviceCalendarRepository.getCalendars()
                val selectedIds = deviceCalendarRepository.getSelectedCalendarIds().first()
                // 선택된 캘린더만 필터링
                val filteredCalendars = calendars.filter { selectedIds.contains(it.id) }
                _state.update {
                    it.copy(
                        availableCalendars = filteredCalendars,
                        selectedCalendarIds = selectedIds
                    )
                }
            } catch (e: SecurityException) {
                _state.update { it.copy(availableCalendars = emptyList()) }
            }
        }
    }

    /** 교체/충당/근태 등 유효교번이 바뀌는 변경 후: 위젯 갱신 + 근무 알람 재등록 */
    private fun refreshAfterShiftChange() {
        WidgetUpdater.updateAll(appContext)
        ShiftReminderWorker.enqueue(appContext)
    }

    fun toggleMemoComplete(memo: Memo) {
        viewModelScope.launch {
            memoRepository.updateMemo(memo.copy(isCompleted = !memo.isCompleted))
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun reorderMemos(fromIndex: Int, toIndex: Int) {
        val currentMemos = _state.value.memos.toMutableList()
        if (fromIndex < 0 || fromIndex >= currentMemos.size ||
            toIndex < 0 || toIndex >= currentMemos.size) {
            return
        }

        // 리스트에서 이동
        val movedItem = currentMemos.removeAt(fromIndex)
        currentMemos.add(toIndex, movedItem)

        // position 값 업데이트
        viewModelScope.launch {
            currentMemos.forEachIndexed { index, memo ->
                memoRepository.updateMemo(memo.copy(position = index.toLong()))
            }
        }

        // 즉시 UI 업데이트
        _state.update { it.copy(memos = currentMemos) }
        WidgetUpdater.updateAll(appContext)
    }

    // ===== 캘린더 이벤트 CRUD =====

    fun createCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch {
            deviceCalendarRepository.createEvent(event)
            // 이벤트 목록 새로고침
            loadCalendarEvents(_state.value.date)
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun updateCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch {
            deviceCalendarRepository.updateEvent(event)
            loadCalendarEvents(_state.value.date)
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun deleteCalendarEvent(eventId: Long) {
        viewModelScope.launch {
            deviceCalendarRepository.deleteEvent(eventId)
            loadCalendarEvents(_state.value.date)
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun refreshEvents() {
        loadCalendarEvents(_state.value.date)
    }

    // ===== 타임블록 드래그 시간 변경 =====

    fun updateMemoTime(memoId: String, newStartTime: LocalTime, newEndTime: LocalTime) {
        viewModelScope.launch {
            val memo = _state.value.memos.find { it.objectId == memoId } ?: return@launch
            memoRepository.updateMemo(memo.copy(startTime = newStartTime, endTime = newEndTime))
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun updateCalendarEventTime(eventId: Long, newStartTime: LocalDateTime, newEndTime: LocalDateTime) {
        viewModelScope.launch {
            val event = _state.value.calendarEvents.find { it.id == eventId } ?: return@launch
            deviceCalendarRepository.updateEvent(event.copy(startTime = newStartTime, endTime = newEndTime))
            loadCalendarEvents(_state.value.date)
            WidgetUpdater.updateAll(appContext)
        }
    }

    // ===== 공휴일 관리 =====

    fun addHoliday(name: String) {
        viewModelScope.launch {
            holidayRepository.addUserHoliday(_state.value.date, name)
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun updateHoliday(name: String) {
        val holidayId = _state.value.holidayId ?: return
        viewModelScope.launch {
            val holiday = holidayRepository.getHolidayByDate(_state.value.date)
            if (holiday != null) {
                holidayRepository.updateHoliday(holiday.copy(name = name))
                WidgetUpdater.updateAll(appContext)
            }
        }
    }

    fun deleteHoliday() {
        val holidayId = _state.value.holidayId ?: return
        viewModelScope.launch {
            holidayRepository.deleteHoliday(holidayId)
            WidgetUpdater.updateAll(appContext)
        }
    }

    // ===== 휴가 관리 =====

    private fun loadVacationInfo(date: LocalDate) {
        viewModelScope.launch {
            vacationRecordRepository.observeByDate(date).collect { record ->
                _state.update { it.copy(vacationRecord = record) }
            }
        }
    }

    private fun loadVacationTypes() {
        viewModelScope.launch {
            // 기본 휴가 종류 삽입 (비어있으면)
            vacationTypeRepository.ensureDefaultsExist()

            vacationTypeRepository.getAllVacationTypes().collect { types ->
                _state.update { it.copy(vacationTypes = types) }
            }
        }
    }

    fun addVacation(vacationType: VacationType, days: Int) {
        viewModelScope.launch {
            vacationRecordRepository.addVacation(
                startDate = _state.value.date,
                days = days,
                vacationTypeId = vacationType.id,
                vacationName = vacationType.name,
                shortName = vacationType.shortName
            )
            WidgetUpdater.updateAll(appContext)
        }
    }

    fun deleteVacation() {
        viewModelScope.launch {
            vacationRecordRepository.deleteByDate(_state.value.date)
            WidgetUpdater.updateAll(appContext)
        }
    }

    // ===== 교번교체 관리 =====

    private fun loadShiftSwapInfo(date: LocalDate) {
        viewModelScope.launch {
            shiftSwapRecordRepository.observeByDate(date).collect { record ->
                _state.update { it.copy(shiftSwapRecord = record) }
                updateEffectiveShift()
            }
        }
    }

    fun addShiftSwap(targetShiftName: String, days: Int) {
        viewModelScope.launch {
            val config = shiftRepository.getUserConfigOnce() ?: return@launch
            shiftSwapRecordRepository.addSwap(
                startDate = _state.value.date,
                days = days,
                targetShiftName = targetShiftName,
                shiftPattern = config.shiftPattern,
                originalScheduleProvider = { date ->
                    shiftRepository.getScheduleByDate(date)?.shiftName
                }
            )
            refreshAfterShiftChange()
        }
    }

    fun deleteShiftSwap() {
        viewModelScope.launch {
            val record = _state.value.shiftSwapRecord ?: return@launch
            shiftSwapRecordRepository.deleteByGroupId(record.groupId)
            refreshAfterShiftChange()
        }
    }

    // ===== 지근 관리 =====

    private fun loadLateWorkInfo(date: LocalDate) {
        viewModelScope.launch {
            lateWorkRecordRepository.observeByDate(date).collect { record ->
                _state.update { it.copy(lateWorkRecord = record) }
                updateEffectiveShift()
            }
        }
    }

    private fun loadLateWorkTypes() {
        viewModelScope.launch {
            lateWorkTypeRepository.getAllLateWorkTypes().collect { types ->
                _state.update { it.copy(lateWorkTypes = types) }
            }
        }
    }

    fun addLateWork(lateWorkType: LateWorkType, days: Int) {
        viewModelScope.launch {
            lateWorkRecordRepository.addLateWork(
                startDate = _state.value.date,
                days = days,
                lateWorkTypeId = lateWorkType.id,
                lateWorkName = lateWorkType.name,
                shortName = lateWorkType.shortName
            )
            refreshAfterShiftChange()
        }
    }

    fun deleteLateWork() {
        viewModelScope.launch {
            lateWorkRecordRepository.deleteByDate(_state.value.date)
            refreshAfterShiftChange()
        }
    }

    // ===== 지휴 관리 =====

    private fun loadLateHolidayInfo(date: LocalDate) {
        viewModelScope.launch {
            lateHolidayRecordRepository.observeByDate(date).collect { record ->
                _state.update { it.copy(lateHolidayRecord = record) }
                updateEffectiveShift()
            }
        }
    }

    private fun loadLateHolidayTypes() {
        viewModelScope.launch {
            lateHolidayTypeRepository.getAllLateHolidayTypes().collect { types ->
                _state.update { it.copy(lateHolidayTypes = types) }
            }
        }
    }

    fun addLateHoliday(lateHolidayType: LateHolidayType, days: Int) {
        viewModelScope.launch {
            lateHolidayRecordRepository.addLateHoliday(
                startDate = _state.value.date,
                days = days,
                lateHolidayTypeId = lateHolidayType.id,
                lateHolidayName = lateHolidayType.name,
                shortName = lateHolidayType.shortName
            )
            refreshAfterShiftChange()
        }
    }

    fun deleteLateHoliday() {
        viewModelScope.launch {
            lateHolidayRecordRepository.deleteByDate(_state.value.date)
            refreshAfterShiftChange()
        }
    }

    private fun updateEffectiveShift() {
        // 이전 계산 Job 취소: 동시 다중 호출 시 마지막 상태 기준으로만 계산
        effectiveShiftJob?.cancel()
        effectiveShiftJob = viewModelScope.launch {
            val state = _state.value
            val originalShift = state.shiftName
            val swap = state.shiftSwapRecord
            val lateWork = state.lateWorkRecord
            val lateHoliday = state.lateHolidayRecord
            val shiftInput = state.shiftInputRecord

            // 우선순위: 지휴 > 충당 > 지근 > 교번교체 > 원래 교번
            // (지근충당의 경우 충당이 지근을 덮어씀)
            val effectiveName = when {
                lateHoliday != null -> lateHoliday.lateHolidayName
                shiftInput != null -> shiftInput.targetShiftName
                lateWork != null -> lateWork.lateWorkName
                swap != null -> swap.swappedShiftName
                else -> originalShift
            }

            _state.update { it.copy(effectiveShiftName = effectiveName) }

            if (effectiveName != null) {
                loadDiaForShift(effectiveName, state.date)
            } else {
                _state.update { it.copy(shiftDia = null) }
            }
        }
    }

    // ===== 지근 토글 (Dialog 없이 바로 추가/삭제) =====
    fun toggleLateWork() {
        val currentRecord = _state.value.lateWorkRecord
        viewModelScope.launch {
            if (currentRecord != null) {
                // 이미 있으면 삭제
                lateWorkRecordRepository.deleteByDate(_state.value.date)
            } else {
                // 없으면 추가 (기본 타입 찾아서)
                val types = lateWorkTypeRepository.getAllLateWorkTypes().first()
                val defaultType = types.find { it.isDefault } ?: types.firstOrNull() ?: LateWorkType(name = "지근", shortName = "지근", isDefault = true)

                // 타입이 없으면 생성 후 추가해야 할 수도 있음
                val typeToUse = if (types.isEmpty()) {
                     val newTypeId = lateWorkTypeRepository.insert(
                         LateWorkType(name = "지근", shortName = "지근", isDefault = true)
                     )
                     LateWorkType(id = newTypeId, name = "지근", shortName = "지근", isDefault = true)
                } else {
                    defaultType
                }

                lateWorkRecordRepository.addLateWork(
                    startDate = _state.value.date,
                    days = 1, // 기본 1일
                    lateWorkTypeId = typeToUse.id,
                    lateWorkName = typeToUse.name,
                    shortName = typeToUse.shortName
                )
            }
            refreshAfterShiftChange()
        }
    }

    // ===== 지휴 토글 (Dialog 없이 바로 추가/삭제) =====
    fun toggleLateHoliday() {
        val currentRecord = _state.value.lateHolidayRecord
        viewModelScope.launch {
            if (currentRecord != null) {
                lateHolidayRecordRepository.deleteByDate(_state.value.date)
            } else {
                val types = lateHolidayTypeRepository.getAllLateHolidayTypes().first()
                val defaultType = types.find { it.isDefault } ?: types.firstOrNull() ?: LateHolidayType(name = "지휴", shortName = "지휴", isDefault = true)

                val typeToUse = if (types.isEmpty()) {
                     val newTypeId = lateHolidayTypeRepository.insert(
                         LateHolidayType(name = "지휴", shortName = "지휴", isDefault = true)
                     )
                     LateHolidayType(id = newTypeId, name = "지휴", shortName = "지휴", isDefault = true)
                } else {
                    defaultType
                }

                lateHolidayRecordRepository.addLateHoliday(
                    startDate = _state.value.date,
                    days = 1,
                    lateHolidayTypeId = typeToUse.id,
                    lateHolidayName = typeToUse.name,
                    shortName = typeToUse.shortName
                )
            }
            refreshAfterShiftChange()
        }
    }

    // ===== 충당 관리 =====

    private fun loadShiftInputInfo(date: LocalDate) {
        viewModelScope.launch {
            shiftInputRecordRepository.observeByDate(date).collect { record ->
                _state.update { it.copy(shiftInputRecord = record) }
                updateEffectiveShift()
            }
        }
    }

    private fun loadShiftInputTypes() {
        viewModelScope.launch {
            // 기본 충당 종류 삽입 (비어있으면)
            shiftInputTypeRepository.insertDefaultTypesIfEmpty()

            shiftInputTypeRepository.getAllShiftInputTypes().collect { types ->
                _state.update { it.copy(shiftInputTypes = types) }
            }
        }
    }

    fun addShiftInput(shiftInputType: ShiftInputType, targetShiftName: String, days: Int) {
        viewModelScope.launch {
            val config = shiftRepository.getUserConfigOnce() ?: return@launch
            shiftInputRecordRepository.addShiftInput(
                startDate = _state.value.date,
                days = days,
                shiftInputType = shiftInputType,
                targetShiftName = targetShiftName,
                shiftPattern = config.shiftPattern,
                originalScheduleProvider = { date ->
                    shiftRepository.getScheduleByDate(date)?.shiftName
                }
            )
            refreshAfterShiftChange()
        }
    }

    fun deleteShiftInput() {
        viewModelScope.launch {
            val record = _state.value.shiftInputRecord ?: return@launch
            shiftInputRecordRepository.deleteByGroupId(record.groupId)
            refreshAfterShiftChange()
        }
    }

    /**
     * 지근충당이 가능한지 확인 (해당 날짜에 지근이 설정되어 있어야 함)
     */
    fun canUseLateWorkShiftInput(): Boolean {
        return _state.value.lateWorkRecord != null
    }
}
