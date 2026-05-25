package com.sonbum.diacalendar2.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.sonbum.diacalendar2.data.local.datastore.CalendarPreferences
import com.sonbum.diacalendar2.domain.model.CalendarEvent
import com.sonbum.diacalendar2.domain.model.DeviceCalendar
import com.sonbum.diacalendar2.domain.repository.DeviceCalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * 기기 캘린더 접근을 위한 Repository 구현체
 * CalendarProvider API를 사용하여 캘린더 목록을 조회합니다.
 */
class DeviceCalendarRepositoryImpl(
    private val context: Context,
    private val calendarPreferences: CalendarPreferences
) : DeviceCalendarRepository {

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    // 이벤트 변경 알림을 위한 SharedFlow
    private val _eventChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val eventChanges: SharedFlow<Unit> = _eventChanges.asSharedFlow()

    companion object {
        private val CALENDAR_PROJECTION = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY
        )

        private const val PROJECTION_ID_INDEX = 0
        private const val PROJECTION_DISPLAY_NAME_INDEX = 1
        private const val PROJECTION_ACCOUNT_NAME_INDEX = 2
        private const val PROJECTION_ACCOUNT_TYPE_INDEX = 3
        private const val PROJECTION_CALENDAR_COLOR_INDEX = 4
        private const val PROJECTION_IS_PRIMARY_INDEX = 5

        // Event projection (단일 이벤트 조회용)
        private val EVENT_PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.DISPLAY_COLOR,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.DURATION
        )

        private const val EVENT_ID_INDEX = 0
        private const val EVENT_CALENDAR_ID_INDEX = 1
        private const val EVENT_TITLE_INDEX = 2
        private const val EVENT_DESCRIPTION_INDEX = 3
        private const val EVENT_LOCATION_INDEX = 4
        private const val EVENT_DTSTART_INDEX = 5
        private const val EVENT_DTEND_INDEX = 6
        private const val EVENT_ALL_DAY_INDEX = 7
        private const val EVENT_COLOR_INDEX = 8
        private const val EVENT_RRULE_INDEX = 9
        private const val EVENT_DURATION_INDEX = 10

        // Instances projection (반복 일정 포함 조회용)
        private val INSTANCE_PROJECTION = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.RRULE
        )

        private const val INSTANCE_EVENT_ID_INDEX = 0
        private const val INSTANCE_CALENDAR_ID_INDEX = 1
        private const val INSTANCE_TITLE_INDEX = 2
        private const val INSTANCE_DESCRIPTION_INDEX = 3
        private const val INSTANCE_LOCATION_INDEX = 4
        private const val INSTANCE_BEGIN_INDEX = 5
        private const val INSTANCE_END_INDEX = 6
        private const val INSTANCE_ALL_DAY_INDEX = 7
        private const val INSTANCE_COLOR_INDEX = 8
        private const val INSTANCE_RRULE_INDEX = 9
    }

    // 캘린더 ID와 displayName 캐시
    private var calendarCache: Map<Long, DeviceCalendar> = emptyMap()

    private suspend fun getCalendarCache(): Map<Long, DeviceCalendar> {
        if (calendarCache.isEmpty()) {
            calendarCache = getCalendars().associateBy { it.id }
        }
        return calendarCache
    }

    override suspend fun getCalendars(): List<DeviceCalendar> = withContext(Dispatchers.IO) {
        val calendars = mutableListOf<DeviceCalendar>()

        val uri = CalendarContract.Calendars.CONTENT_URI
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(
                uri,
                CALENDAR_PROJECTION,
                null,
                null,
                "${CalendarContract.Calendars.ACCOUNT_NAME} ASC"
            )

            cursor?.let {
                while (it.moveToNext()) {
                    val calendar = DeviceCalendar(
                        id = it.getLong(PROJECTION_ID_INDEX),
                        displayName = it.getString(PROJECTION_DISPLAY_NAME_INDEX) ?: "",
                        accountName = it.getString(PROJECTION_ACCOUNT_NAME_INDEX) ?: "",
                        accountType = it.getString(PROJECTION_ACCOUNT_TYPE_INDEX) ?: "",
                        color = it.getInt(PROJECTION_CALENDAR_COLOR_INDEX),
                        isPrimary = it.getInt(PROJECTION_IS_PRIMARY_INDEX) == 1
                    )
                    calendars.add(calendar)
                }
            }
        } finally {
            cursor?.close()
        }

        calendars
    }

    override fun getSelectedCalendarIds(): Flow<Set<Long>> {
        return calendarPreferences.selectedCalendarIds
    }

    override suspend fun saveSelectedCalendarIds(ids: Set<Long>) {
        calendarPreferences.saveSelectedCalendarIds(ids)
    }

    override suspend fun toggleCalendarSelection(calendarId: Long) {
        val currentSelection = calendarPreferences.selectedCalendarIds.first()
        calendarPreferences.toggleCalendarSelection(calendarId, currentSelection)
    }

    // ===== 이벤트 CRUD 구현 =====

    /**
     * Instances 테이블을 사용하여 이벤트 조회 (반복 일정 포함)
     * Instances 테이블은 반복 일정의 각 인스턴스를 포함합니다.
     */
    override suspend fun getEvents(startDate: LocalDate, endDate: LocalDate): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            val selectedIds = calendarPreferences.selectedCalendarIds.first()
            if (selectedIds.isEmpty()) return@withContext emptyList()

            val events = mutableListOf<CalendarEvent>()
            val calendarMap = getCalendarCache()

            val startMillis = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

            // Instances.query를 사용하여 반복 일정 포함 조회
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)
            val uri = builder.build()

            // 선택된 캘린더 ID로 필터링
            val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${selectedIds.joinToString(",")})"

            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(
                    uri,
                    INSTANCE_PROJECTION,
                    selection,
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC"
                )

                cursor?.let {
                    while (it.moveToNext()) {
                        val calendarId = it.getLong(INSTANCE_CALENDAR_ID_INDEX)
                        val calendar = calendarMap[calendarId]
                        val isAllDay = it.getInt(INSTANCE_ALL_DAY_INDEX) == 1
                        val beginMillis = it.getLong(INSTANCE_BEGIN_INDEX)
                        val endMillisValue = it.getLong(INSTANCE_END_INDEX)

                        val rrule = it.getString(INSTANCE_RRULE_INDEX)

                        val event = CalendarEvent(
                            id = it.getLong(INSTANCE_EVENT_ID_INDEX),
                            calendarId = calendarId,
                            title = it.getString(INSTANCE_TITLE_INDEX) ?: "",
                            description = it.getString(INSTANCE_DESCRIPTION_INDEX) ?: "",
                            location = it.getString(INSTANCE_LOCATION_INDEX) ?: "",
                            startTime = if (isAllDay) millisToLocalDateTimeUtc(beginMillis) else millisToLocalDateTimeLocal(beginMillis),
                            endTime = if (isAllDay) millisToLocalDateTimeUtc(endMillisValue) else millisToLocalDateTimeLocal(endMillisValue),
                            isAllDay = isAllDay,
                            color = it.getInt(INSTANCE_COLOR_INDEX),
                            calendarDisplayName = calendar?.displayName ?: "",
                            rrule = rrule
                        )
                        events.add(event)
                    }
                }
            } finally {
                cursor?.close()
            }

            events
        }

    override suspend fun getEventsForDate(date: LocalDate): List<CalendarEvent> {
        // getEvents로 가져온 후, 실제 해당 날짜에 속하는 이벤트만 필터링
        return getEvents(date, date).filter { event ->
            // 이벤트의 실제 시작일과 종료일 범위 내에 date가 포함되는지 확인
            !date.isBefore(event.startDate) && !date.isAfter(event.endDate)
        }
    }

    override suspend fun createEvent(event: CalendarEvent): Long? = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, event.calendarId)
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
                if (event.isAllDay) {
                    // 종일 이벤트는 UTC 기준으로 저장
                    put(CalendarContract.Events.DTSTART, localDateTimeToMillisUtc(event.startTime))
                    put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                    // 반복 일정이 아닌 경우에만 DTEND 설정
                    if (event.rrule == null) {
                        put(CalendarContract.Events.DTEND, localDateTimeToMillisUtc(event.endTime))
                    }
                } else {
                    put(CalendarContract.Events.DTSTART, localDateTimeToMillis(event.startTime))
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    // 반복 일정이 아닌 경우에만 DTEND 설정
                    if (event.rrule == null) {
                        put(CalendarContract.Events.DTEND, localDateTimeToMillis(event.endTime))
                    }
                }
                put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
                // 반복 규칙 설정
                if (event.rrule != null) {
                    put(CalendarContract.Events.RRULE, event.rrule)
                    // 반복 일정은 DURATION으로 길이를 지정 (DTEND 대신)
                    val durationMillis = if (event.isAllDay) {
                        localDateTimeToMillisUtc(event.endTime) - localDateTimeToMillisUtc(event.startTime)
                    } else {
                        localDateTimeToMillis(event.endTime) - localDateTimeToMillis(event.startTime)
                    }
                    // ISO 8601 duration 형식 (P1D = 1일, PT1H = 1시간)
                    val duration = if (event.isAllDay) {
                        val days = durationMillis / (24 * 60 * 60 * 1000)
                        "P${days}D"
                    } else {
                        val seconds = durationMillis / 1000
                        "PT${seconds}S"
                    }
                    put(CalendarContract.Events.DURATION, duration)
                }
            }

            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val result = uri?.lastPathSegment?.toLongOrNull()
            if (result != null) {
                _eventChanges.tryEmit(Unit)
            }
            result
        } catch (e: Exception) {
            android.util.Log.e("DeviceCalendarRepo", "createEvent failed", e)
            null
        }
    }

    override suspend fun updateEvent(event: CalendarEvent): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
                if (event.isAllDay) {
                    // 종일 이벤트는 UTC 기준으로 저장
                    put(CalendarContract.Events.DTSTART, localDateTimeToMillisUtc(event.startTime))
                    put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                } else {
                    put(CalendarContract.Events.DTSTART, localDateTimeToMillis(event.startTime))
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }
                put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
                // 반복 규칙 업데이트
                if (event.rrule != null) {
                    put(CalendarContract.Events.RRULE, event.rrule)
                    // DTEND를 null로 설정하고 DURATION 사용
                    putNull(CalendarContract.Events.DTEND)
                    val durationMillis = if (event.isAllDay) {
                        localDateTimeToMillisUtc(event.endTime) - localDateTimeToMillisUtc(event.startTime)
                    } else {
                        localDateTimeToMillis(event.endTime) - localDateTimeToMillis(event.startTime)
                    }
                    val duration = if (event.isAllDay) {
                        val days = durationMillis / (24 * 60 * 60 * 1000)
                        "P${days}D"
                    } else {
                        val seconds = durationMillis / 1000
                        "PT${seconds}S"
                    }
                    put(CalendarContract.Events.DURATION, duration)
                } else {
                    // 반복 없음 - RRULE 제거하고 DTEND 사용
                    putNull(CalendarContract.Events.RRULE)
                    putNull(CalendarContract.Events.DURATION)
                    if (event.isAllDay) {
                        put(CalendarContract.Events.DTEND, localDateTimeToMillisUtc(event.endTime))
                    } else {
                        put(CalendarContract.Events.DTEND, localDateTimeToMillis(event.endTime))
                    }
                }
            }

            val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
            val rowsUpdated = contentResolver.update(updateUri, values, null, null)
            val success = rowsUpdated > 0
            if (success) {
                _eventChanges.tryEmit(Unit)
            }
            success
        } catch (e: Exception) {
            android.util.Log.e("DeviceCalendarRepo", "updateEvent failed", e)
            false
        }
    }

    override suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rowsDeleted = contentResolver.delete(deleteUri, null, null)
            val success = rowsDeleted > 0
            if (success) {
                _eventChanges.tryEmit(Unit)
            }
            success
        } catch (e: Exception) {
            android.util.Log.e("DeviceCalendarRepo", "deleteEvent failed", e)
            false
        }
    }

    override suspend fun getEventById(eventId: Long): CalendarEvent? = withContext(Dispatchers.IO) {
        val calendarMap = getCalendarCache()

        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(uri, EVENT_PROJECTION, null, null, null)
            cursor?.let {
                if (it.moveToFirst()) {
                    val calendarId = it.getLong(EVENT_CALENDAR_ID_INDEX)
                    val calendar = calendarMap[calendarId]
                    val isAllDay = it.getInt(EVENT_ALL_DAY_INDEX) == 1
                    val startMillisValue = it.getLong(EVENT_DTSTART_INDEX)
                    val rrule = it.getString(EVENT_RRULE_INDEX)
                    val duration = it.getString(EVENT_DURATION_INDEX)

                    // 반복 일정은 DTEND가 null이므로 DURATION으로 endTime 계산
                    val endMillisValue = if (rrule != null && duration != null) {
                        startMillisValue + parseDurationToMillis(duration)
                    } else {
                        it.getLong(EVENT_DTEND_INDEX)
                    }

                    return@withContext CalendarEvent(
                        id = it.getLong(EVENT_ID_INDEX),
                        calendarId = calendarId,
                        title = it.getString(EVENT_TITLE_INDEX) ?: "",
                        description = it.getString(EVENT_DESCRIPTION_INDEX) ?: "",
                        location = it.getString(EVENT_LOCATION_INDEX) ?: "",
                        startTime = if (isAllDay) millisToLocalDateTimeUtc(startMillisValue) else millisToLocalDateTimeLocal(startMillisValue),
                        endTime = if (isAllDay) millisToLocalDateTimeUtc(endMillisValue) else millisToLocalDateTimeLocal(endMillisValue),
                        isAllDay = isAllDay,
                        color = it.getInt(EVENT_COLOR_INDEX),
                        calendarDisplayName = calendar?.displayName ?: "",
                        rrule = rrule
                    )
                }
            }
        } finally {
            cursor?.close()
        }
        null
    }

    // ===== 유틸리티 함수 =====

    /**
     * 비종일 이벤트용 밀리초 -> LocalDateTime 변환
     * Instances 테이블의 BEGIN/END은 UTC 밀리초이므로 시스템 시간대로 변환
     */
    private fun millisToLocalDateTimeLocal(millis: Long): LocalDateTime {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        )
    }

    /**
     * 종일 이벤트용 UTC -> LocalDateTime 변환
     * 종일 이벤트는 UTC 기준으로 저장되므로 시간대 변환 없이 UTC로 읽어야 함
     */
    private fun millisToLocalDateTimeUtc(millis: Long): LocalDateTime {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(millis),
            ZoneId.of("UTC")
        )
    }

    /**
     * 비종일 이벤트용 LocalDateTime -> 밀리초 변환
     * CalendarProvider의 DTSTART/DTEND는 UTC 밀리초이므로 시스템 시간대 기준으로 변환
     */
    private fun localDateTimeToMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * 종일 이벤트용 LocalDateTime -> UTC 밀리초 변환
     * 종일 이벤트는 UTC 기준으로 저장해야 함
     */
    private fun localDateTimeToMillisUtc(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }

    /**
     * ISO 8601 duration 문자열을 밀리초로 변환
     * 예: "P1D" -> 86400000, "PT3600S" -> 3600000
     */
    private fun parseDurationToMillis(duration: String): Long {
        return try {
            val javaDuration = java.time.Duration.parse(duration)
            javaDuration.toMillis()
        } catch (e: Exception) {
            // P1D 같은 날짜 기반 duration은 java.time.Duration이 파싱 못함
            // 수동 파싱
            val dayMatch = Regex("P(\\d+)D").find(duration)
            if (dayMatch != null) {
                dayMatch.groupValues[1].toLong() * 24 * 60 * 60 * 1000
            } else {
                val secondMatch = Regex("PT(\\d+)S").find(duration)
                if (secondMatch != null) {
                    secondMatch.groupValues[1].toLong() * 1000
                } else {
                    // 기본값: 1시간
                    3600000L
                }
            }
        }
    }
}
