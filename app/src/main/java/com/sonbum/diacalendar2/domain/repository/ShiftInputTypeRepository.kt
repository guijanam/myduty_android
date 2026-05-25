package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.ShiftInputType
import kotlinx.coroutines.flow.Flow

interface ShiftInputTypeRepository {
    fun getAllShiftInputTypes(): Flow<List<ShiftInputType>>
    suspend fun getById(id: Long): ShiftInputType?
    suspend fun insert(type: ShiftInputType): Long
    suspend fun insertDefaultTypesIfEmpty()
    suspend fun update(type: ShiftInputType)
    suspend fun delete(id: Long)
}
