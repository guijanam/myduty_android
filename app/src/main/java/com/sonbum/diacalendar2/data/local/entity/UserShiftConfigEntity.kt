package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자의 교번 설정을 저장하는 Entity
 * - 하나의 설정만 유지 (id = 1로 고정)
 */
@Entity(tableName = "user_shift_config")
data class UserShiftConfigEntity(
    @PrimaryKey
    val id: Int = 1,  // 항상 1로 고정 (단일 설정)
    val officeCode: Long,
    val officeName: String,
    val position: String,  // "기관사", "차장", "4조2교대"
    val shiftPattern: String,  // dia_turns1, dia_turns2, sub_turns 중 해당하는 값
    val startDate: String,  // 시작 날짜 (yyyy-MM-dd)
    val todayShift: String,  // 기준 근무 (dia_selects 중 선택한 값)
    val todayShiftIndex: Int? = null,  // 기준 근무의 패턴 내 인덱스 (중복 근무명 구분용)
    val referenceDate: String = "",  // 기준교번의 기준 날짜 (yyyy-MM-dd)
    val createdAt: Long = System.currentTimeMillis()
)
