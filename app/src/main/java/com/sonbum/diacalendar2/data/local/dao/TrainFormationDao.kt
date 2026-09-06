package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonbum.diacalendar2.data.local.entity.TrainFormationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainFormationDao {

    @Query("SELECT * FROM train_formations WHERE date = :date ORDER BY half, sortOrder, id")
    fun observeByDate(date: String): Flow<List<TrainFormationEntity>>

    @Query("SELECT * FROM train_formations WHERE date = :date AND half = :half ORDER BY sortOrder, id")
    suspend fun getByDateAndHalf(date: String, half: String): List<TrainFormationEntity>

    @Query("SELECT * FROM train_formations ORDER BY date DESC, half, sortOrder, id")
    fun getAll(): Flow<List<TrainFormationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TrainFormationEntity): Long

    @Update
    suspend fun update(entity: TrainFormationEntity)

    @Query("DELETE FROM train_formations WHERE id = :id")
    suspend fun deleteById(id: Long)

    // --- 백업용 ---
    @Query("SELECT * FROM train_formations ORDER BY date, half, sortOrder, id")
    suspend fun getAllOnce(): List<TrainFormationEntity>

    @Query("DELETE FROM train_formations")
    suspend fun deleteAll()
}
