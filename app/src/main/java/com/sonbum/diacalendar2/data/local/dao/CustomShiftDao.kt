package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonbum.diacalendar2.data.local.entity.CustomShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomShiftDao {
    @Query("SELECT * FROM custom_shifts ORDER BY createdAt DESC")
    fun getAllCustomShifts(): Flow<List<CustomShiftEntity>>

    @Query("SELECT * FROM custom_shifts WHERE id = :id")
    suspend fun getCustomShiftById(id: Long): CustomShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shift: CustomShiftEntity): Long

    @Update
    suspend fun update(shift: CustomShiftEntity)

    @Query("DELETE FROM custom_shifts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
