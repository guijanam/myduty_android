package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 교번교체 레코드 Entity
 * - 원래 근무 위에 오버레이되는 교체된 근무 저장
 * - groupId로 한번의 교체 작업을 묶어 일괄 삭제 가능
 */
@Entity(tableName = "shift_swap_records")
data class ShiftSwapRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,              // yyyy-MM-dd 형식
    val originalShiftName: String, // 원래 근무명 스냅샷
    val swappedShiftName: String,  // 교체된 근무명
    val groupId: String            // 한번의 교체 작업을 묶는 UUID
)
