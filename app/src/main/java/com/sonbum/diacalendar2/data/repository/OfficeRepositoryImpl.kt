package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.OfficeDao
import com.sonbum.diacalendar2.data.local.dao.OfficeEditBackupDao
import com.sonbum.diacalendar2.data.local.entity.OfficeEditBackupEntity
import com.sonbum.diacalendar2.data.local.mapper.toDomain
import com.sonbum.diacalendar2.data.local.mapper.toEntity
import com.sonbum.diacalendar2.data.remote.SupabaseConfig
import com.sonbum.diacalendar2.data.remote.api.SupabaseApi
import com.sonbum.diacalendar2.domain.model.Office
import com.sonbum.diacalendar2.domain.repository.OfficeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfficeRepositoryImpl(
    private val officeDao: OfficeDao,
    private val supabaseApi: SupabaseApi,
    private val officeEditBackupDao: OfficeEditBackupDao
) : OfficeRepository {

    override fun getAllOffices(): Flow<List<Office>> {
        return officeDao.getAllOffices().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getOfficeByCode(officeCode: Long): Office? {
        return officeDao.getOfficeByCode(officeCode)?.toDomain()
    }

    override suspend fun getOfficeByName(officeName: String): Office? {
        return officeDao.getOfficeByName(officeName)?.toDomain()
    }

    override suspend fun refreshOffices(): Result<Int> {
        return try {
            val apiKey = SupabaseConfig.apiKey
            val offices = supabaseApi.getOffices(
                apiKey = apiKey,
                authorization = "Bearer $apiKey"
            )

            val entities = offices.map { it.toEntity() }

            // deleteAll()을 하면 DiaEntity의 ForeignKey(CASCADE)로 dias 테이블이
            // 함께 삭제되므로, REPLACE 전략으로 upsert만 수행한다.
            // (서버에서 삭제된 승무소는 로컬에 남지만, dias 보존이 우선)
            officeDao.insertAll(entities)
            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOfficeCount(): Int {
        return officeDao.getCount()
    }

    override suspend fun updateOffice(office: Office) {
        val entity = office.toEntity()
        officeDao.update(entity)
        // 백업 테이블에도 저장 (서버 동기화 시 복원용)
        officeEditBackupDao.insert(
            OfficeEditBackupEntity(
                officeCode = entity.officeCode,
                officeName = entity.officeName,
                diaTurns1 = entity.diaTurns1,
                diaTurns2 = entity.diaTurns2,
                subTurns = entity.subTurns,
                diaSelects = entity.diaSelects,
                diaTurns3 = entity.diaTurns3,
                adminPassword = entity.adminPassword,
                backupTimestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun restoreEditedOffices(): Int {
        val backups = officeEditBackupDao.getAll()
        backups.forEach { backup ->
            officeDao.insert(
                com.sonbum.diacalendar2.data.local.entity.OfficeEntity(
                    officeCode = backup.officeCode,
                    officeName = backup.officeName,
                    diaTurns1 = backup.diaTurns1,
                    diaTurns2 = backup.diaTurns2,
                    subTurns = backup.subTurns,
                    diaSelects = backup.diaSelects,
                    diaTurns3 = backup.diaTurns3,
                    adminPassword = backup.adminPassword
                )
            )
        }
        return backups.size
    }

    override suspend fun getEditBackupCount(): Int {
        return officeEditBackupDao.getCount()
    }

    override suspend fun clearEditBackups() {
        officeEditBackupDao.deleteAll()
    }
}
