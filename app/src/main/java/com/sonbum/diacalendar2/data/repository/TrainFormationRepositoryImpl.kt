package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.TrainFormationDao
import com.sonbum.diacalendar2.data.local.entity.TrainFormationEntity
import com.sonbum.diacalendar2.domain.model.TrainFormation
import com.sonbum.diacalendar2.domain.model.TrainHalf
import com.sonbum.diacalendar2.domain.repository.TrainFormationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrainFormationRepositoryImpl(
    private val trainFormationDao: TrainFormationDao
) : TrainFormationRepository {

    override fun observeByDate(date: String): Flow<List<TrainFormation>> =
        trainFormationDao.observeByDate(date).map { list -> list.map { it.toDomain() } }

    override fun getAll(): Flow<List<TrainFormation>> =
        trainFormationDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun add(formation: TrainFormation) {
        trainFormationDao.insert(formation.toEntity())
    }

    override suspend fun update(formation: TrainFormation) {
        trainFormationDao.update(formation.toEntity())
    }

    override suspend fun delete(id: Long) {
        trainFormationDao.deleteById(id)
    }

    override suspend fun nextSortOrder(date: String, half: TrainHalf): Int =
        trainFormationDao.getByDateAndHalf(date, half.name).size

    private fun TrainFormationEntity.toDomain(): TrainFormation = TrainFormation(
        id = id,
        date = date,
        half = TrainHalf.from(half),
        formationNo = formationNo,
        note = note,
        shiftName = shiftName,
        numTr = numTr,
        sortOrder = sortOrder,
        createdAt = createdAt
    )

    private fun TrainFormation.toEntity(): TrainFormationEntity = TrainFormationEntity(
        id = id,
        date = date,
        half = half.name,
        formationNo = formationNo,
        note = note,
        shiftName = shiftName,
        numTr = numTr,
        sortOrder = sortOrder,
        createdAt = createdAt
    )
}
