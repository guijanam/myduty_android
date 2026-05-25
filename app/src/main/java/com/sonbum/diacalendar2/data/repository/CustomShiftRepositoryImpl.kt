package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.CustomShiftDao
import com.sonbum.diacalendar2.data.local.entity.CustomShiftEntity
import com.sonbum.diacalendar2.domain.model.CustomShift
import com.sonbum.diacalendar2.domain.repository.CustomShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomShiftRepositoryImpl(
    private val customShiftDao: CustomShiftDao
) : CustomShiftRepository {

    override fun getAllCustomShifts(): Flow<List<CustomShift>> {
        return customShiftDao.getAllCustomShifts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCustomShiftById(id: Long): CustomShift? {
        return customShiftDao.getCustomShiftById(id)?.toDomain()
    }

    override suspend fun insertCustomShift(shift: CustomShift): Long {
        return customShiftDao.insert(shift.toEntity())
    }

    override suspend fun updateCustomShift(shift: CustomShift) {
        customShiftDao.update(shift.toEntity())
    }

    override suspend fun deleteCustomShift(id: Long) {
        customShiftDao.deleteById(id)
    }

    private fun CustomShiftEntity.toDomain(): CustomShift {
        return CustomShift(
            id = id,
            shiftName = shiftName,
            shiftPattern = shiftPattern
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() },
            createdAt = createdAt
        )
    }

    private fun CustomShift.toEntity(): CustomShiftEntity {
        return CustomShiftEntity(
            id = id,
            shiftName = shiftName,
            shiftPattern = shiftPattern.joinToString(","),
            createdAt = createdAt
        )
    }
}
