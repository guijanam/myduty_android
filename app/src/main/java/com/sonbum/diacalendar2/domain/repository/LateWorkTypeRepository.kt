package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.LateWorkType
import kotlinx.coroutines.flow.Flow

interface LateWorkTypeRepository {
    fun getAllLateWorkTypes(): Flow<List<LateWorkType>>
    suspend fun addLateWorkType(name: String, shortName: String)
    suspend fun insert(type: LateWorkType): Long
    suspend fun updateLateWorkType(type: LateWorkType)
    suspend fun deleteLateWorkType(id: Long)
}
