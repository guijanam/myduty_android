package com.sonbum.diacalendar2.data.repository

import androidx.room.withTransaction
import com.sonbum.diacalendar2.data.local.dao.BirthdayDao
import com.sonbum.diacalendar2.data.local.database.AppDatabase
import com.sonbum.diacalendar2.data.local.entity.BirthdayGroupEntity
import com.sonbum.diacalendar2.data.local.entity.BirthdayMilestoneEntity
import com.sonbum.diacalendar2.data.local.entity.BirthdayPersonEntity
import com.sonbum.diacalendar2.data.local.entity.BirthdayPersonGroupEntity
import com.sonbum.diacalendar2.domain.model.AgeDisplayMode
import com.sonbum.diacalendar2.domain.model.CalendarType
import com.sonbum.diacalendar2.domain.model.BirthdayGroup
import com.sonbum.diacalendar2.domain.model.BirthdayMilestone
import com.sonbum.diacalendar2.domain.model.BirthdayPerson
import com.sonbum.diacalendar2.domain.model.Feb29Policy
import com.sonbum.diacalendar2.domain.model.LeapMonthPolicy
import com.sonbum.diacalendar2.domain.model.MilestoneRuleType
import com.sonbum.diacalendar2.domain.repository.BirthdayRepository
import com.sonbum.diacalendar2.domain.util.BirthdayDateResolver
import com.sonbum.diacalendar2.domain.util.MilestoneCalculator
import com.sonbum.diacalendar2.domain.util.occurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File

class BirthdayRepositoryImpl(
    private val db: AppDatabase,
    private val dao: BirthdayDao,
    private val dateResolver: BirthdayDateResolver,
    private val milestoneCalculator: MilestoneCalculator
) : BirthdayRepository {

    override fun observePeople(): Flow<List<BirthdayPerson>> =
        combine(dao.observePeople(), dao.observePersonGroups()) { people, links ->
            val groupsByPerson = links.groupBy { it.personId }.mapValues { (_, value) -> value.map { it.groupId }.toSet() }
            people.map { it.toDomain(groupsByPerson[it.id].orEmpty()) }
        }

    override fun observeGroups(): Flow<List<BirthdayGroup>> =
        dao.observeGroups().map { groups -> groups.map { it.toDomain() } }

    override fun observeMilestones(personId: Long): Flow<List<BirthdayMilestone>> =
        dao.observeMilestones(personId).map { list -> list.map { it.toDomain() } }

    override suspend fun getPeopleOnce(): List<BirthdayPerson> {
        val links = dao.getPersonGroupsOnce().groupBy { it.personId }
        return dao.getPeopleOnce().map { entity ->
            entity.toDomain(links[entity.id].orEmpty().map { it.groupId }.toSet())
        }
    }

    override suspend fun getPerson(id: Long): BirthdayPerson? {
        val entity = dao.getPerson(id) ?: return null
        val groups = dao.getPersonGroupsOnce().filter { it.personId == id }.map { it.groupId }.toSet()
        return entity.toDomain(groups)
    }

    override suspend fun savePerson(person: BirthdayPerson): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val id = if (person.id == 0L) {
            dao.insertPerson(person.toEntity().copy(createdAt = now, updatedAt = now))
        } else {
            val existing = dao.getPerson(person.id)
            dao.updatePerson(person.toEntity().copy(createdAt = existing?.createdAt ?: person.createdAt, updatedAt = now))
            if (existing?.photoPath != null && existing.photoPath != person.photoPath) {
                runCatching { File(existing.photoPath).delete() }
            }
            person.id
        }
        dao.deleteGroupsForPerson(id)
        dao.insertPersonGroups(person.groupIds.map { BirthdayPersonGroupEntity(id, it) })
        if (person.id == 0L) dao.insertMilestones(defaultMilestones(id))
        id
    }

    override suspend fun deletePerson(id: Long) {
        val photo = dao.getPerson(id)?.photoPath
        dao.deletePerson(id)
        photo?.let { runCatching { File(it).delete() } }
    }

    override suspend fun saveGroup(group: BirthdayGroup): Long {
        return if (group.id == 0L) dao.insertGroup(group.toEntity())
        else {
            dao.updateGroup(group.toEntity())
            group.id
        }
    }

    override suspend fun deleteCustomGroup(id: Long) = dao.deleteCustomGroup(id)

    override suspend fun saveMilestone(milestone: BirthdayMilestone): Long {
        return if (milestone.id == 0L) dao.insertMilestone(milestone.toEntity())
        else {
            dao.updateMilestone(milestone.toEntity())
            milestone.id
        }
    }

    override suspend fun deleteMilestone(id: Long) = dao.deleteMilestone(id)

    override suspend fun getOccurrencesForYear(year: Int) =
        getPeopleOnce().mapNotNull { dateResolver.occurrence(it, year) }.sortedBy { it.date }

    override suspend fun getMilestoneOccurrences() = getPeopleOnce().flatMap { person ->
        dao.getMilestonesOnce(person.id).mapNotNull { milestoneCalculator.occurrence(person, it.toDomain()) }
    }.sortedBy { it.date }

    override suspend fun ensureDefaults() {
        dao.insertGroups(DEFAULT_GROUPS.mapIndexed { index, name ->
            BirthdayGroupEntity(id = (index + 1).toLong(), name = name, isDefault = true, sortOrder = index)
        })
    }

    private fun defaultMilestones(personId: Long): List<BirthdayMilestoneEntity> = listOf(
        defaultMilestone(personId, "백일", MilestoneRuleType.DAYS_AFTER_BIRTH, 100),
        defaultMilestone(personId, "첫돌", MilestoneRuleType.FULL_AGE, 1),
        defaultMilestone(personId, "성년", MilestoneRuleType.FULL_AGE, 19),
        defaultMilestone(personId, "환갑", MilestoneRuleType.FULL_AGE, 60),
        defaultMilestone(personId, "칠순", MilestoneRuleType.FULL_AGE, 70),
        defaultMilestone(personId, "팔순", MilestoneRuleType.FULL_AGE, 80),
        defaultMilestone(personId, "구순", MilestoneRuleType.FULL_AGE, 90)
    )

    private fun defaultMilestone(personId: Long, name: String, type: MilestoneRuleType, value: Int) =
        BirthdayMilestoneEntity(
            personId = personId, name = name, ruleType = type.name, ruleValue = value,
            enabled = true, notificationEnabled = true, isDefault = true
        )

    companion object {
        val DEFAULT_GROUPS = listOf("가족", "친척", "친구", "직장", "학교 동창")
    }
}

