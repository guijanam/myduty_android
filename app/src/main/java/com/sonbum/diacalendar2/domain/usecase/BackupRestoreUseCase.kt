package com.sonbum.diacalendar2.domain.usecase

import com.sonbum.diacalendar2.domain.model.BackupDia
import com.sonbum.diacalendar2.domain.model.BackupOffice
import com.sonbum.diacalendar2.domain.model.DiacalBackup
import com.sonbum.diacalendar2.domain.model.LocalDia
import com.sonbum.diacalendar2.domain.model.LocalOffice
import com.sonbum.diacalendar2.domain.repository.LocalDiaRepository
import com.sonbum.diacalendar2.domain.repository.LocalOfficeRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ImportResult(
    val importedOfficeCount: Int,
    val importedDiaCount: Int,
    val renamedOffices: List<String>
)

class BackupRestoreUseCase(
    private val localOfficeRepository: LocalOfficeRepository,
    private val localDiaRepository: LocalDiaRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToStream(outputStream: OutputStream) {
        val offices = localOfficeRepository.getAllLocalOffices().first()

        val backupOffices = offices.map { office ->
            val dias = localDiaRepository.getLocalDiasByOfficeId(office.id).first()
            BackupOffice(
                officeName = office.officeName,
                diaTurns1 = office.diaTurns1,
                diaTurns2 = office.diaTurns2,
                subTurns = office.subTurns,
                diaSelects = office.diaSelects,
                diaTurns3 = office.diaTurns3,
                createdAt = office.createdAt,
                dias = dias.map { dia ->
                    BackupDia(
                        diaId = dia.diaId,
                        typeName = dia.typeName,
                        firstTime = dia.firstTime,
                        numTr1 = dia.numTr1,
                        numTr2 = dia.numTr2,
                        secondTime = dia.secondTime,
                        thirdTime = dia.thirdTime,
                        totalTime = dia.totalTime,
                        workTime = dia.workTime
                    )
                }
            )
        }

        val backup = DiacalBackup(
            version = 1,
            exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            offices = backupOffices
        )

        val jsonString = json.encodeToString(DiacalBackup.serializer(), backup)
        outputStream.bufferedWriter().use { it.write(jsonString) }
    }

    suspend fun importFromStream(inputStream: InputStream): ImportResult {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val backup = json.decodeFromString(DiacalBackup.serializer(), jsonString)

        var importedOfficeCount = 0
        var importedDiaCount = 0
        val renamedOffices = mutableListOf<String>()

        for (backupOffice in backup.offices) {
            val resolvedName = resolveOfficeName(backupOffice.officeName)

            if (resolvedName != backupOffice.officeName) {
                renamedOffices.add("${backupOffice.officeName} → $resolvedName")
            }

            val newOfficeId = localOfficeRepository.insertLocalOffice(
                LocalOffice(
                    id = 0,
                    officeName = resolvedName,
                    diaTurns1 = backupOffice.diaTurns1,
                    diaTurns2 = backupOffice.diaTurns2,
                    subTurns = backupOffice.subTurns,
                    diaSelects = backupOffice.diaSelects,
                    diaTurns3 = backupOffice.diaTurns3,
                    createdAt = backupOffice.createdAt
                )
            )
            importedOfficeCount++

            for (backupDia in backupOffice.dias) {
                localDiaRepository.insertLocalDia(
                    LocalDia(
                        id = 0,
                        diaId = backupDia.diaId,
                        localOfficeId = newOfficeId,
                        officeName = resolvedName,
                        typeName = backupDia.typeName,
                        firstTime = backupDia.firstTime,
                        numTr1 = backupDia.numTr1,
                        numTr2 = backupDia.numTr2,
                        secondTime = backupDia.secondTime,
                        thirdTime = backupDia.thirdTime,
                        totalTime = backupDia.totalTime,
                        workTime = backupDia.workTime
                    )
                )
                importedDiaCount++
            }
        }

        return ImportResult(
            importedOfficeCount = importedOfficeCount,
            importedDiaCount = importedDiaCount,
            renamedOffices = renamedOffices
        )
    }

    private suspend fun resolveOfficeName(originalName: String): String {
        if (localOfficeRepository.getLocalOfficeByName(originalName) == null) {
            return originalName
        }
        var suffix = 2
        while (true) {
            val candidate = "$originalName ($suffix)"
            if (localOfficeRepository.getLocalOfficeByName(candidate) == null) {
                return candidate
            }
            suffix++
        }
    }
}
