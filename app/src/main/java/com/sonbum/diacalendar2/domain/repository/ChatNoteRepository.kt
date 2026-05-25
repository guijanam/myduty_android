package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.ChatNote
import kotlinx.coroutines.flow.Flow

interface ChatNoteRepository {
	fun getAllNotes(): Flow<List<ChatNote>>
	suspend fun insertNote(note: ChatNote)
	suspend fun updateNote(note: ChatNote)
	suspend fun deleteNote(note: ChatNote)
}
