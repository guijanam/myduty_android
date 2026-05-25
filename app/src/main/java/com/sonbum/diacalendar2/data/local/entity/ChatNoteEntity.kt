package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_notes")
data class ChatNoteEntity(
	@PrimaryKey
	val id: String = UUID.randomUUID().toString(),
	val content: String,
	val createdAt: String,
	val isPinned: Boolean = false,
	val imagePath: String? = null
)
