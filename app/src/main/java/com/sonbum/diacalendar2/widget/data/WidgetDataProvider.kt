package com.sonbum.diacalendar2.widget.data

import com.sonbum.diacalendar2.data.local.dao.DiaDao
import com.sonbum.diacalendar2.data.local.dao.HolidayDao
import com.sonbum.diacalendar2.data.local.dao.LateHolidayRecordDao
import com.sonbum.diacalendar2.data.local.dao.LateWorkRecordDao
import com.sonbum.diacalendar2.data.local.dao.LocalDiaDao
import com.sonbum.diacalendar2.data.local.dao.MemoDao
import com.sonbum.diacalendar2.data.local.dao.ShiftInputRecordDao
import com.sonbum.diacalendar2.data.local.dao.ShiftScheduleDao
import com.sonbum.diacalendar2.data.local.dao.ShiftSwapRecordDao
import com.sonbum.diacalendar2.data.local.dao.UserShiftConfigDao
import com.sonbum.diacalendar2.data.local.dao.VacationRecordDao
import com.sonbum.diacalendar2.domain.model.Dia
import com.sonbum.diacalendar2.domain.repository.DeviceCalendarRepository
import com.sonbum.diacalendar2.domain.util.DayTypeResolver
import java.time.DayOfWeek
import java.time.LocalDate

data class WidgetDayData(
    val date: LocalDate,
    val effectiveShiftName: String?,
    val workTime: String?,
    val memoTitles: List<String>,
    val calendarEventTitles: List<String>,
    val isToday: Boolean,
    val isHoliday: Boolean,
    /** 교번교체로 바뀐 날(글자색 주황색 표시용) */
    val isSwap: Boolean = false,
    /** 충당으로 바뀐 날의 색상 HEX(예: "#4CAF50"). 충당이 아니면 null */
    val shiftInputColorHex: String? = null,
    /** 근태(휴가)인 날(글자색 빨간색 표시용) */
    val isVacation: Boolean = false
)

/** 알람용: 날짜별 유효교번과 3시각(출근/전반/후반) */
data class EffectiveShiftTimes(
    val date: LocalDate,
    val effectiveShiftName: String?,
    val workTime: String?,    // 출근
    val firstTime: String?,   // 전반사업
    val secondTime: String?,  // 후반사업
    val numTr1: String? = null,   // 전반열번
    val numTr2: String? = null,   // 후반열번
    val typeName: String? = null  // 요일타입(평일/토요일/일요일 등)
)

