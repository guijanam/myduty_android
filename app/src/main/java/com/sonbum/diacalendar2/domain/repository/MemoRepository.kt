package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.Memo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MemoRepository {
    fun getMemosByDate(date: LocalDate): Flow<List<Memo>>
    fun getAllMemos(): Flow<List<Memo>>
    fun getDatesWithMemos(): Flow<List<LocalDate>>
    fun getMemosByDates(dates: List<LocalDate>): Flow<List<Memo>>
    suspend fun getMemoById(id: String): Memo?
    suspend fun insertMemo(memo: Memo)
    suspend fun updateMemo(memo: Memo)
    suspend fun deleteMemo(memo: Memo)
    suspend fun deleteMemoById(id: String)
}
