package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vacation_types")
data class VacationTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val shortName: String,
    val isDefault: Boolean = false,
    val annualQuota: Int = 0,
    val resetMonthDay: String = "01-01"
)
