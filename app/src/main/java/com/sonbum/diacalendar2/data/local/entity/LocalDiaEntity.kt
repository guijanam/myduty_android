package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_dias",
    foreignKeys = [
        ForeignKey(
            entity = LocalOfficeEntity::class,
            parentColumns = ["id"],
            childColumns = ["localOfficeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["localOfficeId"])]
)
data class LocalDiaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaId: String,
    val localOfficeId: Long,
    val officeName: String,
    val typeName: String?,
    val firstTime: String?,
    val numTr1: String?,
    val numTr2: String?,
    val secondTime: String?,
    val thirdTime: String?,
    val totalTime: String?,
    val workTime: String?
)
