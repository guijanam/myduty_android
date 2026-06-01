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
    val isHoliday: Boolean
)

/** 알람용: 날짜별 유효교번과 3시각(출근/전반/후반) */
data class EffectiveShiftTimes(
    val date: LocalDate,
    val effectiveShiftName: String?,
    val workTime: String?,    // 출근
    val firstTime: String?,   // 전반사업
    val secondTime: String?   // 후반사업
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
    private val deviceCalendarRepository: DeviceCalendarRepository
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

            // 3. Apply priority: 지휴 > 충당 > 지근 > 교번교체 > 원래 교번
            val effectiveName = when {
                lateHoliday != null -> lateHoliday.lateHolidayName
                shiftInput != null -> shiftInput.targetShiftName
                lateWork != null -> lateWork.lateWorkName
                swap != null -> swap.swappedShiftName
                else -> originalShift
            }

            // 4. Resolve workTime from Dia
            val workTime = if (effectiveName != null && officeName != null) {
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

            WidgetDayData(
                date = date,
                effectiveShiftName = effectiveName,
                workTime = workTime,
                memoTitles = memoTitles,
                calendarEventTitles = calendarEventTitles,
                isToday = date == today,
                isHoliday = isHoliday
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

            val effectiveName = when {
                lateHoliday != null -> lateHoliday.lateHolidayName
                shiftInput != null -> shiftInput.targetShiftName
                lateWork != null -> lateWork.lateWorkName
                swap != null -> swap.swappedShiftName
                else -> originalShift
            }

            val dia = if (effectiveName != null && officeName != null) {
                resolveDia(effectiveName, date, officeName, isLocalOffice, holidayDates)
            } else null

            EffectiveShiftTimes(
                date = date,
                effectiveShiftName = effectiveName,
                workTime = dia?.workTime,
                firstTime = dia?.firstTime,
                secondTime = dia?.secondTime
            )
        }
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
