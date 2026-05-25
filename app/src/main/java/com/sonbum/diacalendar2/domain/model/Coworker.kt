package com.sonbum.diacalendar2.domain.model

import java.time.LocalDate

data class Coworker(
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    /** 소속 그룹 ID 목록 (여러 그룹 중복 가능) */
    val groupIds: List<Long> = emptyList(),
    /** 근무 순환 패턴 */
    val shiftPattern: List<String> = emptyList(),
    /** 기준 날짜 */
    val referenceDate: LocalDate? = null,
    /** 기준 근무명 */
    val referenceShift: String = "",
    /** 기준 근무의 패턴 내 인덱스 (중복 근무명 구분용) */
    val referenceShiftIndex: Int? = null,
    val createdAt: Long = 0
)
