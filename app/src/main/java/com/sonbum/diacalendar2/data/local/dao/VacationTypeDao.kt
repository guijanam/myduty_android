package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.VacationTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VacationTypeDao {
    @Query("SELECT * FROM vacation_types ORDER BY id ASC")
    fun getAllVacationTypes(): Flow<List<VacationTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vacationType: VacationTypeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vacationTypes: List<VacationTypeEntity>)

    @Query("DELETE FROM vacation_types WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM vacation_types")
    suspend fun getCount(): Int

    @Query("SELECT * FROM vacation_types WHERE id = :id")
    suspend fun getById(id: Long): VacationTypeEntity?

    @Query("UPDATE vacation_types SET shortName = :shortName WHERE id = :id")
    suspend fun updateShortName(id: Long, shortName: String)

    @Query("UPDATE vacation_types SET name = :name, shortName = :shortName WHERE id = :id")
    suspend fun updateNameAndShortName(id: Long, name: String, shortName: String)

    @Query("DELETE FROM vacation_types")
    suspend fun deleteAll()
}
