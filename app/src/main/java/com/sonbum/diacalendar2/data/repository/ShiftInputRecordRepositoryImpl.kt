package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.ShiftInputRecordDao
import com.sonbum.diacalendar2.data.local.entity.ShiftInputRecordEntity
import com.sonbum.diacalendar2.domain.model.ShiftInputRecord
import com.sonbum.diacalendar2.domain.model.ShiftInputType
import com.sonbum.diacalendar2.domain.repository.ShiftInputRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class ShiftInputRecordRepositoryImpl(
    private val shiftInputRecordDao: ShiftInputRecordDao
) : ShiftInputRecordRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun getAllRecords(): Flow<List<ShiftInputRecord>> {
        return shiftInputRecordDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeByDate(date: LocalDate): Flow<ShiftInputRecord?> {
        return shiftInputRecordDao.observeByDate(date.format(dateFormatter)).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getByDate(date: LocalDate): ShiftInputRecord? {
        return shiftInputRecordDao.getByDate(date.format(dateFormatter))?.toDomain()
    }

    override suspend fun addShiftInput(
        startDate: LocalDate,
        days: Int,
        shiftInputType: ShiftInputType,
        targetShiftName: String,
        shiftPattern: List<String>,
        originalScheduleProvider: suspend (LocalDate) -> String?
    ) {
        val groupId = UUID.randomUUID().toString()
        val records = mutableListOf<ShiftInputRecordEntity>()

        // 교번교체와 동일한 로직: 시작일부터 days 일수만큼 순환 패턴 적용
        // 패턴에 없는 근무가 선택된 경우(targetIndex == -1)에는 순환 없이 그대로 사용
        val patternSize = shiftPattern.size
        val targetIndex = shiftPattern.indexOf(targetShiftName)
        val canRotate = targetIndex >= 0 && patternSize > 0

        for (offset in 0 until days) {
            val currentDate = startDate.plusDays(offset.toLong())
            val dateStr = currentDate.format(dateFormatter)

            // 기존 충당 레코드 삭제
            shiftInputRecordDao.deleteByDate(dateStr)

            // 원래 교번 조회
            val originalShiftName = originalScheduleProvider(currentDate) ?: ""

            // 교체할 교번 계산 (순환 패턴)
            val swappedShiftName = if (canRotate) {
                val swappedIndex = ((targetIndex + offset) % patternSize + patternSize) % patternSize
                shiftPattern[swappedIndex]
            } else {
                targetShiftName
            }

            records.add(
                ShiftInputRecordEntity(
                    date = dateStr,
                    shiftInputTypeId = shiftInputType.id,
                    shiftInputTypeName = shiftInputType.name,
                    shortName = shiftInputType.shortName,
                    colorHex = shiftInputType.colorHex,
                    targetShiftName = swappedShiftName,
                    originalShiftName = originalShiftName,
                    groupId = groupId
                )
            )
        }

        shiftInputRecordDao.insertAll(records)
    }

    override suspend fun deleteByDate(date: LocalDate) {
        shiftInputRecordDao.deleteByDate(date.format(dateFormatter))
    }

    override suspend fun deleteByGroupId(groupId: String) {
        shiftInputRecordDao.deleteByGroupId(groupId)
    }

    private fun ShiftInputRecordEntity.toDomain(): ShiftInputRecord {
        return ShiftInputRecord(
            id = id,
            date = LocalDate.parse(date, dateFormatter),
            shiftInputTypeId = shiftInputTypeId,
            shiftInputTypeName = shiftInputTypeName,
            shortName = shortName,
            colorHex = colorHex,
            targetShiftName = targetShiftName,
            originalShiftName = originalShiftName,
            groupId = groupId
        )
    }
}
