package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 충당 기록 Entity
 * 테이블: shift_input_records
 */
@Entity(tableName = "shift_input_records")
data class ShiftInputRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // yyyy-MM-dd 형식
    val shiftInputTypeId: Long,
    val shiftInputTypeName: String, // 스냅샷
    val shortName: String, // 스냅샷
    val colorHex: String, // 색상 스냅샷
    val targetShiftName: String, // 교체할 교번
    val originalShiftName: String, // 원래 교번 스냅샷
    val groupId: String // 그룹 ID (UUID)
)
