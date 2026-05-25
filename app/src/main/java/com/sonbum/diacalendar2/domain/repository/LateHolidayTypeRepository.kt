package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.LateHolidayType
import kotlinx.coroutines.flow.Flow

interface LateHolidayTypeRepository {
    fun getAllLateHolidayTypes(): Flow<List<LateHolidayType>>
    suspend fun addLateHolidayType(name: String, shortName: String)
    suspend fun insert(type: LateHolidayType): Long
    suspend fun updateLateHolidayType(type: LateHolidayType)
    suspend fun deleteLateHolidayType(id: Long)
}
