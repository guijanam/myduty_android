package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_offices")
data class LocalOfficeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val officeName: String,
    val diaTurns1: String?,
    val diaTurns2: String?,
    val subTurns: String?,
    val diaSelects: String?,
    val diaTurns3: String?,  // 운휴 근무 (공휴일/일요일 포함 시 휴무 계산에 사용)
    val createdAt: Long = System.currentTimeMillis()
)
