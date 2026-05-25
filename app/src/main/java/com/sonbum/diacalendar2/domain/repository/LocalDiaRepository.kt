package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.LocalDia
import kotlinx.coroutines.flow.Flow

interface LocalDiaRepository {
    fun getLocalDiasByOfficeId(officeId: Long): Flow<List<LocalDia>>
    fun getLocalDiasByOfficeName(officeName: String): Flow<List<LocalDia>>
    suspend fun getLocalDiaById(id: Long): LocalDia?
    suspend fun getLocalDiaByDiaIdAndOfficeAndType(diaId: String, officeName: String, typeName: String): LocalDia?
    suspend fun getLocalDiaByDiaIdAndOffice(diaId: String, officeName: String): LocalDia?
    suspend fun insertLocalDia(dia: LocalDia): Long
    suspend fun updateLocalDia(dia: LocalDia)
    suspend fun deleteLocalDia(id: Long)
    suspend fun deleteLocalDiasByOfficeId(officeId: Long)
    suspend fun getLocalDiaCountByOfficeId(officeId: Long): Int
}
