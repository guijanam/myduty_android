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

    // 달력 표시용: 전체가 아닌 기간 범위만 구독하여 힙 상주량을 제한
    @Query(
        "SELECT * FROM memos WHERE dateString BETWEEN :startDate AND :endDate " +
            "ORDER BY dateString DESC, position ASC"
    )
    fun getMemosBetween(startDate: String, endDate: String): Flow<List<MemoEntity>>

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

    /**
     * 메모 내역 화면용 페이지 조회.
     * 연도/검색어 필터를 SQL로 내려 전체 메모를 힙에 올리지 않는다.
     * year 가 null 이면 전체 연도, query 가 빈 문자열이면 검색 없음.
     */
    @Query(
        """
        SELECT * FROM memos
        WHERE (:year IS NULL OR substr(dateString, 1, 4) = :year)
          AND (
            :query = ''
            OR title LIKE '%' || :query || '%'
            OR content LIKE '%' || :query || '%'
          )
        ORDER BY dateString DESC, position ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getMemosPaged(
        year: String?,
        query: String,
        limit: Int,
        offset: Int
    ): List<MemoEntity>

    /** 연도 선택 드롭다운용: 메모가 존재하는 연도만 내림차순으로 조회 */
    @Query("SELECT DISTINCT substr(dateString, 1, 4) FROM memos ORDER BY 1 DESC")
    fun getMemoYears(): Flow<List<String>>

    @Query("SELECT * FROM memos WHERE dateString IN (:dateStrings) ORDER BY position ASC")
    fun getMemosByDates(dateStrings: List<String>): Flow<List<MemoEntity>>

    @Query("DELETE FROM memos")
    suspend fun deleteAllMemos()
}
