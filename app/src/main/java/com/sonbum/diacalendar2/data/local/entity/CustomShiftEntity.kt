package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_shifts")
data class CustomShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftName: String,
    val shiftPattern: String,
    val createdAt: Long = System.currentTimeMillis()
)
