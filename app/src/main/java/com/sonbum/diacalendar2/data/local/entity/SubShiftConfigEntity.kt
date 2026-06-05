package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자의 sub 근무(보조 교번) 설정을 저장하는 Entity
 * - 메인 근무 설정(user_shift_config)과 동일한 구조이나 별도 테이블로 관리
 * - 하나의 설정만 유지 (id = 1로 고정)
 */
@Entity(tableName = "sub_shift_config")
data class SubShiftConfigEntity(
    @PrimaryKey
    val id: Int = 1,  // 항상 1로 고정 (단일 설정)
    val officeCode: Long,
    val officeName: String,
    val position: String,  // "기관사", "차장", "4조2교대"
    val shiftPattern: String,
    val startDate: String,  // 시작 날짜 (yyyy-MM-dd)
    val todayShift: String,  // 기준 근무
    val todayShiftIndex: Int? = null,
    val referenceDate: String = "",  // 기준 날짜 (yyyy-MM-dd)
    val createdAt: Long = System.currentTimeMillis()
)
