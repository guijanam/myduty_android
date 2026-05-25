package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.HolidayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holidays")
    fun getAllHolidays(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays WHERE locdate = :date")
    suspend fun getHolidayByDate(date: String): HolidayEntity?

    @Query("SELECT locdate FROM holidays WHERE isHoliday = 'Y'")
    fun getAllHolidayDates(): Flow<List<String>>

    @Query("SELECT locdate FROM holidays WHERE isHoliday = 'Y'")
    suspend fun getAllHolidayDatesOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holidays: List<HolidayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(holiday: HolidayEntity)

    @Query("DELETE FROM holidays")
    suspend fun deleteAll()

    // 서버 공휴일만 삭제 (사용자 생성 공휴일 보존)
    @Query("DELETE FROM holidays WHERE isUserCreated = 0")
    suspend fun deleteServerHolidays()

    // 특정 공휴일 삭제
    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteById(id: String)

    // 사용자 생성 공휴일만 조회
    @Query("SELECT * FROM holidays WHERE isUserCreated = 1")
    fun getUserCreatedHolidays(): Flow<List<HolidayEntity>>
}
