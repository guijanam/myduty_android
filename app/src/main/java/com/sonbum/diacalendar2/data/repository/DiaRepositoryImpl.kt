package com.sonbum.diacalendar2.data.repository

import android.util.Log
import com.sonbum.diacalendar2.data.local.dao.DiaDao
import com.sonbum.diacalendar2.data.local.dao.DiaEditBackupDao
import com.sonbum.diacalendar2.data.local.entity.DiaEditBackupEntity
import com.sonbum.diacalendar2.data.local.entity.DiaEntity
import com.sonbum.diacalendar2.data.local.mapper.toDomain
import com.sonbum.diacalendar2.data.local.mapper.toEntity
import com.sonbum.diacalendar2.data.remote.SupabaseConfig
import com.sonbum.diacalendar2.data.remote.api.SupabaseApi
import com.sonbum.diacalendar2.domain.model.Dia
import com.sonbum.diacalendar2.domain.repository.DiaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "DiaRepository"

class DiaRepositoryImpl(
    private val diaDao: DiaDao,
    private val supabaseApi: SupabaseApi,
    private val diaEditBackupDao: DiaEditBackupDao
) : DiaRepository {

    override fun getAllDias(): Flow<List<Dia>> {
        return diaDao.getAllDias().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDiasByOfficeId(officeId: Int): Flow<List<Dia>> {
        return diaDao.getDiasByOfficeId(officeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDiasByOfficeName(officeName: String): Flow<List<Dia>> {
        return diaDao.getDiasByOfficeName(officeName).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDiaById(id: Long): Dia? {
        return diaDao.getDiaById(id)?.toDomain()
    }

    override suspend fun getDiaByDiaIdAndOffice(diaId: String, officeName: String): Dia? {
        return diaDao.getDiaByDiaIdAndOffice(diaId, officeName)?.toDomain()
    }

    override suspend fun getDiaByDiaIdAndOfficeAndType(diaId: String, officeName: String, typeName: String): Dia? {
        return diaDao.getDiaByDiaIdAndOfficeAndType(diaId, officeName, typeName)?.toDomain()
    }

    override suspend fun refreshDias(): Result<Int> {
        return try {
            val apiKey = SupabaseConfig.apiKey
            val dias = supabaseApi.getDias(
                apiKey = apiKey,
                authorization = "Bearer $apiKey"
            )

            val entities = dias.map { it.toEntity() }

            diaDao.deleteAll()
            diaDao.insertAll(entities)
            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshDiasByOfficeId(officeId: Int): Result<Int> {
        return try {
            Log.d(TAG, "refreshDiasByOfficeId() - 승무소 ID: $officeId 근무표 조회 시작")

            val apiKey = SupabaseConfig.apiKey
            val dias = supabaseApi.getDiasByOfficeId(
                apiKey = apiKey,
                authorization = "Bearer $apiKey",
                officeIdFilter = "eq.$officeId"
            )

            Log.d(TAG, "refreshDiasByOfficeId() - 서버에서 ${dias.size}개 근무표 조회 완료")

            val entities = dias.map { it.toEntity() }

            // 기존 모든 근무표 삭제 (마지막 선택된 승무소 데이터만 유지)
            val countBefore = diaDao.getCount()
            Log.d(TAG, "refreshDiasByOfficeId() - 기존 근무표 삭제 시작 (현재 $countBefore 개)")
            diaDao.deleteAll()
            Log.d(TAG, "refreshDiasByOfficeId() - 기존 근무표 모두 삭제 완료")

            // 새로 가져온 데이터 삽입
            diaDao.insertAll(entities)
            val countAfter = diaDao.getCount()
            Log.d(TAG, "refreshDiasByOfficeId() - 새 근무표 저장 완료 (총 $countAfter 개)")

            // 저장된 데이터 샘플 로그
            if (entities.isNotEmpty()) {
                Log.d(TAG, "=== 저장된 근무표 샘플 (처음 5개) ===")
                entities.take(5).forEachIndexed { index, entity ->
                    Log.d(TAG, "[$index] diaId: ${entity.diaId}, typeName: ${entity.typeName}, " +
                            "firstTime: ${entity.firstTime}, workTime: ${entity.workTime}")
                }
            }

            Result.success(entities.size)
        } catch (e: Exception) {
            Log.e(TAG, "refreshDiasByOfficeId() - 오류 발생: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getDiaCount(): Int {
        return diaDao.getCount()
    }

    override suspend fun getDiaCountByOfficeId(officeId: Int): Int {
        return diaDao.getCountByOfficeId(officeId)
    }

    override suspend fun deleteAllDias() {
        val countBefore = diaDao.getCount()
        Log.d(TAG, "deleteAllDias() - 삭제 전 근무표 개수: $countBefore")
        diaDao.deleteAll()
        Log.d(TAG, "deleteAllDias() - 모든 근무표 삭제 완료")
    }

    override suspend fun updateDia(dia: Dia) {
        val entity = dia.toEntity()
        diaDao.update(entity)
        // 백업 테이블에도 저장 (서버 동기화 시 복원용)
        diaEditBackupDao.insert(
            DiaEditBackupEntity(
                id = entity.id,
                diaId = entity.diaId,
                officeName = entity.officeName,
                officeId = entity.officeId,
                typeName = entity.typeName,
                firstTime = entity.firstTime,
                numTr1 = entity.numTr1,
                numTr2 = entity.numTr2,
                secondTime = entity.secondTime,
                thirdTime = entity.thirdTime,
                totalTime = entity.totalTime,
                workTime = entity.workTime,
                backupTimestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun restoreEditedDias(): Int {
        val backups = diaEditBackupDao.getAll()
        backups.forEach { backup ->
            diaDao.insert(
                DiaEntity(
                    id = backup.id,
                    diaId = backup.diaId,
                    officeName = backup.officeName,
                    officeId = backup.officeId,
                    typeName = backup.typeName,
                    firstTime = backup.firstTime,
                    numTr1 = backup.numTr1,
                    numTr2 = backup.numTr2,
                    secondTime = backup.secondTime,
                    thirdTime = backup.thirdTime,
                    totalTime = backup.totalTime,
                    workTime = backup.workTime
                )
            )
        }
        return backups.size
    }

    override suspend fun getDiaEditBackupCount(): Int {
        return diaEditBackupDao.getCount()
    }

    override suspend fun clearDiaEditBackups() {
        diaEditBackupDao.deleteAll()
    }
}
