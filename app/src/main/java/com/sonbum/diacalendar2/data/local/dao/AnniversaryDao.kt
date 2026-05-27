package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sonbum.diacalendar2.data.local.entity.AnniversaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnniversaryDao {
    @Query("SELECT * FROM anniversaries ORDER BY month, day")
    fun getAll(): Flow<List<AnniversaryEntity>>

    @Insert
    suspend fun insert(entity: AnniversaryEntity)

    @Update
    suspend fun update(entity: AnniversaryEntity)

    @Query("DELETE FROM anniversaries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM anniversaries ORDER BY month, day")
    suspend fun getAllOnce(): List<AnniversaryEntity>

    @Query("DELETE FROM anniversaries")
    suspend fun deleteAll()
}
