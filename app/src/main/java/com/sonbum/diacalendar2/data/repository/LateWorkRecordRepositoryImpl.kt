package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.LateWorkRecordDao
import com.sonbum.diacalendar2.data.local.entity.LateWorkRecordEntity
import com.sonbum.diacalendar2.domain.model.LateWorkRecord
import com.sonbum.diacalendar2.domain.repository.LateWorkRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class LateWorkRecordRepositoryImpl(
    private val dao: LateWorkRecordDao
) : LateWorkRecordRepository {

    override fun getAllRecords(): Flow<List<LateWorkRecord>> {
        return dao.getAll().map { list ->
            list.map { entity ->
                entity.toDomain(LocalDate.parse(entity.date))
            }
        }
    }

    override fun observeByDate(date: LocalDate): Flow<LateWorkRecord?> {
        return dao.getByDate(date.toString()).map { entity ->
            entity?.toDomain(date)
        }
    }

    override suspend fun addLateWork(
        startDate: LocalDate,
        days: Int,
        lateWorkTypeId: Long,
        lateWorkName: String,
        shortName: String
    ) {
        // NOTE: days 파라미터가 있지만, 현재 DB 구조상 1일치만 저장하는 것으로 보임.
        // VacationRecordRepositoryImpl도 비슷하게 동작할 것으로 추정됨.
        // 일단 단일 날짜에 대해서만 저장하도록 구현.
        // 만약 기간 입력이 필요하다면 loop를 돌며 insert해야 함.
        for (i in 0 until days) {
            val date = startDate.plusDays(i.toLong())
            val entity = LateWorkRecordEntity(
                date = date.toString(),
                lateWorkTypeId = lateWorkTypeId,
                lateWorkName = lateWorkName,
                shortName = shortName
            )
            dao.insert(entity)
        }
    }

    override suspend fun deleteByDate(date: LocalDate) {
        dao.deleteByDate(date.toString())
    }

    private fun LateWorkRecordEntity.toDomain(dateObj: LocalDate): LateWorkRecord {
        return LateWorkRecord(
            id = id,
            date = dateObj,
            lateWorkTypeId = lateWorkTypeId,
            lateWorkName = lateWorkName,
            shortName = shortName
        )
    }
}
