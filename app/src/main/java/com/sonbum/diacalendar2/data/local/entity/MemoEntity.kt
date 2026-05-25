package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey
    val objectId: String = UUID.randomUUID().toString(),
    val hexColorString: String,
    val title: String,
    val content: String,
    val startTimeString: String,
    val endTimeString: String,
    val dateString: String,  // yyyy-MM-dd 형식
    val isCompleted: Boolean = false,
    val position: Long = 0L,
    val reminderEnabled: Boolean = false,
    val reminderTimeMillis: Long? = null,
    val imagePath: String? = null,
    val isAllDay: Boolean = false
)
