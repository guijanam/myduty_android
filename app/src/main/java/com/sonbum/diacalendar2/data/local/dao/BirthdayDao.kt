package com.sonbum.diacalendar2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sonbum.diacalendar2.data.local.entity.BirthdayGroupEntity
import com.sonbum.diacalendar2.data.local.entity.BirthdayMilestoneEntity
import com.sonbum.diacalendar2.data.local.entity.BirthdayPersonEntity
import com.sonbum.diacalendar2.data.local.entity.BirthdayPersonGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthdayDao {
    @Query("SELECT * FROM birthday_people ORDER BY name COLLATE NOCASE")
    fun observePeople(): Flow<List<BirthdayPersonEntity>>

    @Query("SELECT * FROM birthday_people ORDER BY name COLLATE NOCASE")
    suspend fun getPeopleOnce(): List<BirthdayPersonEntity>

    @Query("SELECT * FROM birthday_people WHERE id = :id")
    suspend fun getPerson(id: Long): BirthdayPersonEntity?

    @Insert
    suspend fun insertPerson(person: BirthdayPersonEntity): Long

    @Update
    suspend fun updatePerson(person: BirthdayPersonEntity)

    @Query("DELETE FROM birthday_people WHERE id = :id")
    suspend fun deletePerson(id: Long)

    @Query("DELETE FROM birthday_people")
    suspend fun deleteAllPeople()

    @Query("SELECT * FROM birthday_groups ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeGroups(): Flow<List<BirthdayGroupEntity>>

    @Query("SELECT * FROM birthday_groups ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getGroupsOnce(): List<BirthdayGroupEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroups(groups: List<BirthdayGroupEntity>)

    @Insert
    suspend fun insertGroup(group: BirthdayGroupEntity): Long

    @Update
    suspend fun updateGroup(group: BirthdayGroupEntity)

    @Query("DELETE FROM birthday_groups WHERE id = :id AND isDefault = 0")
    suspend fun deleteCustomGroup(id: Long)

    @Query("DELETE FROM birthday_groups")
    suspend fun deleteAllGroups()

    @Query("SELECT * FROM birthday_person_groups")
    fun observePersonGroups(): Flow<List<BirthdayPersonGroupEntity>>

    @Query("SELECT * FROM birthday_person_groups")
    suspend fun getPersonGroupsOnce(): List<BirthdayPersonGroupEntity>

    @Query("DELETE FROM birthday_person_groups WHERE personId = :personId")
    suspend fun deleteGroupsForPerson(personId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPersonGroups(links: List<BirthdayPersonGroupEntity>)

    @Query("SELECT * FROM birthday_milestones WHERE personId = :personId ORDER BY ruleType, ruleValue")
    fun observeMilestones(personId: Long): Flow<List<BirthdayMilestoneEntity>>

    @Query("SELECT * FROM birthday_milestones ORDER BY personId, ruleType, ruleValue")
    suspend fun getAllMilestonesOnce(): List<BirthdayMilestoneEntity>

    @Query("SELECT * FROM birthday_milestones WHERE personId = :personId ORDER BY ruleType, ruleValue")
    suspend fun getMilestonesOnce(personId: Long): List<BirthdayMilestoneEntity>

    @Insert
    suspend fun insertMilestone(milestone: BirthdayMilestoneEntity): Long

    @Insert
    suspend fun insertMilestones(milestones: List<BirthdayMilestoneEntity>)

    @Update
    suspend fun updateMilestone(milestone: BirthdayMilestoneEntity)

    @Query("DELETE FROM birthday_milestones WHERE id = :id")
    suspend fun deleteMilestone(id: Long)

    @Query("DELETE FROM birthday_milestones")
    suspend fun deleteAllMilestones()
}
