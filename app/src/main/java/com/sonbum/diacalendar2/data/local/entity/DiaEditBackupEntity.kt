package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dia_edit_backups")
data class DiaEditBackupEntity(
    @PrimaryKey
    val id: Long,
    val diaId: String,
    val officeName: String,
    val officeId: Int?,
    val typeName: String?,
    val firstTime: String?,
    val numTr1: String?,
    val numTr2: String?,
    val secondTime: String?,
    val thirdTime: String?,
    val totalTime: String?,
    val workTime: String?,
    val backupTimestamp: Long
)
