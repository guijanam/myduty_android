package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.ChatNoteDao
import com.sonbum.diacalendar2.data.local.mapper.toDomain
import com.sonbum.diacalendar2.data.local.mapper.toEntity
import com.sonbum.diacalendar2.domain.model.ChatNote
import com.sonbum.diacalendar2.domain.repository.ChatNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatNoteRepositoryImpl(
	private val chatNoteDao: ChatNoteDao
) : ChatNoteRepository {

	override fun getAllNotes(): Flow<List<ChatNote>> {
		return chatNoteDao.getAllNotes().map { entities ->
			entities.map { it.toDomain() }
		}
	}

	override suspend fun insertNote(note: ChatNote) {
		chatNoteDao.insert(note.toEntity())
	}

	override suspend fun updateNote(note: ChatNote) {
		chatNoteDao.update(note.toEntity())
	}

	override suspend fun deleteNote(note: ChatNote) {
		chatNoteDao.delete(note.toEntity())
	}
}
