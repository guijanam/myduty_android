package com.sonbum.diacalendar2.data.repository

import androidx.room.withTransaction
import com.sonbum.diacalendar2.data.local.dao.CoworkerDao
import com.sonbum.diacalendar2.data.local.dao.CoworkerGroupDao
import com.sonbum.diacalendar2.data.local.database.AppDatabase
import com.sonbum.diacalendar2.data.local.entity.CoworkerEntity
import com.sonbum.diacalendar2.data.local.entity.CoworkerGroupEntity
import com.sonbum.diacalendar2.domain.model.Coworker
import com.sonbum.diacalendar2.domain.model.CoworkerGroup
import com.sonbum.diacalendar2.domain.repository.CoworkerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CoworkerRepositoryImpl(
    private val coworkerDao: CoworkerDao,
    private val coworkerGroupDao: CoworkerGroupDao,
    private val db: AppDatabase
) : CoworkerRepository {

    // ─── Coworker ──────────────────────────────────────────────────────────

    override fun getAllCoworkers(): Flow<List<Coworker>> =
        coworkerDao.getAllCoworkers().map { list -> list.map { it.toDomain() } }

    override suspend fun getCoworkerById(id: Long): Coworker? =
        coworkerDao.getById(id)?.toDomain()

    override suspend fun saveCoworker(coworker: Coworker): Long {
        val entity = coworker.toEntity()
        return if (entity.id == 0L) {
            coworkerDao.insert(entity)
        } else {
            coworkerDao.update(entity)
            entity.id
        }
    }

    override suspend fun deleteCoworker(id: Long) {
        coworkerDao.deleteById(id)
    }

    override suspend fun updateCoworkerSortOrders(ordered: List<Coworker>) {
        db.withTransaction {
            ordered.forEachIndexed { index, coworker ->
                coworkerDao.updateSortOrder(coworker.id, index)
            }
        }
    }

    // ─── Group ─────────────────────────────────────────────────────────────

    override fun getAllGroups(): Flow<List<CoworkerGroup>> =
        coworkerGroupDao.getAllGroups().map { list -> list.map { it.toDomain() } }

    override suspend fun saveGroup(group: CoworkerGroup): Long {
        val entity = group.toEntity()
        return if (entity.id == 0L) {
            coworkerGroupDao.insert(entity)
        } else {
            coworkerGroupDao.update(entity)
            entity.id
        }
    }

    override suspend fun updateGroup(group: CoworkerGroup) {
        coworkerGroupDao.update(group.toEntity())
    }

    override suspend fun deleteGroup(id: Long) {
        // 1. 그룹 삭제
        coworkerGroupDao.deleteById(id)
        // 2. 해당 그룹 ID를 참조하는 동료들의 groupIds에서 제거
        val all = coworkerDao.getAllOnce()
        all.forEach { entity ->
            val ids = entity.groupIds.parseIds().filter { it != id }
            if (ids.size != entity.groupIds.parseIds().size) {
                coworkerDao.update(entity.copy(groupIds = ids.joinToString(",")))
            }
        }
    }

    // ─── Schedule calculation ───────────────────────────────────────────────

    override fun calculateScheduleForMonth(
        coworker: Coworker,
        year: Int,
        month: Int
    ): Map<LocalDate, String> {
        val pattern = coworker.shiftPattern
        val refDate = coworker.referenceDate ?: return emptyMap()
        if (pattern.isEmpty()) return emptyMap()

        val patternSize = pattern.size
        val refShiftIndex = coworker.referenceShiftIndex
            ?: pattern.indexOf(coworker.referenceShift).takeIf { it >= 0 }
            ?: return emptyMap()

        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())

        val result = mutableMapOf<LocalDate, String>()
        var current = firstDay
        while (!current.isAfter(lastDay)) {
            val daysFromRef = ChronoUnit.DAYS.between(refDate, current).toInt()
            val idx = ((refShiftIndex + daysFromRef) % patternSize + patternSize) % patternSize
            result[current] = pattern[idx]
            current = current.plusDays(1)
        }
        return result
    }

    // ─── Mappers ────────────────────────────────────────────────────────────

    private fun CoworkerEntity.toDomain() = Coworker(
        id = id,
        name = name,
        sortOrder = sortOrder,
        groupIds = groupIds.parseIds(),
        shiftPattern = shiftPattern.parsePattern(),
        referenceDate = referenceDate.toLocalDateOrNull(),
        referenceShift = referenceShift,
        referenceShiftIndex = referenceShiftIndex,
        createdAt = createdAt
    )

    private fun Coworker.toEntity() = CoworkerEntity(
        id = id,
        name = name,
        sortOrder = sortOrder,
        groupIds = groupIds.joinToString(","),
        shiftPattern = shiftPattern.joinToString(","),
        referenceDate = referenceDate?.toString() ?: "",
        referenceShift = referenceShift,
        referenceShiftIndex = referenceShiftIndex,
        createdAt = createdAt
    )

    private fun CoworkerGroupEntity.toDomain() = CoworkerGroup(
        id = id,
        name = name,
        sortOrder = sortOrder,
        createdAt = createdAt
    )

    private fun CoworkerGroup.toEntity() = CoworkerGroupEntity(
        id = id,
        name = name,
        sortOrder = sortOrder,
        createdAt = createdAt
    )

    private fun String.parseIds(): List<Long> =
        if (isBlank()) emptyList()
        else split(",").mapNotNull { it.trim().toLongOrNull() }

    private fun String.parsePattern(): List<String> =
        if (isBlank()) emptyList()
        else split(",").map { it.trim() }.filter { it.isNotEmpty() }

    private fun String.toLocalDateOrNull(): LocalDate? =
        if (isBlank()) null
        else try { LocalDate.parse(this) } catch (e: Exception) { null }
}
