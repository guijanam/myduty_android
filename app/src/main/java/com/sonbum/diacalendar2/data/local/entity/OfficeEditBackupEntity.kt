package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "office_edit_backups")
data class OfficeEditBackupEntity(
    @PrimaryKey
    val officeCode: Long,
    val officeName: String,
    val diaTurns1: String?,
    val diaTurns2: String?,
    val subTurns: String?,
    val diaSelects: String?,
    val diaTurns3: String?,
    val adminPassword: String?,
    val backupTimestamp: Long
)
