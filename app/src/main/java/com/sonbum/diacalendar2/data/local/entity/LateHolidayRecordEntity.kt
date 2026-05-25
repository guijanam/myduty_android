package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "late_holiday_records")
data class LateHolidayRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,          // "2026-01-27" 형식
    val lateHolidayTypeId: Long,
    val lateHolidayName: String,  // 스냅샷
    val shortName: String      // 스냅샷
)
