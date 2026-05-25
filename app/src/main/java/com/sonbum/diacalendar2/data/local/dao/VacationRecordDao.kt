package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.VacationRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VacationRecordDao {

    @Query("SELECT * FROM vacation_records WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): VacationRecordEntity?

    @Query("SELECT * FROM vacation_records WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<VacationRecordEntity?>

    @Query("SELECT * FROM vacation_records")
    fun getAll(): Flow<List<VacationRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: VacationRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<VacationRecordEntity>)

    @Query("DELETE FROM vacation_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM vacation_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vacation_records")
    suspend fun deleteAll()
}
