package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.ShiftInputTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftInputTypeDao {

    @Query("SELECT * FROM shift_input_types ORDER BY id ASC")
    fun getAll(): Flow<List<ShiftInputTypeEntity>>

    @Query("SELECT * FROM shift_input_types WHERE id = :id")
    suspend fun getById(id: Long): ShiftInputTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: ShiftInputTypeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<ShiftInputTypeEntity>)

    @Query("DELETE FROM shift_input_types WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE shift_input_types SET name = :name, shortName = :shortName, colorHex = :colorHex, requiresLateWork = :requiresLateWork WHERE id = :id")
    suspend fun update(id: Long, name: String, shortName: String, colorHex: String, requiresLateWork: Int)

    @Query("SELECT COUNT(*) FROM shift_input_types")
    suspend fun getCount(): Int

    @Query("DELETE FROM shift_input_types")
    suspend fun deleteAll()
}
