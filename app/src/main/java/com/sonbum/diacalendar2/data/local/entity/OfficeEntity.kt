package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offices")
data class OfficeEntity(
    @PrimaryKey
    val officeCode: Long,
    val officeName: String,
    val diaTurns1: String?,
    val diaTurns2: String?,
    val subTurns: String?,
    val diaSelects: String?,
    val diaTurns3: String?,  // jsonb를 String으로 저장
    val adminPassword: String?
)
