package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.LocalDiaDao
import com.sonbum.diacalendar2.data.local.mapper.toDomain
import com.sonbum.diacalendar2.data.local.mapper.toEntity
import com.sonbum.diacalendar2.domain.model.LocalDia
import com.sonbum.diacalendar2.domain.repository.LocalDiaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalDiaRepositoryImpl(
    private val localDiaDao: LocalDiaDao
) : LocalDiaRepository {

    override fun getLocalDiasByOfficeId(officeId: Long): Flow<List<LocalDia>> {
        return localDiaDao.getDiasByOfficeId(officeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLocalDiasByOfficeName(officeName: String): Flow<List<LocalDia>> {
        return localDiaDao.getDiasByOfficeName(officeName).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLocalDiaById(id: Long): LocalDia? {
        return localDiaDao.getDiaById(id)?.toDomain()
    }

    override suspend fun getLocalDiaByDiaIdAndOfficeAndType(
        diaId: String,
        officeName: String,
        typeName: String
    ): LocalDia? {
        return localDiaDao.getDiaByDiaIdAndOfficeAndType(diaId, officeName, typeName)?.toDomain()
    }

    override suspend fun getLocalDiaByDiaIdAndOffice(diaId: String, officeName: String): LocalDia? {
        return localDiaDao.getDiaByDiaIdAndOffice(diaId, officeName)?.toDomain()
    }

    override suspend fun insertLocalDia(dia: LocalDia): Long {
        return localDiaDao.insert(dia.toEntity())
    }

    override suspend fun updateLocalDia(dia: LocalDia) {
        localDiaDao.update(dia.toEntity())
    }

    override suspend fun deleteLocalDia(id: Long) {
        localDiaDao.deleteById(id)
    }

    override suspend fun deleteLocalDiasByOfficeId(officeId: Long) {
        localDiaDao.deleteByOfficeId(officeId)
    }

    override suspend fun getLocalDiaCountByOfficeId(officeId: Long): Int {
        return localDiaDao.getCountByOfficeId(officeId)
    }
}
