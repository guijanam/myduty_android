package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey
    val id: String,
    val locdate: String,  // yyyy-MM-dd 형식
    val dateName: String,
    val isHoliday: String = "Y",
    val isUserCreated: Boolean = false  // 사용자가 직접 추가한 공휴일 여부
)
