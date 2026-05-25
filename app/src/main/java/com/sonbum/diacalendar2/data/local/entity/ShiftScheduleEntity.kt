package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 3년치 근무 스케줄을 저장하는 Entity
 * - 날짜별 근무를 저장
 */
@Entity(
    tableName = "shift_schedules",
    indices = [Index(value = ["date"], unique = true)]
)
data class ShiftScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,  // yyyy-MM-dd 형식
    val shiftName: String  // 근무명 (예: "1", "2", "비", "주휴" 등)
)
