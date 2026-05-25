package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.ShiftInputTypeDao
import com.sonbum.diacalendar2.data.local.entity.ShiftInputTypeEntity
import com.sonbum.diacalendar2.domain.model.ShiftInputType
import com.sonbum.diacalendar2.domain.repository.ShiftInputTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShiftInputTypeRepositoryImpl(
    private val shiftInputTypeDao: ShiftInputTypeDao
) : ShiftInputTypeRepository {

    override fun getAllShiftInputTypes(): Flow<List<ShiftInputType>> {
        return shiftInputTypeDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): ShiftInputType? {
        return shiftInputTypeDao.getById(id)?.toDomain()
    }

    override suspend fun insert(type: ShiftInputType): Long {
        return shiftInputTypeDao.insert(type.toEntity())
    }

    override suspend fun insertDefaultTypesIfEmpty() {
        val count = shiftInputTypeDao.getCount()
        if (count == 0) {
            val defaultTypes = ShiftInputType.DEFAULT_TYPES.map { it.toEntity() }
            shiftInputTypeDao.insertAll(defaultTypes)
        }
    }

    override suspend fun update(type: ShiftInputType) {
        shiftInputTypeDao.update(
            id = type.id,
            name = type.name,
            shortName = type.shortName,
            colorHex = type.colorHex,
            requiresLateWork = if (type.requiresLateWork) 1 else 0
        )
    }

    override suspend fun delete(id: Long) {
        shiftInputTypeDao.deleteById(id)
    }

    private fun ShiftInputTypeEntity.toDomain(): ShiftInputType {
        return ShiftInputType(
            id = id,
            name = name,
            shortName = shortName,
            colorHex = colorHex,
            isDefault = isDefault == 1,
            requiresLateWork = requiresLateWork == 1
        )
    }

    private fun ShiftInputType.toEntity(): ShiftInputTypeEntity {
        return ShiftInputTypeEntity(
            id = if (id == 0L) 0 else id,
            name = name,
            shortName = shortName,
            colorHex = colorHex,
            isDefault = if (isDefault) 1 else 0,
            requiresLateWork = if (requiresLateWork) 1 else 0
        )
    }
}
