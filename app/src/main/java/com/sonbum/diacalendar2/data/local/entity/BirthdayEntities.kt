package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "birthday_people")
data class BirthdayPersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val photoPath: String? = null,
    val relationship: String = "",
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val calendarType: String,
    val isLeapMonth: Boolean = false,
    val timeZoneId: String,
    val ageDisplayMode: String,
    val leapMonthPolicy: String,
    val feb29Policy: String,
    val notificationEnabled: Boolean = true,
    val notificationOffsetsCsv: String = "30,7,3,1,0",
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val calendarSyncEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "birthday_groups",
    indices = [Index(value = ["name"], unique = true)]
)
data class BirthdayGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "birthday_person_groups",
    primaryKeys = ["personId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = BirthdayPersonEntity::class,
            parentColumns = ["id"], childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BirthdayGroupEntity::class,
            parentColumns = ["id"], childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId"), Index("groupId")]
)
data class BirthdayPersonGroupEntity(
    val personId: Long,
    val groupId: Long
)

@Entity(
    tableName = "birthday_milestones",
    foreignKeys = [
        ForeignKey(
            entity = BirthdayPersonEntity::class,
            parentColumns = ["id"], childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId")]
)
data class BirthdayMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val name: String,
    val ruleType: String,
    val ruleValue: Int,
    val enabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val isDefault: Boolean = false
)
