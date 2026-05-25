package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.LocalOfficeDao
import com.sonbum.diacalendar2.data.local.mapper.toDomain
import com.sonbum.diacalendar2.data.local.mapper.toEntity
import com.sonbum.diacalendar2.domain.model.LocalOffice
import com.sonbum.diacalendar2.domain.repository.LocalOfficeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalOfficeRepositoryImpl(
    private val localOfficeDao: LocalOfficeDao
) : LocalOfficeRepository {

    override fun getAllLocalOffices(): Flow<List<LocalOffice>> {
        return localOfficeDao.getAllLocalOffices().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLocalOfficeById(id: Long): LocalOffice? {
        return localOfficeDao.getLocalOfficeById(id)?.toDomain()
    }

    override suspend fun getLocalOfficeByName(name: String): LocalOffice? {
        return localOfficeDao.getLocalOfficeByName(name)?.toDomain()
    }

    override suspend fun insertLocalOffice(office: LocalOffice): Long {
        return localOfficeDao.insert(office.toEntity())
    }

    override suspend fun updateLocalOffice(office: LocalOffice) {
        localOfficeDao.update(office.toEntity())
    }

    override suspend fun deleteLocalOffice(id: Long) {
        localOfficeDao.deleteById(id)
    }

    override suspend fun getLocalOfficeCount(): Int {
        return localOfficeDao.getCount()
    }
}
