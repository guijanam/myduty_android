package com.sonbum.diacalendar2.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class ChatNote(
	val id: String = UUID.randomUUID().toString(),
	val content: String = "",
	val createdAt: LocalDateTime = LocalDateTime.now(),
	val isPinned: Boolean = false,
	val imagePath: String? = null
)
