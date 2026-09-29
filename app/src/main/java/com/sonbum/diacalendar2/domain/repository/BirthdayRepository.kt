package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.BirthdayGroup
import com.sonbum.diacalendar2.domain.model.BirthdayMilestone
import com.sonbum.diacalendar2.domain.model.BirthdayOccurrence
import com.sonbum.diacalendar2.domain.model.BirthdayPerson
import com.sonbum.diacalendar2.domain.model.MilestoneOccurrence
import kotlinx.coroutines.flow.Flow

interface BirthdayRepository {
    fun observePeople(): Flow<List<BirthdayPerson>>
    fun observeGroups(): Flow<List<BirthdayGroup>>
    fun observeMilestones(personId: Long): Flow<List<BirthdayMilestone>>
    suspend fun getPeopleOnce(): List<BirthdayPerson>
    suspend fun getPerson(id: Long): BirthdayPerson?
    suspend fun savePerson(person: BirthdayPerson): Long
    suspend fun deletePerson(id: Long)
    suspend fun saveGroup(group: BirthdayGroup): Long
    suspend fun deleteCustomGroup(id: Long)
    suspend fun saveMilestone(milestone: BirthdayMilestone): Long
    suspend fun deleteMilestone(id: Long)
    suspend fun getOccurrencesForYear(year: Int): List<BirthdayOccurrence>
    suspend fun getMilestoneOccurrences(): List<MilestoneOccurrence>
    suspend fun ensureDefaults()
}
