package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.TrainFormation
import com.sonbum.diacalendar2.domain.model.TrainHalf
import kotlinx.coroutines.flow.Flow

interface TrainFormationRepository {
    fun observeByDate(date: String): Flow<List<TrainFormation>>
    fun getAll(): Flow<List<TrainFormation>>
    suspend fun add(formation: TrainFormation)
    suspend fun update(formation: TrainFormation)
    suspend fun delete(id: Long)

    /** 같은 (date, half) 내 다음 sortOrder 값 */
    suspend fun nextSortOrder(date: String, half: TrainHalf): Int
}
