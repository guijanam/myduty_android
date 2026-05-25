package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.LocalOffice
import kotlinx.coroutines.flow.Flow

interface LocalOfficeRepository {
    fun getAllLocalOffices(): Flow<List<LocalOffice>>
    suspend fun getLocalOfficeById(id: Long): LocalOffice?
    suspend fun getLocalOfficeByName(name: String): LocalOffice?
    suspend fun insertLocalOffice(office: LocalOffice): Long
    suspend fun updateLocalOffice(office: LocalOffice)
    suspend fun deleteLocalOffice(id: Long)
    suspend fun getLocalOfficeCount(): Int
}
