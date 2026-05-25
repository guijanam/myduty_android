package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dias",
    foreignKeys = [
        ForeignKey(
            entity = OfficeEntity::class,
            parentColumns = ["officeCode"],
            childColumns = ["officeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["officeId"])]
)
data class DiaEntity(
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
    val workTime: String?
)
