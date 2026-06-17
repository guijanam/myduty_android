package com.sonbum.diacalendar2.domain.usecase

import com.sonbum.diacalendar2.domain.repository.LateHolidayRecordRepository
import com.sonbum.diacalendar2.domain.repository.LateWorkRecordRepository
import com.sonbum.diacalendar2.domain.repository.ShiftInputRecordRepository
import com.sonbum.diacalendar2.domain.repository.ShiftRepository
import com.sonbum.diacalendar2.domain.repository.ShiftSwapRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * 날짜별 "최종 근무(effectiveShiftName)"를 계산하는 UseCase.
 *
 * 원본 데이터(shift_schedules + 교체/충당/지근/지휴 레코드)는 변경하지 않고,
 * 우선순위에 따라 병합한 결과 맵만 반환한다.
 *
 * 우선순위(낮음 → 높음, 뒤가 앞을 덮어씀):
 *   1. 원래 교번 (ShiftSchedule.shiftName)
 *   2. 교번 교체 (ShiftSwapRecord.swappedShiftName)
 *   3. 지근 (LateWorkRecord.shortName)
 *   4. 충당 (ShiftInputRecord.targetShiftName)
 *   5. 지휴 (LateHolidayRecord.shortName)
 *
 * HomeViewModel.observeShiftSchedules 와 동일한 병합 규칙을 단일 출처로 모은 것.
 */
class EffectiveShiftUseCase(
    private val shiftRepository: ShiftRepository,
    private val shiftSwapRecordRepository: ShiftSwapRecordRepository,
    private val shiftInputRecordRepository: ShiftInputRecordRepository,
    private val lateWorkRecordRepository: LateWorkRecordRepository,
    private val lateHolidayRecordRepository: LateHolidayRecordRepository
) {

    /**
     * 날짜별 최종 근무 맵을 Flow로 반환. 어떤 레이어든 바뀌면 새 맵을 emit.
     */
    fun observe(): Flow<Map<LocalDate, String>> {
        return combine(
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
            merge(scheduleMap, swapMap, shiftInputMap, lateWorkMap, lateHolidayMap)
        }
    }

    /**
     * 이미 로드된 맵들을 우선순위에 따라 병합. (Flow 없이 일회성 계산에도 재사용 가능)
     */
    fun merge(
        scheduleMap: Map<LocalDate, String>,
        swapMap: Map<LocalDate, String>,
        shiftInputMap: Map<LocalDate, String>,
        lateWorkMap: Map<LocalDate, String>,
        lateHolidayMap: Map<LocalDate, String>
    ): Map<LocalDate, String> {
        val effectiveMap = scheduleMap.toMutableMap()
        // 1. 교체
        swapMap.forEach { (date, name) -> effectiveMap[date] = name }
        // 2. 지근 (교체보다 우선)
        lateWorkMap.forEach { (date, name) -> effectiveMap[date] = name }
        // 3. 충당 (지근보다 우선)
        shiftInputMap.forEach { (date, name) -> effectiveMap[date] = name }
        // 4. 지휴 (충당보다 우선)
        lateHolidayMap.forEach { (date, name) -> effectiveMap[date] = name }
        return effectiveMap
    }
}
