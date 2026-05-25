package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.LateWorkTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LateWorkTypeDao {
    @Query("SELECT * FROM late_work_types ORDER BY id ASC")
    fun getAll(): Flow<List<LateWorkTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: LateWorkTypeEntity): Long

    @Query("DELETE FROM late_work_types WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE late_work_types SET name = :name, shortName = :shortName WHERE id = :id")
    suspend fun update(id: Long, name: String, shortName: String)

    @Query("DELETE FROM late_work_types")
    suspend fun deleteAll()
}
