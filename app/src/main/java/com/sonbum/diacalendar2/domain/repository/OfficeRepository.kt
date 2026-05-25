package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.Office
import kotlinx.coroutines.flow.Flow

interface OfficeRepository {
    fun getAllOffices(): Flow<List<Office>>
    suspend fun getOfficeByCode(officeCode: Long): Office?
    suspend fun getOfficeByName(officeName: String): Office?
    suspend fun refreshOffices(): Result<Int>
    suspend fun getOfficeCount(): Int
    suspend fun updateOffice(office: Office)
    suspend fun restoreEditedOffices(): Int
    suspend fun getEditBackupCount(): Int
    suspend fun clearEditBackups()
}
