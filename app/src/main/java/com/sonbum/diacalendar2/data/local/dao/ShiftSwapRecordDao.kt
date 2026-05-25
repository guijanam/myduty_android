package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.ShiftSwapRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftSwapRecordDao {

    @Query("SELECT * FROM shift_swap_records WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): ShiftSwapRecordEntity?

    @Query("SELECT * FROM shift_swap_records WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<ShiftSwapRecordEntity?>

    @Query("SELECT * FROM shift_swap_records")
    fun getAll(): Flow<List<ShiftSwapRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ShiftSwapRecordEntity>)

    @Query("DELETE FROM shift_swap_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM shift_swap_records WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ShiftSwapRecordEntity): Long

    @Query("DELETE FROM shift_swap_records")
    suspend fun deleteAll()
}
