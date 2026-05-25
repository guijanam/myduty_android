package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.LateHolidayRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LateHolidayRecordDao {
    @Query("SELECT * FROM late_holiday_records WHERE date = :date")
    fun getByDate(date: String): Flow<LateHolidayRecordEntity?>

    @Query("SELECT * FROM late_holiday_records WHERE date = :date LIMIT 1")
    suspend fun getByDateOnce(date: String): LateHolidayRecordEntity?

    @Query("SELECT * FROM late_holiday_records")
    fun getAll(): Flow<List<LateHolidayRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: LateHolidayRecordEntity)

    @Query("DELETE FROM late_holiday_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM late_holiday_records")
    suspend fun deleteAll()
}
