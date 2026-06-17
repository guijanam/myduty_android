package com.sonbum.diacalendar2.domain.usecase

import com.sonbum.diacalendar2.data.local.datastore.CalendarPreferences
import com.sonbum.diacalendar2.data.repository.DeviceCalendarRepositoryImpl
import com.sonbum.diacalendar2.domain.model.CalendarEvent
import com.sonbum.diacalendar2.domain.repository.DeviceCalendarRepository
import com.sonbum.diacalendar2.domain.repository.LateHolidayRecordRepository
import com.sonbum.diacalendar2.domain.repository.LateWorkRecordRepository
import com.sonbum.diacalendar2.domain.repository.ShiftInputRecordRepository
import com.sonbum.diacalendar2.domain.repository.ShiftRepository
import com.sonbum.diacalendar2.domain.repository.ShiftSwapRecordRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 날짜별 "최종 근무(effectiveShiftName)"를 기기의 전용 캘린더에 all-day 이벤트로 등록한다.
 *
 * - 원본 데이터(shift_schedules + 레이어 레코드)는 절대 변경하지 않는다. (단방향 복사)
 * - 사용자가 고른 Google 계정 캘린더에 쓴다. → CalendarProvider가 Google 서버로 동기화하므로
 *   웹(calendar.google.com)에서도 보이고, Google 캘린더 공유로 동료와 공유 가능.
 * - 동기화 전략: 우리가 등록한 근무 이벤트(마커로 식별)만 비우고 최종 근무를 다시 채운다.
 *   (사용자의 다른 일정은 건드리지 않음)
 */
class ShiftCalendarSyncUseCase(
    private val deviceCalendarRepository: DeviceCalendarRepository,
    private val calendarPreferences: CalendarPreferences,
    private val effectiveShiftUseCase: EffectiveShiftUseCase,
    private val shiftRepository: ShiftRepository,
    private val shiftSwapRecordRepository: ShiftSwapRecordRepository,
    private val shiftInputRecordRepository: ShiftInputRecordRepository,
    private val lateWorkRecordRepository: LateWorkRecordRepository,
    private val lateHolidayRecordRepository: LateHolidayRecordRepository
) {

    data class SyncResult(val eventCount: Int)

    class NoCalendarSelectedException : Exception("동기화할 캘린더가 선택되지 않았습니다.")

    /**
     * 동기화 실행. 동기화가 꺼져 있으면 아무것도 하지 않는다.
     */
    suspend fun sync(): Result<SyncResult> {
        return try {
            val enabled = calendarPreferences.shiftSyncEnabled.first()
            if (!enabled) return Result.success(SyncResult(0))

            // 1. 사용자가 선택한 대상 캘린더 확인
            val calendarId = calendarPreferences.shiftSyncCalendarId.first()
            if (calendarId <= 0) return Result.failure(NoCalendarSelectedException())

            // 2. 최종 근무 계산 (일회성 스냅샷)
            val effectiveMap = computeEffectiveMapOnce()

            // 3. 우리가 등록한 기존 근무 이벤트만 비우고 다시 채우기
            deviceCalendarRepository.deleteShiftSyncEvents(calendarId)

            // 제목 괄호 안 라벨 결정: 사용자 지정 라벨(미설정 시 승무소명 기본값)
            val customLabel = calendarPreferences.shiftSyncLabel.first()
            val label = customLabel ?: shiftRepository.getUserConfigOnce()?.officeName

            var count = 0
            effectiveMap
                .filterValues { it.isNotBlank() }
                .toSortedMap()
                .forEach { (date, shiftName) ->
                    val created = deviceCalendarRepository.createEvent(
                        buildAllDayEvent(calendarId, date, shiftName, label)
                    )
                    if (created != null) count++
                }

            Result.success(SyncResult(count))
        } catch (e: Exception) {
            android.util.Log.e("ShiftCalendarSync", "sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * 동기화를 끌 때: 우리가 등록한 근무 이벤트만 제거한다.
     * (사용자의 다른 일정은 그대로 유지)
     */
    suspend fun clearSyncedEvents(): Result<Unit> {
        return try {
            val calendarId = calendarPreferences.shiftSyncCalendarId.first()
            if (calendarId > 0) {
                deviceCalendarRepository.deleteShiftSyncEvents(calendarId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 현재 설정된 라벨을 반환. 사용자가 지정했으면 그 값, 아니면 승무소명(기본값).
     * UI 입력란 초기값 표시용.
     */
    suspend fun getCurrentLabel(): String {
        val custom = calendarPreferences.shiftSyncLabel.first()
        return custom ?: (shiftRepository.getUserConfigOnce()?.officeName ?: "")
    }

    private suspend fun computeEffectiveMapOnce(): Map<LocalDate, String> {
        val scheduleMap = shiftRepository.getScheduleMap().first()
        val swapMap = shiftSwapRecordRepository.getAllRecords().first()
            .associate { it.date to it.swappedShiftName }
        val shiftInputMap = shiftInputRecordRepository.getAllRecords().first()
            .associate { it.date to it.targetShiftName }
        val lateWorkMap = lateWorkRecordRepository.getAllRecords().first()
            .associate { it.date to it.shortName }
        val lateHolidayMap = lateHolidayRecordRepository.getAllRecords().first()
            .associate { it.date to it.shortName }
        return effectiveShiftUseCase.merge(
            scheduleMap, swapMap, shiftInputMap, lateWorkMap, lateHolidayMap
        )
    }

    private fun buildAllDayEvent(
        calendarId: Long,
        date: LocalDate,
        shiftName: String,
        label: String?
    ): CalendarEvent {
        // all-day 이벤트: 시작 = 당일 00:00, 종료 = 다음날 00:00 (1일)
        val start = LocalDateTime.of(date, LocalTime.MIDNIGHT)
        val end = LocalDateTime.of(date.plusDays(1), LocalTime.MIDNIGHT)
        // 라벨이 있으면 "근무명 (라벨)", 비었으면 "근무명"만
        val title = if (!label.isNullOrBlank()) "$shiftName ($label)" else shiftName
        return CalendarEvent(
            id = 0,
            calendarId = calendarId,
            title = title,
            description = DeviceCalendarRepositoryImpl.SHIFT_SYNC_MARKER,
            location = "",
            startTime = start,
            endTime = end,
            isAllDay = true,
            color = 0,
            rrule = null
        )
    }
}
