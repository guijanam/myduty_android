package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * sub 근무(보조 교번)의 날짜별 근무순서를 저장하는 Entity
 * - 메인 근무(shift_schedules)와 동일한 구조이나 별도 테이블로 관리
 * - 근무순서(교번 이름)만 저장하며 근무표 상세 정보는 가지지 않는다
 */
@Entity(
    tableName = "sub_shift_schedules",
    indices = [Index(value = ["date"], unique = true)]
)
data class SubShiftScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,  // yyyy-MM-dd 형식
    val shiftName: String
)
