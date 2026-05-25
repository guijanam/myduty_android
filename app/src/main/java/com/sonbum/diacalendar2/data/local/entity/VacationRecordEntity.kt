package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vacation_records")
data class VacationRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,          // "2026-01-27" 형식
    val vacationTypeId: Long,
    val vacationName: String,  // 스냅샷 (휴가 타입 삭제 시에도 표시 가능)
    val shortName: String      // 스냅샷
)
