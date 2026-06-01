package com.sonbum.diacalendar2.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Context에 대한 DataStore 확장 프로퍼티
val Context.textSizeDataStore: DataStore<Preferences> by preferencesDataStore(name = "text_size_preferences")

/**
 * 캘린더 텍스트 크기 설정
 */
data class CalendarTextSizes(
	val crewPatternFontSize: Float = 9f, // 근무조 패턴 텍스트 크기 (기본 9sp)
    val dateFontSize: Float = 12f,      // 날짜 텍스트 크기 (기본 12sp)
    val shiftFontSize: Float = 14f,     // 근무 텍스트 크기 (기본 14sp)
    val eventFontSize: Float = 8f,      // 연동 캘린더 일정 텍스트 크기 (기본 8sp)
    val memoFontSize: Float = 8f,       // 메모 텍스트 크기 (기본 8sp)

) {
    companion object {
        val DEFAULT = CalendarTextSizes()

        // 텍스트 크기 범위
        const val MIN_DATE_SIZE = 8f
        const val MAX_DATE_SIZE = 18f
        const val MIN_SHIFT_SIZE = 10f
        const val MAX_SHIFT_SIZE = 20f
        const val MIN_EVENT_SIZE = 6f
        const val MAX_EVENT_SIZE = 14f
        const val MIN_MEMO_SIZE = 6f
        const val MAX_MEMO_SIZE = 14f
        const val MIN_CREW_PATTERN_SIZE = 6f
        const val MAX_CREW_PATTERN_SIZE = 14f
    }
}

/**
 * 텍스트 크기 설정을 저장하는 DataStore 관리 클래스
 */
class TextSizePreferences(private val context: Context) {

    companion object {
        private val DATE_FONT_SIZE = floatPreferencesKey("date_font_size")
        private val SHIFT_FONT_SIZE = floatPreferencesKey("shift_font_size")
        private val EVENT_FONT_SIZE = floatPreferencesKey("event_font_size")
        private val MEMO_FONT_SIZE = floatPreferencesKey("memo_font_size")
        private val CREW_PATTERN_FONT_SIZE = floatPreferencesKey("crew_pattern_font_size")
        // 근무표 화면 글자 크기 단계 인덱스 (0=보통, 1=크게, 2=더크게)
        private val DIA_TABLE_FONT_SCALE_INDEX = intPreferencesKey("dia_table_font_scale_index")
    }

    /** 근무표 화면 글자 크기 단계 인덱스 (앱 재실행 후에도 유지) */
    val diaTableFontScaleIndex: Flow<Int> = context.textSizeDataStore.data
        .map { it[DIA_TABLE_FONT_SCALE_INDEX] ?: 0 }

    suspend fun saveDiaTableFontScaleIndex(index: Int) {
        context.textSizeDataStore.edit { it[DIA_TABLE_FONT_SCALE_INDEX] = index }
    }

    /**
     * 텍스트 크기 설정을 Flow로 반환
     */
    val textSizes: Flow<CalendarTextSizes> = context.textSizeDataStore.data
        .map { preferences ->
            CalendarTextSizes(
                dateFontSize = preferences[DATE_FONT_SIZE] ?: CalendarTextSizes.DEFAULT.dateFontSize,
                shiftFontSize = preferences[SHIFT_FONT_SIZE] ?: CalendarTextSizes.DEFAULT.shiftFontSize,
                eventFontSize = preferences[EVENT_FONT_SIZE] ?: CalendarTextSizes.DEFAULT.eventFontSize,
                memoFontSize = preferences[MEMO_FONT_SIZE] ?: CalendarTextSizes.DEFAULT.memoFontSize,
                crewPatternFontSize = preferences[CREW_PATTERN_FONT_SIZE] ?: CalendarTextSizes.DEFAULT.crewPatternFontSize
            )
        }

    /**
     * 날짜 텍스트 크기 저장
     */
    suspend fun saveDateFontSize(size: Float) {
        context.textSizeDataStore.edit { preferences ->
            preferences[DATE_FONT_SIZE] = size.coerceIn(
                CalendarTextSizes.MIN_DATE_SIZE,
                CalendarTextSizes.MAX_DATE_SIZE
            )
        }
    }

    /**
     * 근무 텍스트 크기 저장
     */
    suspend fun saveShiftFontSize(size: Float) {
        context.textSizeDataStore.edit { preferences ->
            preferences[SHIFT_FONT_SIZE] = size.coerceIn(
                CalendarTextSizes.MIN_SHIFT_SIZE,
                CalendarTextSizes.MAX_SHIFT_SIZE
            )
        }
    }

    /**
     * 연동 캘린더 일정 텍스트 크기 저장
     */
    suspend fun saveEventFontSize(size: Float) {
        context.textSizeDataStore.edit { preferences ->
            preferences[EVENT_FONT_SIZE] = size.coerceIn(
                CalendarTextSizes.MIN_EVENT_SIZE,
                CalendarTextSizes.MAX_EVENT_SIZE
            )
        }
    }

    /**
     * 메모 텍스트 크기 저장
     */
    suspend fun saveMemoFontSize(size: Float) {
        context.textSizeDataStore.edit { preferences ->
            preferences[MEMO_FONT_SIZE] = size.coerceIn(
                CalendarTextSizes.MIN_MEMO_SIZE,
                CalendarTextSizes.MAX_MEMO_SIZE
            )
        }
    }

    /**
     * 근무조 패턴 텍스트 크기 저장
     */
    suspend fun saveCrewPatternFontSize(size: Float) {
        context.textSizeDataStore.edit { preferences ->
            preferences[CREW_PATTERN_FONT_SIZE] = size.coerceIn(
                CalendarTextSizes.MIN_CREW_PATTERN_SIZE,
                CalendarTextSizes.MAX_CREW_PATTERN_SIZE
            )
        }
    }

    /**
     * 모든 텍스트 크기 저장
     */
    suspend fun saveAllTextSizes(textSizes: CalendarTextSizes) {
        context.textSizeDataStore.edit { preferences ->
	        preferences[CREW_PATTERN_FONT_SIZE] = textSizes.crewPatternFontSize.coerceIn(
		        CalendarTextSizes.MIN_CREW_PATTERN_SIZE,
		        CalendarTextSizes.MAX_CREW_PATTERN_SIZE
	        )
            preferences[DATE_FONT_SIZE] = textSizes.dateFontSize.coerceIn(
                CalendarTextSizes.MIN_DATE_SIZE,
                CalendarTextSizes.MAX_DATE_SIZE
            )
            preferences[SHIFT_FONT_SIZE] = textSizes.shiftFontSize.coerceIn(
                CalendarTextSizes.MIN_SHIFT_SIZE,
                CalendarTextSizes.MAX_SHIFT_SIZE
            )
            preferences[EVENT_FONT_SIZE] = textSizes.eventFontSize.coerceIn(
                CalendarTextSizes.MIN_EVENT_SIZE,
                CalendarTextSizes.MAX_EVENT_SIZE
            )
            preferences[MEMO_FONT_SIZE] = textSizes.memoFontSize.coerceIn(
                CalendarTextSizes.MIN_MEMO_SIZE,
                CalendarTextSizes.MAX_MEMO_SIZE
            )

        }
    }

    /**
     * 기본값으로 초기화
     */
    suspend fun resetToDefault() {
        saveAllTextSizes(CalendarTextSizes.DEFAULT)
    }
}
