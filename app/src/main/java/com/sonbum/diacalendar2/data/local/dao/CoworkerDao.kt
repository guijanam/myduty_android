package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonbum.diacalendar2.data.local.entity.CoworkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoworkerDao {
    @Query("SELECT * FROM coworkers ORDER BY sortOrder ASC")
    fun getAllCoworkers(): Flow<List<CoworkerEntity>>

    @Query("SELECT * FROM coworkers WHERE id = :id")
    suspend fun getById(id: Long): CoworkerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(coworker: CoworkerEntity): Long

    @Update
    suspend fun update(coworker: CoworkerEntity)

    @Query("DELETE FROM coworkers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE coworkers SET sortOrder = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)

    @Query("SELECT * FROM coworkers")
    suspend fun getAllOnce(): List<CoworkerEntity>
}
