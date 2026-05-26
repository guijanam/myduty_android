package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.VacationType
import kotlinx.coroutines.flow.Flow

interface VacationTypeRepository {
    fun getAllVacationTypes(): Flow<List<VacationType>>
    suspend fun addVacationType(name: String, shortName: String, annualQuota: Int = 0, resetMonthDay: String = "01-01"): Long
    suspend fun deleteVacationType(id: Long)
    suspend fun updateShortName(id: Long, shortName: String)
    suspend fun updateVacationType(id: Long, name: String, shortName: String, annualQuota: Int = 0, resetMonthDay: String = "01-01")
    suspend fun ensureDefaultsExist()
}
