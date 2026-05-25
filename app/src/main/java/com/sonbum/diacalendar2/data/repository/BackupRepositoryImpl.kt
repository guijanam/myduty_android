package com.sonbum.diacalendar2.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.sonbum.diacalendar2.data.local.dao.*
import com.sonbum.diacalendar2.data.local.entity.*
import com.sonbum.diacalendar2.domain.model.*
import com.sonbum.diacalendar2.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class BackupRepositoryImpl(
    private val context: Context,
    private val memoDao: MemoDao,
    private val userShiftConfigDao: UserShiftConfigDao,
    private val shiftScheduleDao: ShiftScheduleDao,
    private val vacationTypeDao: VacationTypeDao,
    private val vacationRecordDao: VacationRecordDao,
    private val shiftSwapRecordDao: ShiftSwapRecordDao,
    private val shiftInputTypeDao: ShiftInputTypeDao,
    private val shiftInputRecordDao: ShiftInputRecordDao,
    private val lateWorkTypeDao: LateWorkTypeDao,
    private val lateWorkRecordDao: LateWorkRecordDao,
    private val lateHolidayTypeDao: LateHolidayTypeDao,
    private val localOfficeDao: LocalOfficeDao,
    private val localDiaDao: LocalDiaDao,
    private val lateHolidayRecordDao: LateHolidayRecordDao,
    private val chatNoteDao: ChatNoteDao,
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun collectBackupData(): AppBackupData = withContext(Dispatchers.IO) {
        // UserShiftConfig
        val config = userShiftConfigDao.getConfigOnce()
        val userShiftConfigBackup = config?.let {
            UserShiftConfigBackup(
                officeCode = it.officeCode.toInt(),
                officeName = it.officeName,
                position = it.position,
                shiftPattern = it.shiftPattern.split(",").filter { s -> s.isNotBlank() },
                startDate = it.startDate,
                todayShift = it.todayShift,
                referenceDate = it.referenceDate
            )
        }

        // ShiftSchedules
        val schedules = shiftScheduleDao.getAllSchedulesOnce()
        val shiftScheduleBackups = schedules.map {
            ShiftScheduleBackup(date = it.date, shiftName = it.shiftName)
        }

        // Memos
        val memos = memoDao.getAllMemos().first()
        val memoBackups = memos.map {
            MemoBackup(
                id = it.objectId,
                date = it.dateString,
                title = it.title,
                content = it.content,
                colorHex = it.hexColorString,
                isCompleted = it.isCompleted,
                position = it.position,
                hasTime = it.startTimeString.isNotBlank(),
                hour = parseHour(it.startTimeString),
                minute = parseMinute(it.startTimeString),
                endDate = if (it.endTimeString.isNotBlank() && it.endTimeString != it.dateString) it.endTimeString else null,
                imageBase64 = encodeImageToBase64(it.imagePath)
            )
        }

        // VacationTypes
        val vacationTypes = vacationTypeDao.getAllVacationTypes().first()
        val vacationTypeBackups = vacationTypes.map {
            VacationTypeBackup(id = it.id, name = it.name, shortName = it.shortName, isDefault = it.isDefault)
        }

        // VacationRecords
        val vacationRecords = vacationRecordDao.getAll().first()
        val vacationRecordBackups = vacationRecords.map {
            VacationRecordBackup(
                id = it.id,
                date = it.date,
                vacationTypeId = it.vacationTypeId,
                vacationName = it.vacationName,
                shortName = it.shortName,
                groupId = "" // VacationRecord에는 groupId가 없음
            )
        }

        // ShiftSwapRecords
        val shiftSwapRecords = shiftSwapRecordDao.getAll().first()
        val shiftSwapRecordBackups = shiftSwapRecords.map {
            ShiftSwapRecordBackup(
                id = it.id,
                date = it.date,
                originalShiftName = it.originalShiftName,
                swappedShiftName = it.swappedShiftName,
                groupId = it.groupId
            )
        }

        // ShiftInputTypes
        val shiftInputTypes = shiftInputTypeDao.getAll().first()
        val shiftInputTypeBackups = shiftInputTypes.map {
            ShiftInputTypeBackup(
                id = it.id,
                name = it.name,
                shortName = it.shortName,
                colorHex = it.colorHex,
                requiresLateWork = it.requiresLateWork == 1,
                isDefault = it.isDefault == 1
            )
        }

        // ShiftInputRecords
        val shiftInputRecords = shiftInputRecordDao.getAll().first()
        val shiftInputRecordBackups = shiftInputRecords.map {
            ShiftInputRecordBackup(
                id = it.id,
                date = it.date,
                shiftInputTypeId = it.shiftInputTypeId,
                shortName = it.shortName,
                colorHex = it.colorHex,
                targetShiftName = it.targetShiftName,
                originalShiftName = it.originalShiftName,
                groupId = it.groupId
            )
        }

        // LateWorkTypes
        val lateWorkTypes = lateWorkTypeDao.getAll().first()
        val lateWorkTypeBackups = lateWorkTypes.map {
            LateWorkTypeBackup(id = it.id, name = it.name, shortName = it.shortName, isDefault = it.isDefault == 1)
        }

        // LateWorkRecords
        val lateWorkRecords = lateWorkRecordDao.getAll().first()
        val lateWorkRecordBackups = lateWorkRecords.map {
            LateWorkRecordBackup(
                id = it.id,
                date = it.date,
                lateWorkTypeId = it.lateWorkTypeId,
                lateWorkName = it.lateWorkName,
                shortName = it.shortName,
                groupId = "" // LateWorkRecord에는 groupId가 없음
            )
        }

        // LateHolidayTypes
        val lateHolidayTypes = lateHolidayTypeDao.getAll().first()
        val lateHolidayTypeBackups = lateHolidayTypes.map {
            LateHolidayTypeBackup(id = it.id, name = it.name, shortName = it.shortName, isDefault = it.isDefault == 1)
        }

        // LateHolidayRecords
        val lateHolidayRecords = lateHolidayRecordDao.getAll().first()
        val lateHolidayRecordBackups = lateHolidayRecords.map {
            LateHolidayRecordBackup(
                id = it.id,
                date = it.date,
                lateHolidayTypeId = it.lateHolidayTypeId,
                lateHolidayName = it.lateHolidayName,
                shortName = it.shortName,
                groupId = "" // LateHolidayRecord에는 groupId가 없음
            )
        }

        // LocalOffices
        val localOffices = localOfficeDao.getAllOffices().first()
        val localOfficeBackups = localOffices.map {
            LocalOfficeBackup(
                id = it.id,
                officeName = it.officeName,
                diaTurns1 = it.diaTurns1,
                diaTurns2 = it.diaTurns2,
                subTurns = it.subTurns,
                diaSelects = it.diaSelects,
                diaTurns3 = it.diaTurns3
            )
        }

        // LocalDias
        val localDias = localDiaDao.getAllLocalDias().first()
        val localDiaBackups = localDias.map {
            LocalDiaBackup(
                id = it.id,
                diaId = it.diaId,
                officeName = it.officeName,
                typeName = it.typeName,
                firstTime = it.firstTime,
                numTr1 = it.numTr1,
                numTr2 = it.numTr2,
                secondTime = it.secondTime,
                thirdTime = it.thirdTime,
                totalTime = it.totalTime,
                workTime = it.workTime
            )
        }

        // ChatNotes
        val chatNotes = chatNoteDao.getAllNotes().first()
        val chatNoteBackups = chatNotes.map {
            ChatNoteBackup(
                id = it.id,
                content = it.content,
                createdAt = it.createdAt,
                isPinned = it.isPinned,
                imageBase64 = encodeImageToBase64(it.imagePath)
            )
        }

        AppBackupData(
            userShiftConfig = userShiftConfigBackup,
            shiftSchedules = shiftScheduleBackups,
            memos = memoBackups,
            vacationTypes = vacationTypeBackups,
            vacationRecords = vacationRecordBackups,
            shiftSwapRecords = shiftSwapRecordBackups,
            shiftInputTypes = shiftInputTypeBackups,
            shiftInputRecords = shiftInputRecordBackups,
            lateWorkTypes = lateWorkTypeBackups,
            lateWorkRecords = lateWorkRecordBackups,
            lateHolidayTypes = lateHolidayTypeBackups,
            lateHolidayRecords = lateHolidayRecordBackups,
            localOffices = localOfficeBackups,
            localDias = localDiaBackups,
            chatNotes = chatNoteBackups
        )
    }

    override suspend fun exportToFile(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupData = collectBackupData()
            val jsonString = json.encodeToString(backupData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(Exception("파일을 열 수 없습니다"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readFromFile(uri: Uri): Result<AppBackupData> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return@withContext Result.failure(Exception("파일을 읽을 수 없습니다"))

            val backupData = json.decodeFromString<AppBackupData>(jsonString)
            Result.success(backupData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreFromBackup(backupData: AppBackupData, clearExisting: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var restoredCount = 0

            if (clearExisting) {
                // 기존 데이터 삭제
                memoDao.deleteAllMemos()
                userShiftConfigDao.deleteConfig()
                shiftScheduleDao.deleteAll()
                vacationTypeDao.deleteAll()
                vacationRecordDao.deleteAll()
                shiftSwapRecordDao.deleteAll()
                shiftInputTypeDao.deleteAll()
                shiftInputRecordDao.deleteAll()
                lateWorkTypeDao.deleteAll()
                lateWorkRecordDao.deleteAll()
                lateHolidayTypeDao.deleteAll()
                lateHolidayRecordDao.deleteAll()
                localDiaDao.deleteAll()
                localOfficeDao.deleteAll()
                chatNoteDao.deleteAll()
            }

            // UserShiftConfig 복원
            backupData.userShiftConfig?.let { config ->
                userShiftConfigDao.saveConfig(
                    UserShiftConfigEntity(
                        officeCode = config.officeCode.toLong(),
                        officeName = config.officeName,
                        position = config.position,
                        shiftPattern = config.shiftPattern.joinToString(","),
                        startDate = config.startDate,
                        todayShift = config.todayShift,
                        referenceDate = config.referenceDate
                    )
                )
                restoredCount++
            }

            // ShiftSchedules 복원 (배치로)
            if (backupData.shiftSchedules.isNotEmpty()) {
                val entities = backupData.shiftSchedules.map {
                    ShiftScheduleEntity(date = it.date, shiftName = it.shiftName)
                }
                entities.chunked(1000).forEach { chunk ->
                    shiftScheduleDao.insertAll(chunk)
                }
                restoredCount += backupData.shiftSchedules.size
            }

            // Memos 복원
            backupData.memos.forEach { memo ->
                val startTime = if (memo.hasTime) String.format("%02d:%02d", memo.hour, memo.minute) else ""
                val restoredImagePath = decodeBase64ToImage(memo.imageBase64)
                memoDao.insertMemo(
                    MemoEntity(
                        objectId = memo.id,
                        dateString = memo.date,
                        title = memo.title,
                        content = memo.content,
                        hexColorString = memo.colorHex,
                        isCompleted = memo.isCompleted,
                        position = memo.position,
                        startTimeString = startTime,
                        endTimeString = memo.endDate ?: memo.date,
                        imagePath = restoredImagePath
                    )
                )
                restoredCount++
            }

            // VacationTypes 복원
            backupData.vacationTypes.forEach { type ->
                vacationTypeDao.insert(
                    VacationTypeEntity(
                        name = type.name,
                        shortName = type.shortName,
                        isDefault = type.isDefault
                    )
                )
                restoredCount++
            }

            // VacationRecords 복원
            backupData.vacationRecords.forEach { record ->
                vacationRecordDao.insert(
                    VacationRecordEntity(
                        date = record.date,
                        vacationTypeId = record.vacationTypeId,
                        vacationName = record.vacationName,
                        shortName = record.shortName
                    )
                )
                restoredCount++
            }

            // ShiftSwapRecords 복원
            backupData.shiftSwapRecords.forEach { record ->
                shiftSwapRecordDao.insert(
                    ShiftSwapRecordEntity(
                        date = record.date,
                        originalShiftName = record.originalShiftName,
                        swappedShiftName = record.swappedShiftName,
                        groupId = record.groupId
                    )
                )
                restoredCount++
            }

            // ShiftInputTypes 복원
            backupData.shiftInputTypes.forEach { type ->
                shiftInputTypeDao.insert(
                    ShiftInputTypeEntity(
                        name = type.name,
                        shortName = type.shortName,
                        colorHex = type.colorHex,
                        requiresLateWork = if (type.requiresLateWork) 1 else 0,
                        isDefault = if (type.isDefault) 1 else 0
                    )
                )
                restoredCount++
            }

            // ShiftInputRecords 복원
            backupData.shiftInputRecords.forEach { record ->
                shiftInputRecordDao.insert(
                    ShiftInputRecordEntity(
                        date = record.date,
                        shiftInputTypeId = record.shiftInputTypeId,
                        shiftInputTypeName = record.shortName,
                        shortName = record.shortName,
                        colorHex = record.colorHex,
                        targetShiftName = record.targetShiftName,
                        originalShiftName = record.originalShiftName,
                        groupId = record.groupId
                    )
                )
                restoredCount++
            }

            // LateWorkTypes 복원
            backupData.lateWorkTypes.forEach { type ->
                lateWorkTypeDao.insert(
                    LateWorkTypeEntity(
                        name = type.name,
                        shortName = type.shortName,
                        isDefault = if (type.isDefault) 1 else 0
                    )
                )
                restoredCount++
            }

            // LateWorkRecords 복원
            backupData.lateWorkRecords.forEach { record ->
                lateWorkRecordDao.insert(
                    LateWorkRecordEntity(
                        date = record.date,
                        lateWorkTypeId = record.lateWorkTypeId,
                        lateWorkName = record.lateWorkName,
                        shortName = record.shortName
                    )
                )
                restoredCount++
            }

            // LateHolidayTypes 복원
            backupData.lateHolidayTypes.forEach { type ->
                lateHolidayTypeDao.insert(
                    LateHolidayTypeEntity(
                        name = type.name,
                        shortName = type.shortName,
                        isDefault = if (type.isDefault) 1 else 0
                    )
                )
                restoredCount++
            }

            // LateHolidayRecords 복원
            backupData.lateHolidayRecords.forEach { record ->
                lateHolidayRecordDao.insert(
                    LateHolidayRecordEntity(
                        date = record.date,
                        lateHolidayTypeId = record.lateHolidayTypeId,
                        lateHolidayName = record.lateHolidayName,
                        shortName = record.shortName
                    )
                )
                restoredCount++
            }

            // LocalOffices 및 LocalDias 복원
            val oldToNewOfficeIdMap = mutableMapOf<Long, Long>()
            backupData.localOffices.forEach { office ->
                val newId = localOfficeDao.insert(
                    LocalOfficeEntity(
                        officeName = office.officeName,
                        diaTurns1 = office.diaTurns1,
                        diaTurns2 = office.diaTurns2,
                        subTurns = office.subTurns,
                        diaSelects = office.diaSelects,
                        diaTurns3 = office.diaTurns3
                    )
                )
                oldToNewOfficeIdMap[office.id] = newId
                restoredCount++
            }

            backupData.localDias.forEach { dia ->
                // officeName으로 localOfficeId 찾기
                val localOffice = localOfficeDao.getOfficeByName(dia.officeName)
                if (localOffice != null) {
                    localDiaDao.insert(
                        LocalDiaEntity(
                            diaId = dia.diaId,
                            localOfficeId = localOffice.id,
                            officeName = dia.officeName,
                            typeName = dia.typeName,
                            firstTime = dia.firstTime,
                            numTr1 = dia.numTr1,
                            numTr2 = dia.numTr2,
                            secondTime = dia.secondTime,
                            thirdTime = dia.thirdTime,
                            totalTime = dia.totalTime,
                            workTime = dia.workTime
                        )
                    )
                    restoredCount++
                }
            }

            // ChatNotes 복원
            backupData.chatNotes.forEach { note ->
                val restoredImagePath = decodeBase64ToImage(note.imageBase64)
                chatNoteDao.insert(
                    ChatNoteEntity(
                        id = note.id,
                        content = note.content,
                        createdAt = note.createdAt,
                        isPinned = note.isPinned,
                        imagePath = restoredImagePath
                    )
                )
                restoredCount++
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 이미지 파일을 Base64 문자열로 인코딩
     */
    private fun encodeImageToBase64(imagePath: String?): String? {
        if (imagePath == null) return null
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                val bytes = file.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Base64 문자열을 이미지 파일로 디코딩하여 내부 저장소에 저장
     */
    private fun decodeBase64ToImage(base64: String?): String? {
        if (base64 == null) return null
        return try {
            val imagesDir = File(context.filesDir, "memo_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            val fileName = "memo_${UUID.randomUUID()}.jpg"
            val destFile = File(imagesDir, fileName)

            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            destFile.writeBytes(bytes)

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseHour(timeString: String): Int {
        return try {
            if (timeString.contains(":")) {
                timeString.split(":")[0].toInt()
            } else 0
        } catch (e: Exception) { 0 }
    }

    private fun parseMinute(timeString: String): Int {
        return try {
            if (timeString.contains(":")) {
                timeString.split(":")[1].toInt()
            } else 0
        } catch (e: Exception) { 0 }
    }
}
