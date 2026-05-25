package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.Dia
import kotlinx.coroutines.flow.Flow

interface DiaRepository {
    fun getAllDias(): Flow<List<Dia>>
    fun getDiasByOfficeId(officeId: Int): Flow<List<Dia>>
    fun getDiasByOfficeName(officeName: String): Flow<List<Dia>>
    suspend fun getDiaById(id: Long): Dia?
    suspend fun getDiaByDiaIdAndOffice(diaId: String, officeName: String): Dia?
    suspend fun getDiaByDiaIdAndOfficeAndType(diaId: String, officeName: String, typeName: String): Dia?
    suspend fun refreshDias(): Result<Int>
    suspend fun refreshDiasByOfficeId(officeId: Int): Result<Int>
    suspend fun getDiaCount(): Int
    suspend fun getDiaCountByOfficeId(officeId: Int): Int
    suspend fun deleteAllDias()
    suspend fun updateDia(dia: Dia)
    suspend fun restoreEditedDias(): Int
    suspend fun getDiaEditBackupCount(): Int
    suspend fun clearDiaEditBackups()
}
