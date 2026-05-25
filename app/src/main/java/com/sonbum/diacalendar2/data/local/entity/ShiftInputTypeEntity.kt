package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 충당 종류 Entity
 * 테이블: shift_input_types
 */
@Entity(tableName = "shift_input_types")
data class ShiftInputTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val shortName: String,
    val colorHex: String,
    val isDefault: Int = 0, // Room은 Boolean을 직접 지원하지 않으므로 Int 사용
    val requiresLateWork: Int = 0 // 지근이 설정된 날짜에만 사용 가능 여부
)