class WidgetDataProvider(
    private val shiftScheduleDao: ShiftScheduleDao,
    private val shiftSwapRecordDao: ShiftSwapRecordDao,
    private val shiftInputRecordDao: ShiftInputRecordDao,
    private val lateWorkRecordDao: LateWorkRecordDao,
    private val lateHolidayRecordDao: LateHolidayRecordDao,
    private val userShiftConfigDao: UserShiftConfigDao,
    private val diaDao: DiaDao,
    private val localDiaDao: LocalDiaDao,
    private val memoDao: MemoDao,
    private val holidayDao: HolidayDao,
    private val deviceCalendarRepository: DeviceCalendarRepository,
    private val vacationRecordDao: VacationRecordDao
) {
    suspend fun loadDayDataList(dates: List<LocalDate>): List<WidgetDayData> {
        val config = userShiftConfigDao.getConfigOnce()
        val today = LocalDate.now()

        val holidayDateStrings = holidayDao.getAllHolidayDatesOnce()
        val holidayDates = holidayDateStrings.mapNotNull { str ->
            try { LocalDate.parse(str) } catch (_: Exception) { null }
        }.toSet()

        val isLocalOffice = config != null && config.officeCode < 0
        val officeName = config?.officeName

        return dates.map { date ->
            val dateStr = date.toString()

            // 1. Get original shift
            val originalShift = shiftScheduleDao.getScheduleByDate(dateStr)?.shiftName

            // 2. Get override records
            val swap = shiftSwapRecordDao.getByDate(dateStr)
            val shiftInput = shiftInputRecordDao.getByDate(dateStr)
            val lateWork = lateWorkRecordDao.getByDateOnce(dateStr)
            val lateHoliday = lateHolidayRecordDao.getByDateOnce(dateStr)
            val vacation = vacationRecordDao.getByDate(dateStr)

            // 3. Apply priority: 근태(휴가) > 지휴 > 충당 > 지근 > 교번교체 > 원래 교번
            val effectiveName = when {
                vacation != null -> vacation.shortName
                lateHoliday != null -> lateHoliday.lateHolidayName
                shiftInput != null -> shiftInput.targetShiftName
                lateWork != null -> lateWork.lateWorkName
                swap != null -> swap.swappedShiftName
                else -> originalShift
            }

            // 4. Resolve workTime from Dia (휴가일은 출근 시각 없음)
            val workTime = if (vacation == null && effectiveName != null && officeName != null) {
                resolveWorkTime(effectiveName, date, officeName, isLocalOffice, holidayDates)
            } else null

            // 5. Load memos
            val memoTitles = memoDao.getMemosByDateOnce(dateStr).map { it.title }

            // 6. Load calendar events (graceful on permission denied)
            val calendarEventTitles = try {
                deviceCalendarRepository.getEventsForDate(date).map { it.title }
            } catch (_: SecurityException) {
                emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            // 7. Check holiday
            val isHoliday = date in holidayDates || date.dayOfWeek == DayOfWeek.SUNDAY

            // 8. 글자색 힌트: 충당/교체로 바뀐 날 (휴가/지휴/지근이 더 우선이면 색 힌트 없음)
            val shiftInputColorHex = if (vacation == null && lateHoliday == null && shiftInput != null) {
                shiftInput.colorHex
            } else null
            val isSwap = vacation == null && lateHoliday == null &&
                shiftInput == null && lateWork == null && swap != null

            WidgetDayData(
                date = date,
                effectiveShiftName = effectiveName,
                workTime = workTime,
                memoTitles = memoTitles,
                calendarEventTitles = calendarEventTitles,
                isToday = date == today,
                isHoliday = isHoliday,
                isSwap = isSwap,
                shiftInputColorHex = shiftInputColorHex,
                isVacation = vacation != null
            )
        }
    }

    /**
     * 알람용: 주어진 날짜들의 유효교번 + 3시각을 계산한다.
     * 유효교번 우선순위(지휴>충당>지근>교체>원래)와 workTime 해석을 위젯과 동일하게 재사용한다.
     */
    suspend fun loadEffectiveShiftTimes(dates: List<LocalDate>): List<EffectiveShiftTimes> {
        val config = userShiftConfigDao.getConfigOnce()
        val holidayDates = holidayDao.getAllHolidayDatesOnce().mapNotNull { str ->
            try { LocalDate.parse(str) } catch (_: Exception) { null }
        }.toSet()
        val isLocalOffice = config != null && config.officeCode < 0
        val officeName = config?.officeName

        return dates.map { date ->
            val dateStr = date.toString()
            val originalShift = shiftScheduleDao.getScheduleByDate(dateStr)?.shiftName
            val swap = shiftSwapRecordDao.getByDate(dateStr)
            val shiftInput = shiftInputRecordDao.getByDate(dateStr)
            val lateWork = lateWorkRecordDao.getByDateOnce(dateStr)
            val lateHoliday = lateHolidayRecordDao.getByDateOnce(dateStr)
            val vacation = vacationRecordDao.getByDate(dateStr)

            val effectiveName = when {
                vacation != null -> vacation.shortName
                lateHoliday != null -> lateHoliday.lateHolidayName
                shiftInput != null -> shiftInput.targetShiftName
                lateWork != null -> lateWork.lateWorkName
                swap != null -> swap.swappedShiftName
                else -> originalShift
            }

            // "~"로 끝나는 날(예: "59~")은 전날 야간 근무가 이어지는 날이다.
            // 그 날 자체 근무는 없으므로 자체 Dia를 조회하지 않고,
            // 전날 cross-day 근무의 후반(secondTime/numTr2)만 표시한다.
            val isCarryOverDay = effectiveName?.endsWith("~") == true

            // 휴가일은 출근 시각이 없어 알람이 해제되도록 Dia 조회를 건너뛴다.
            val dia = if (vacation == null && !isCarryOverDay && effectiveName != null && officeName != null) {
                resolveDia(effectiveName, date, officeName, isLocalOffice, holidayDates)
            } else null

            // 전날 후반 carryover (이 날이 "~"일 때만)
            var carryOverSecondTime: String? = null
            var carryOverNumTr2: String? = null
            if (isCarryOverDay && vacation == null && officeName != null) {
                val prevDate = date.minusDays(1)
                // 전날 근무도 교체/충당/지근/지휴/근태가 반영된 "유효 근무"로 조회해야 한다.
                // (원래 교번만 보면 이미 교체된 옛 근무의 후반이 계속 표시된다)
                val prevShift = resolveEffectiveShiftName(prevDate)
                if (!prevShift.isNullOrBlank()) {
                    val prevDia = resolveDia(prevShift.removeSuffix("~"), prevDate, officeName, isLocalOffice, holidayDates)
                    if (prevDia != null && DayTypeResolver.isCrossDayType(prevDia.typeName)) {
                        carryOverSecondTime = prevDia.secondTime
                        carryOverNumTr2 = prevDia.numTr2
                    }
                }
            }

            EffectiveShiftTimes(
                date = date,
                effectiveShiftName = effectiveName,
                workTime = dia?.workTime,
                firstTime = dia?.firstTime,
                secondTime = dia?.secondTime ?: carryOverSecondTime,
                numTr1 = dia?.numTr1,
                numTr2 = dia?.numTr2 ?: carryOverNumTr2,
                typeName = if (dia != null) DayTypeResolver.resolveTypeName(date, holidayDates) else null
            )
        }
    }

    /**
     * 특정 날짜의 유효 근무명을 조회한다.
     * 우선순위: 근태(휴가) > 지휴 > 충당 > 지근 > 교번교체 > 원래 교번
     */
    private suspend fun resolveEffectiveShiftName(date: LocalDate): String? {
        val dateStr = date.toString()
        vacationRecordDao.getByDate(dateStr)?.let { return it.shortName }
        lateHolidayRecordDao.getByDateOnce(dateStr)?.let { return it.lateHolidayName }
        shiftInputRecordDao.getByDate(dateStr)?.let { return it.targetShiftName }
        lateWorkRecordDao.getByDateOnce(dateStr)?.let { return it.lateWorkName }
        shiftSwapRecordDao.getByDate(dateStr)?.let { return it.swappedShiftName }
        return shiftScheduleDao.getScheduleByDate(dateStr)?.shiftName
    }

    private suspend fun resolveWorkTime(
        shiftName: String,
        date: LocalDate,
        officeName: String,
        isLocalOffice: Boolean,
        holidayDates: Set<LocalDate>
    ): String? = resolveDia(shiftName, date, officeName, isLocalOffice, holidayDates)?.workTime

    private suspend fun resolveDia(
        shiftName: String,
        date: LocalDate,
        officeName: String,
        isLocalOffice: Boolean,
        holidayDates: Set<LocalDate>
    ): Dia? {
        val typeName = DayTypeResolver.resolveTypeName(date, holidayDates)
        val fallbackTypes = DayTypeResolver.getFallbackTypeNames(typeName)

        val dia: Dia? = if (isLocalOffice) {
            var result: com.sonbum.diacalendar2.data.local.entity.LocalDiaEntity? = null
            for (type in fallbackTypes) {
                result = localDiaDao.getDiaByDiaIdAndOfficeAndType(shiftName, officeName, type)
                if (result != null) break
            }
            result?.let {
                Dia(
                    id = it.id,
                    diaId = it.diaId,
                    officeName = it.officeName,
                    officeId = null,
                    typeName = it.typeName,
                    firstTime = it.firstTime,
                    numTr1 = it.numTr1,
                    numTr2 = it.numTr2,
                    secondTime = it.secondTime,
                    thirdTime = it.thirdTime,
                    totalTime = it.totalTime,
                    workTime = it.workTime
                )
            }
        } else {
            var result: com.sonbum.diacalendar2.data.local.entity.DiaEntity? = null
            for (type in fallbackTypes) {
                result = diaDao.getDiaByDiaIdAndOfficeAndType(shiftName, officeName, type)
                if (result != null) break
            }
            result?.let {
                Dia(
                    id = it.id,
                    diaId = it.diaId,
                    officeName = it.officeName,
                    officeId = it.officeId,
                    typeName = it.typeName,
                    firstTime = it.firstTime,
                    numTr1 = it.numTr1,
                    numTr2 = it.numTr2,
                    secondTime = it.secondTime,
                    thirdTime = it.thirdTime,
                    totalTime = it.totalTime,
                    workTime = it.workTime
                )
            }
        }

        return dia
    }
}
