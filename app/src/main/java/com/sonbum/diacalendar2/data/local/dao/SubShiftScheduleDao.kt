package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.SubShiftScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubShiftScheduleDao {

    @Query("SELECT * FROM sub_shift_schedules ORDER BY date ASC")
    fun getAllSchedules(): Flow<List<SubShiftScheduleEntity>>

    @Query("SELECT * FROM sub_shift_schedules WHERE date = :date")
    suspend fun getScheduleByDate(date: String): SubShiftScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<SubShiftScheduleEntity>)

    @Query("DELETE FROM sub_shift_schedules")
    suspend fun deleteAll()

    @Query("DELETE FROM sub_shift_schedules WHERE date >= :fromDate")
    suspend fun deleteFromDate(fromDate: String)

    @Query("SELECT COUNT(*) FROM sub_shift_schedules")
    suspend fun getCount(): Int
}
