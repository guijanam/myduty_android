package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonbum.diacalendar2.data.local.entity.CoworkerGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoworkerGroupDao {
    @Query("SELECT * FROM coworker_groups ORDER BY sortOrder ASC")
    fun getAllGroups(): Flow<List<CoworkerGroupEntity>>

    @Query("SELECT * FROM coworker_groups WHERE id = :id")
    suspend fun getById(id: Long): CoworkerGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: CoworkerGroupEntity): Long

    @Update
    suspend fun update(group: CoworkerGroupEntity)

    @Query("DELETE FROM coworker_groups WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE coworker_groups SET sortOrder = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)

    @Query("SELECT * FROM coworker_groups ORDER BY sortOrder ASC")
    suspend fun getAllOnce(): List<CoworkerGroupEntity>

    @Query("DELETE FROM coworker_groups")
    suspend fun deleteAll()
}
