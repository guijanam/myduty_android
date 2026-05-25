package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sonbum.diacalendar2.data.local.entity.ShiftInputRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftInputRecordDao {

    @Query("SELECT * FROM shift_input_records WHERE date = :date LIMIT 1")
    fun observeByDate(date: String): Flow<ShiftInputRecordEntity?>

    @Query("SELECT * FROM shift_input_records WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): ShiftInputRecordEntity?

    @Query("SELECT * FROM shift_input_records ORDER BY date ASC")
    fun getAll(): Flow<List<ShiftInputRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ShiftInputRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ShiftInputRecordEntity>)

    @Query("DELETE FROM shift_input_records WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM shift_input_records WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Query("DELETE FROM shift_input_records")
    suspend fun deleteAll()
}
