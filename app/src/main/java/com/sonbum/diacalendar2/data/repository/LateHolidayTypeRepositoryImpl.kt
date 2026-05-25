package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.LateHolidayTypeDao
import com.sonbum.diacalendar2.data.local.entity.LateHolidayTypeEntity
import com.sonbum.diacalendar2.domain.model.LateHolidayType
import com.sonbum.diacalendar2.domain.repository.LateHolidayTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LateHolidayTypeRepositoryImpl(
    private val dao: LateHolidayTypeDao
) : LateHolidayTypeRepository {

    override fun getAllLateHolidayTypes(): Flow<List<LateHolidayType>> {
        return dao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addLateHolidayType(name: String, shortName: String) {
        dao.insert(
            LateHolidayTypeEntity(
                name = name,
                shortName = shortName
            )
        )
    }

    override suspend fun insert(type: LateHolidayType): Long {
        return dao.insert(
            LateHolidayTypeEntity(
                name = type.name,
                shortName = type.shortName,
                isDefault = if (type.isDefault) 1 else 0
            )
        )
    }

    override suspend fun updateLateHolidayType(type: LateHolidayType) {
        dao.update(type.id, type.name, type.shortName)
    }

    override suspend fun deleteLateHolidayType(id: Long) {
        dao.deleteById(id)
    }

    private fun LateHolidayTypeEntity.toDomain(): LateHolidayType {
        return LateHolidayType(
            id = id,
            name = name,
            shortName = shortName,
            isDefault = isDefault == 1
        )
    }
}
