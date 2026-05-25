package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.ShiftSwapRecordDao
import com.sonbum.diacalendar2.data.local.entity.ShiftSwapRecordEntity
import com.sonbum.diacalendar2.domain.model.ShiftSwapRecord
import com.sonbum.diacalendar2.domain.repository.ShiftSwapRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class ShiftSwapRecordRepositoryImpl(
    private val shiftSwapRecordDao: ShiftSwapRecordDao
) : ShiftSwapRecordRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun getAllRecords(): Flow<List<ShiftSwapRecord>> {
        return shiftSwapRecordDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeByDate(date: LocalDate): Flow<ShiftSwapRecord?> {
        return shiftSwapRecordDao.observeByDate(date.format(dateFormatter)).map { it?.toDomain() }
    }

    override suspend fun getByDate(date: LocalDate): ShiftSwapRecord? {
        return shiftSwapRecordDao.getByDate(date.format(dateFormatter))?.toDomain()
    }

    override suspend fun addSwap(
        startDate: LocalDate,
        days: Int,
        targetShiftName: String,
        shiftPattern: List<String>,
        originalScheduleProvider: suspend (LocalDate) -> String?
    ) {
        val targetIndex = shiftPattern.indexOf(targetShiftName)
        if (targetIndex == -1) return

        val groupId = UUID.randomUUID().toString()
        val patternSize = shiftPattern.size

        val records = (0 until days).mapNotNull { offset ->
            val date = startDate.plusDays(offset.toLong())
            val shiftIndex = (targetIndex + offset) % patternSize
            val swappedShift = shiftPattern[shiftIndex]
            val originalShift = originalScheduleProvider(date) ?: return@mapNotNull null

            // 기존 교체 레코드가 있으면 삭제
            shiftSwapRecordDao.deleteByDate(date.format(dateFormatter))

            ShiftSwapRecordEntity(
                date = date.format(dateFormatter),
                originalShiftName = originalShift,
                swappedShiftName = swappedShift,
                groupId = groupId
            )
        }

        if (records.isNotEmpty()) {
            shiftSwapRecordDao.insertAll(records)
        }
    }

    override suspend fun deleteByGroupId(groupId: String) {
        shiftSwapRecordDao.deleteByGroupId(groupId)
    }

    override suspend fun deleteByDate(date: LocalDate) {
        shiftSwapRecordDao.deleteByDate(date.format(dateFormatter))
    }

    private fun ShiftSwapRecordEntity.toDomain(): ShiftSwapRecord {
        return ShiftSwapRecord(
            id = id,
            date = LocalDate.parse(date, dateFormatter),
            originalShiftName = originalShiftName,
            swappedShiftName = swappedShiftName,
            groupId = groupId
        )
    }
}
