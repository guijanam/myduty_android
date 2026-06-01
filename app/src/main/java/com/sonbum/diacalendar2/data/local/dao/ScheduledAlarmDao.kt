package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.ScheduledAlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledAlarmDao {

    @Query("SELECT * FROM scheduled_alarms ORDER BY triggerAtMillis ASC")
    fun observeAll(): Flow<List<ScheduledAlarmEntity>>

    @Query("SELECT * FROM scheduled_alarms ORDER BY triggerAtMillis ASC")
    suspend fun getAllOnce(): List<ScheduledAlarmEntity>

    @Query("SELECT * FROM scheduled_alarms WHERE date = :date AND slot = :slot LIMIT 1")
    suspend fun getByDateSlot(date: String, slot: Int): ScheduledAlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alarm: ScheduledAlarmEntity)

    @Query("UPDATE scheduled_alarms SET dismissed = :dismissed WHERE date = :date AND slot = :slot")
    suspend fun setDismissed(date: String, slot: Int, dismissed: Boolean)

    @Query("DELETE FROM scheduled_alarms WHERE date = :date AND slot = :slot")
    suspend fun delete(date: String, slot: Int)

    /** 윈도우 밖(과거 또는 5일 이후) 정리 */
    @Query("DELETE FROM scheduled_alarms WHERE date < :fromDate OR date > :toDate")
    suspend fun deleteOutsideWindow(fromDate: String, toDate: String)

    @Query("DELETE FROM scheduled_alarms")
    suspend fun clear()
}
