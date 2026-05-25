package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.LateHolidayTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LateHolidayTypeDao {
    @Query("SELECT * FROM late_holiday_types ORDER BY id ASC")
    fun getAll(): Flow<List<LateHolidayTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: LateHolidayTypeEntity): Long

    @Query("DELETE FROM late_holiday_types WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE late_holiday_types SET name = :name, shortName = :shortName WHERE id = :id")
    suspend fun update(id: Long, name: String, shortName: String)

    @Query("DELETE FROM late_holiday_types")
    suspend fun deleteAll()
}
