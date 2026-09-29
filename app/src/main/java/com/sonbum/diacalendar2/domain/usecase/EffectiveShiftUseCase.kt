package com.sonbum.diacalendar2.domain.usecase

import com.sonbum.diacalendar2.domain.repository.LateHolidayRecordRepository
import com.sonbum.diacalendar2.domain.repository.LateWorkRecordRepository
import com.sonbum.diacalendar2.domain.repository.ShiftInputRecordRepository
import com.sonbum.diacalendar2.domain.repository.ShiftRepository
import com.sonbum.diacalendar2.domain.repository.ShiftSwapRecordRepository
import com.sonbum.diacalendar2.domain.repository.VacationRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * 날짜별 최종 근무.
 *
 * [isVacation] 이면 [name] 은 근태(휴가) 약칭이므로 일반 근무 배지가 아닌
 * VacationBadge 로 표시해야 한다.
 */
data class EffectiveShift(
    val name: String,
    val isVacation: Boolean = false
)

/**
 * 날짜별 "최종 근무(effectiveShiftName)"를 계산하는 UseCase.
 *
 * 원본 데이터(shift_schedules + 교체/충당/지근/지휴/근태 레코드)는 변경하지 않고,
 * 우선순위에 따라 병합한 결과 맵만 반환한다.
 *
 * 우선순위(낮음 → 높음, 뒤가 앞을 덮어씀):
 *   1. 원래 교번 (ShiftSchedule.shiftName)
 *   2. 교번 교체 (ShiftSwapRecord.swappedShiftName)
 *   3. 지근 (LateWorkRecord.shortName)
 *   4. 충당 (ShiftInputRecord.targetShiftName)
 *   5. 지휴 (LateHolidayRecord.shortName)
 *   6. 근태/휴가 (VacationRecord.shortName)
 *
 * 즉 표시 우선순위는 근태 > 지휴 > 충당 > 지근 > 교번교체 > 원래 교번 이며,
 * WidgetDataProvider 및 DateDetailViewModel.resolveEffectiveShiftName 과 동일하다.
 */
class EffectiveShiftUseCase(
    private val shiftRepository: ShiftRepository,
    private val shiftSwapRecordRepository: ShiftSwapRecordRepository,
    private val shiftInputRecordRepository: ShiftInputRecordRepository,
    private val lateWorkRecordRepository: LateWorkRecordRepository,
    private val lateHolidayRecordRepository: LateHolidayRecordRepository,
    private val vacationRecordRepository: VacationRecordRepository
) {

    /**
     * 날짜별 최종 근무 맵을 Flow로 반환. 어떤 레이어든 바뀌면 새 맵을 emit.
     */
    fun observe(): Flow<Map<LocalDate, EffectiveShift>> {
        val shiftLayers = combine(
            shiftRepository.getScheduleMap(),
            shiftSwapRecordRepository.getAllRecords().map { records ->
                records.associate { it.date to it.swappedShiftName }
            },
            shiftInputRecordRepository.getAllRecords().map { records ->
                records.associate { it.date to it.targetShiftName }
            },
            lateWorkRecordRepository.getAllRecords().map { records ->
                records.associate { it.date to it.shortName }
            },
            lateHolidayRecordRepository.getAllRecords().map { records ->
                records.associate { it.date to it.shortName }
            }
        ) { scheduleMap, swapMap, shiftInputMap, lateWorkMap, lateHolidayMap ->
            listOf(scheduleMap, swapMap, shiftInputMap, lateWorkMap, lateHolidayMap)
        }

        val vacationLayer = vacationRecordRepository.getAllRecords().map { records ->
            records.associate { it.date to it.shortName }
        }

        return combine(shiftLayers, vacationLayer) { layers, vacationMap ->
            merge(
                scheduleMap = layers[0],
                swapMap = layers[1],
                shiftInputMap = layers[2],
                lateWorkMap = layers[3],
                lateHolidayMap = layers[4],
                vacationMap = vacationMap
            )
        }
    }

    /**
     * 근무명 문자열만 필요한 호출자용. 근태 여부는 구분하지 않는다.
     */
    fun observeNames(): Flow<Map<LocalDate, String>> =
        observe().map { map -> map.mapValues { it.value.name } }

    /**
     * 이미 로드된 맵들을 우선순위에 따라 병합. (Flow 없이 일회성 계산에도 재사용 가능)
     */
    fun merge(
        scheduleMap: Map<LocalDate, String>,
        swapMap: Map<LocalDate, String>,
        shiftInputMap: Map<LocalDate, String>,
        lateWorkMap: Map<LocalDate, String>,
        lateHolidayMap: Map<LocalDate, String>,
        vacationMap: Map<LocalDate, String>
    ): Map<LocalDate, EffectiveShift> {
        val effectiveMap = mutableMapOf<LocalDate, EffectiveShift>()
        scheduleMap.forEach { (date, name) -> effectiveMap[date] = EffectiveShift(name) }
        // 1. 교체
        swapMap.forEach { (date, name) -> effectiveMap[date] = EffectiveShift(name) }
        // 2. 지근 (교체보다 우선)
        lateWorkMap.forEach { (date, name) -> effectiveMap[date] = EffectiveShift(name) }
        // 3. 충당 (지근보다 우선)
        shiftInputMap.forEach { (date, name) -> effectiveMap[date] = EffectiveShift(name) }
        // 4. 지휴 (충당보다 우선)
        lateHolidayMap.forEach { (date, name) -> effectiveMap[date] = EffectiveShift(name) }
        // 5. 근태/휴가 (모든 레이어보다 우선)
        vacationMap.forEach { (date, name) ->
            effectiveMap[date] = EffectiveShift(name, isVacation = true)
        }
        return effectiveMap
    }
}
