package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.LateWorkTypeDao
import com.sonbum.diacalendar2.data.local.entity.LateWorkTypeEntity
import com.sonbum.diacalendar2.domain.model.LateWorkType
import com.sonbum.diacalendar2.domain.repository.LateWorkTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LateWorkTypeRepositoryImpl(
    private val dao: LateWorkTypeDao
) : LateWorkTypeRepository {

    override fun getAllLateWorkTypes(): Flow<List<LateWorkType>> {
        return dao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addLateWorkType(name: String, shortName: String) {
        dao.insert(
            LateWorkTypeEntity(
                name = name,
                shortName = shortName
            )
        )
    }

    override suspend fun insert(type: LateWorkType): Long {
        return dao.insert(
            LateWorkTypeEntity(
                name = type.name,
                shortName = type.shortName,
                isDefault = if (type.isDefault) 1 else 0
            )
        )
    }

    override suspend fun updateLateWorkType(type: LateWorkType) {
        dao.update(type.id, type.name, type.shortName)
    }

    override suspend fun deleteLateWorkType(id: Long) {
        dao.deleteById(id)
    }

    private fun LateWorkTypeEntity.toDomain(): LateWorkType {
        return LateWorkType(
            id = id,
            name = name,
            shortName = shortName,
            isDefault = isDefault == 1
        )
    }
}
