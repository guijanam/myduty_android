package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.LateWorkRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LateWorkRecordDao {
    @Query("SELECT * FROM late_work_records WHERE date = :date")
    fun getByDate(date: String): Flow<LateWorkRecordEntity?>

    @Query("SELECT * FROM late_work_records WHERE date = :date LIMIT 1")
    suspend fun getByDateOnce(date: String): LateWorkRecordEntity?

    @Query("SELECT * FROM late_work_records")
    fun getAll(): Flow<List<LateWorkRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: LateWorkRecordEntity)

    @Query("DELETE FROM late_work_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM late_work_records")
    suspend fun deleteAll()
}
