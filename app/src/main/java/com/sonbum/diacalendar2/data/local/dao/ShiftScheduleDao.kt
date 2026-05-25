package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.ShiftScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftScheduleDao {

    @Query("SELECT * FROM shift_schedules ORDER BY date ASC")
    fun getAllSchedules(): Flow<List<ShiftScheduleEntity>>

    @Query("SELECT * FROM shift_schedules WHERE date = :date")
    suspend fun getScheduleByDate(date: String): ShiftScheduleEntity?

    @Query("SELECT * FROM shift_schedules WHERE date = :date")
    fun observeScheduleByDate(date: String): Flow<ShiftScheduleEntity?>

    @Query("SELECT * FROM shift_schedules WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getSchedulesBetween(startDate: String, endDate: String): Flow<List<ShiftScheduleEntity>>

    @Query("SELECT * FROM shift_schedules WHERE date LIKE :yearMonth || '%' ORDER BY date ASC")
    fun getSchedulesByMonth(yearMonth: String): Flow<List<ShiftScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<ShiftScheduleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ShiftScheduleEntity)

    @Query("DELETE FROM shift_schedules")
    suspend fun deleteAll()

    @Query("DELETE FROM shift_schedules WHERE date >= :fromDate")
    suspend fun deleteFromDate(fromDate: String)

    @Query("SELECT COUNT(*) FROM shift_schedules")
    suspend fun getCount(): Int

    @Query("SELECT * FROM shift_schedules ORDER BY date ASC")
    suspend fun getAllSchedulesOnce(): List<ShiftScheduleEntity>
}
