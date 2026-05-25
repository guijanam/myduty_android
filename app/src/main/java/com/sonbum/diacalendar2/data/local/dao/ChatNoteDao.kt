package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonbum.diacalendar2.data.local.entity.ChatNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatNoteDao {
	@Query("SELECT * FROM chat_notes ORDER BY createdAt ASC")
	fun getAllNotes(): Flow<List<ChatNoteEntity>>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(note: ChatNoteEntity)

	@Update
	suspend fun update(note: ChatNoteEntity)

	@Delete
	suspend fun delete(note: ChatNoteEntity)

	@Query("DELETE FROM chat_notes")
	suspend fun deleteAll()
}
