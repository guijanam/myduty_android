package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonbum.diacalendar2.data.local.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Query("SELECT * FROM memos WHERE dateString = :dateString ORDER BY position ASC")
    fun getMemosByDate(dateString: String): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos ORDER BY dateString DESC, position ASC")
    fun getAllMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE objectId = :id")
    suspend fun getMemoById(id: String): MemoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: MemoEntity)

    @Update
    suspend fun updateMemo(memo: MemoEntity)

    @Delete
    suspend fun deleteMemo(memo: MemoEntity)

    @Query("DELETE FROM memos WHERE objectId = :id")
    suspend fun deleteMemoById(id: String)

    @Query("SELECT * FROM memos WHERE dateString = :dateString ORDER BY position ASC")
    suspend fun getMemosByDateOnce(dateString: String): List<MemoEntity>

    @Query("SELECT DISTINCT dateString FROM memos")
    fun getDatesWithMemos(): Flow<List<String>>

    @Query("SELECT * FROM memos WHERE dateString IN (:dateStrings) ORDER BY position ASC")
    fun getMemosByDates(dateStrings: List<String>): Flow<List<MemoEntity>>

    @Query("DELETE FROM memos")
    suspend fun deleteAllMemos()
}