private fun BirthdayPersonEntity.toDomain(groupIds: Set<Long>) = BirthdayPerson(
    id = id, name = name, photoPath = photoPath, relationship = relationship,
    birthYear = birthYear, birthMonth = birthMonth, birthDay = birthDay,
    calendarType = enumOrDefault(calendarType, CalendarType.SOLAR),
    isLeapMonth = isLeapMonth, timeZoneId = timeZoneId,
    ageDisplayMode = enumOrDefault(ageDisplayMode, AgeDisplayMode.FULL_AGE),
    leapMonthPolicy = enumOrDefault(leapMonthPolicy, LeapMonthPolicy.REGULAR_SAME_MONTH),
    feb29Policy = enumOrDefault(feb29Policy, Feb29Policy.FEBRUARY_28),
    notificationEnabled = notificationEnabled,
    notificationOffsets = notificationOffsetsCsv.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet(),
    notificationHour = notificationHour, notificationMinute = notificationMinute,
    calendarSyncEnabled = calendarSyncEnabled, groupIds = groupIds,
    createdAt = createdAt, updatedAt = updatedAt
)

private fun BirthdayPerson.toEntity() = BirthdayPersonEntity(
    id = id, name = name, photoPath = photoPath, relationship = relationship,
    birthYear = birthYear, birthMonth = birthMonth, birthDay = birthDay,
    calendarType = calendarType.name, isLeapMonth = isLeapMonth, timeZoneId = timeZoneId,
    ageDisplayMode = ageDisplayMode.name, leapMonthPolicy = leapMonthPolicy.name,
    feb29Policy = feb29Policy.name, notificationEnabled = notificationEnabled,
    notificationOffsetsCsv = notificationOffsets.sortedDescending().joinToString(","),
    notificationHour = notificationHour, notificationMinute = notificationMinute,
    calendarSyncEnabled = calendarSyncEnabled, createdAt = createdAt, updatedAt = updatedAt
)

private fun BirthdayGroupEntity.toDomain() = BirthdayGroup(id, name, isDefault, sortOrder)
private fun BirthdayGroup.toEntity() = BirthdayGroupEntity(id, name, isDefault, sortOrder)

private fun BirthdayMilestoneEntity.toDomain() = BirthdayMilestone(
    id, personId, name, enumOrDefault(ruleType, MilestoneRuleType.FULL_AGE), ruleValue,
    enabled, notificationEnabled, isDefault
)

private fun BirthdayMilestone.toEntity() = BirthdayMilestoneEntity(
    id, personId, name, ruleType.name, ruleValue, enabled, notificationEnabled, isDefault
)

private inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default
