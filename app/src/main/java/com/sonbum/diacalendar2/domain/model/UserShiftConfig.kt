package com.sonbum.diacalendar2.domain.model

import java.time.LocalDate

/**
 * 사용자 교번 설정 Domain 모델
 */
data class UserShiftConfig(
    val officeCode: Long,
    val officeName: String,
    val position: Position,
    val shiftPattern: List<String>,  // 교번 패턴 배열
    val startDate: LocalDate,
    val todayShift: String,
    val todayShiftIndex: Int? = null,  // 기준 근무의 패턴 내 인덱스 (중복 근무명 구분용)
    val referenceDate: LocalDate = LocalDate.now(),  // 기준교번의 기준 날짜
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class Position(val displayName: String, val fieldName: String) {
        ENGINEER("기관사", "dia_turns1"),
        CONDUCTOR("차장", "dia_turns2"),
        FOUR_SHIFT("4조2교대", "sub_turns")
    }
}

/**
 * 날짜별 근무 스케줄
 */
data class ShiftSchedule(
    val date: LocalDate,
    val shiftName: String
)
