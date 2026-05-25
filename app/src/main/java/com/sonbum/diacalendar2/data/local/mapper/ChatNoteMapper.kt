package com.sonbum.diacalendar2.data.local.mapper

import com.sonbum.diacalendar2.data.local.entity.ChatNoteEntity
import com.sonbum.diacalendar2.domain.model.ChatNote
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun ChatNoteEntity.toDomain(): ChatNote {
	return ChatNote(
		id = id,
		content = content,
		createdAt = LocalDateTime.parse(createdAt, dateTimeFormatter),
		isPinned = isPinned,
		imagePath = imagePath
	)
}

fun ChatNote.toEntity(): ChatNoteEntity {
	return ChatNoteEntity(
		id = id,
		content = content,
		createdAt = createdAt.format(dateTimeFormatter),
		isPinned = isPinned,
		imagePath = imagePath
	)
}
