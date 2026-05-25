package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.CustomShift
import kotlinx.coroutines.flow.Flow

interface CustomShiftRepository {
    fun getAllCustomShifts(): Flow<List<CustomShift>>
    suspend fun getCustomShiftById(id: Long): CustomShift?
    suspend fun insertCustomShift(shift: CustomShift): Long
    suspend fun updateCustomShift(shift: CustomShift)
    suspend fun deleteCustomShift(id: Long)
}
