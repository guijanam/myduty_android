package com.sonbum.diacalendar2.domain.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBackupDataCompatibilityTest {
    @Test
    fun `version 2 backup without birthday fields keeps safe defaults`() {
        val oldBackup = Json { ignoreUnknownKeys = true }.decodeFromString<AppBackupData>(
            """{"version":2,"createdAt":123,"anniversaries":[]}"""
        )

        assertEquals(2, oldBackup.version)
        assertTrue(oldBackup.birthdayPeople.isEmpty())
        assertTrue(oldBackup.birthdayGroups.isEmpty())
        assertTrue(oldBackup.birthdayMilestones.isEmpty())
    }
}
