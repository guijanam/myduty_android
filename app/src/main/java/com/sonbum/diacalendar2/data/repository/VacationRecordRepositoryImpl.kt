package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.VacationRecordDao
import com.sonbum.diacalendar2.data.local.entity.VacationRecordEntity
import com.sonbum.diacalendar2.domain.model.VacationRecord
import com.sonbum.diacalendar2.domain.repository.VacationRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class VacationRecordRepositoryImpl(
    private val vacationRecordDao: VacationRecordDao
) : VacationRecordRepository {

    override fun getAllRecords(): Flow<List<VacationRecord>> {
        return vacationRecordDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeByDate(date: LocalDate): Flow<VacationRecord?> {
        return vacationRecordDao.observeByDate(date.toString()).map { it?.toDomain() }
    }

    override suspend fun getByDate(date: LocalDate): VacationRecord? {
        return vacationRecordDao.getByDate(date.toString())?.toDomain()
    }

    override suspend fun addVacation(
        startDate: LocalDate,
        days: Int,
        vacationTypeId: Long,
        vacationName: String,
        shortName: String
    ) {
        val records = (0 until days).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            VacationRecordEntity(
                date = date.toString(),
                vacationTypeId = vacationTypeId,
                vacationName = vacationName,
                shortName = shortName
            )
        }
        vacationRecordDao.insertAll(records)
    }

    override suspend fun deleteByDate(date: LocalDate) {
        vacationRecordDao.deleteByDate(date.toString())
    }

    private fun VacationRecordEntity.toDomain(): VacationRecord {
        return VacationRecord(
            id = id,
            date = LocalDate.parse(date),
            vacationTypeId = vacationTypeId,
            vacationName = vacationName,
            shortName = shortName
        )
    }
}
