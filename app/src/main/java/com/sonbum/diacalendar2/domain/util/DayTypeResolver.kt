package com.sonbum.diacalendar2.domain.util

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 날짜의 유형을 판별하고, 두 날짜의 유형 조합으로 typeName을 결정하는 유틸리티.
 *
 * 유형 분류:
 * - 평 (평일): 월~금 중 공휴일이 아닌 날
 * - 휴 (휴일): 일요일 또는 공휴일
 * - 토 (토요일): 토요일 (공휴일이 아닌 경우)
 *
 * typeName 조합 (오늘 + 내일):
 * 평+평 → 평평, 평+휴 → 평휴, 평+토 → 평토
 * 휴+평 → 휴평, 휴+휴 → 휴휴, 휴+토 → 휴토
 * 토+평 → 토, 토+휴 → 토휴, 토+토 → 토
 */
object DayTypeResolver {

    private enum class DayType(val label: String) {
        WEEKDAY("평"),
        HOLIDAY("휴"),
        SATURDAY("토")
    }

    /**
     * 날짜의 유형을 판별한다.
     * @param date 판별할 날짜
     * @param holidayDates 공휴일 날짜 목록
     */
    private fun classifyDay(date: LocalDate, holidayDates: Set<LocalDate>): DayType {
        // 공휴일이면 휴일 (토요일이더라도 공휴일이면 휴일로 취급)
        if (date in holidayDates) return DayType.HOLIDAY
        // 일요일이면 휴일
        if (date.dayOfWeek == DayOfWeek.SUNDAY) return DayType.HOLIDAY
        // 토요일이면 토
        if (date.dayOfWeek == DayOfWeek.SATURDAY) return DayType.SATURDAY
        // 나머지는 평일
        return DayType.WEEKDAY
    }

    /**
     * 오늘 날짜와 내일 날짜의 유형을 조합하여 typeName을 반환한다.
     *
     * @param date 클릭한 날짜 (오늘)
     * @param holidayDates 공휴일 날짜 목록
     * @return typeName (예: "평평", "평휴", "토휴" 등)
     */
    fun resolveTypeName(date: LocalDate, holidayDates: Set<LocalDate>): String {
        val today = classifyDay(date, holidayDates)
        val tomorrow = classifyDay(date.plusDays(1), holidayDates)

        return when (today) {
            DayType.WEEKDAY -> today.label + tomorrow.label  // 평평, 평휴, 평토
            DayType.HOLIDAY -> when (tomorrow) {
                DayType.WEEKDAY -> "휴평"
                DayType.HOLIDAY -> "휴휴"
                DayType.SATURDAY -> "휴토"
            }
            DayType.SATURDAY -> when (tomorrow) {
                DayType.HOLIDAY -> "토휴"
                else -> "토"  // 토+평, 토+토 모두 "토"
            }
        }
    }

    /**
     * typeName의 fallback 후보 목록을 반환한다.
     *
     * 서버 DB에는 "평일", "휴일" 같은 단일 유형명과
     * "평평", "평휴" 같은 조합 유형명이 혼재할 수 있다.
     * 조합 유형명으로 먼저 조회하고, 없으면 오늘 기준 단일 유형명으로 fallback한다.
     *
     * fallback 규칙: 오늘이 평일이면 "평일", 오늘이 휴일이면 "휴일"
     * - "평평" → ["평평", "평일"]
     * - "평휴" → ["평휴", "평일"]
     * - "평토" → ["평토", "평일"]
     * - "휴평" → ["휴평", "휴일"]
     * - "휴휴" → ["휴휴", "휴일"]
     * - "휴토" → ["휴토", "휴일"]
     * - "토"   → ["토"]
     * - "토휴" → ["토휴", "토"]
     */
    fun getFallbackTypeNames(typeName: String): List<String> {
        return when {
            typeName.startsWith("평") && typeName != "평일" -> listOf(typeName, "평일")
            typeName.startsWith("휴") && typeName != "휴일" -> listOf(typeName, "휴일")
            typeName.startsWith("토") && typeName != "토" -> listOf(typeName, "토")
            else -> listOf(typeName)
        }
    }

    /**
     * 오늘 날짜의 단일 유형명을 반환한다 (UI 표시용).
     * @return "평일", "휴일", "토요일"
     */
    fun getDayTypeLabel(date: LocalDate, holidayDates: Set<LocalDate>): String {
        return when (classifyDay(date, holidayDates)) {
            DayType.WEEKDAY -> "평일"
            DayType.HOLIDAY -> "휴일"
            DayType.SATURDAY -> "토요일"
        }
    }
}
