package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.MemoDao
import com.sonbum.diacalendar2.data.local.mapper.toDomain
import com.sonbum.diacalendar2.data.local.mapper.toEntity
import com.sonbum.diacalendar2.domain.model.Memo
import com.sonbum.diacalendar2.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class MemoRepositoryImpl(
    private val memoDao: MemoDao
) : MemoRepository {

    override fun getMemosByDate(date: LocalDate): Flow<List<Memo>> {
        return memoDao.getMemosByDate(date.toString()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllMemos(): Flow<List<Memo>> {
        return memoDao.getAllMemos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDatesWithMemos(): Flow<List<LocalDate>> {
        return memoDao.getDatesWithMemos().map { dateStrings ->
            dateStrings.map { LocalDate.parse(it) }
        }
    }

    override fun getMemosByDates(dates: List<LocalDate>): Flow<List<Memo>> {
        return memoDao.getMemosByDates(dates.map { it.toString() }).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMemoById(id: String): Memo? {
        return memoDao.getMemoById(id)?.toDomain()
    }

    override suspend fun insertMemo(memo: Memo) {
        memoDao.insertMemo(memo.toEntity())
    }

    override suspend fun updateMemo(memo: Memo) {
        memoDao.updateMemo(memo.toEntity())
    }

    override suspend fun deleteMemo(memo: Memo) {
        memoDao.deleteMemo(memo.toEntity())
    }

    override suspend fun deleteMemoById(id: String) {
        memoDao.deleteMemoById(id)
    }
}
