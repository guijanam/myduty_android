package com.sonbum.diacalendar2.domain.model

import kotlinx.serialization.Serializable

// ===== .diacal 파일 백업용 (기존) =====

@Serializable
data class DiacalBackup(
    val version: Int = 1,
    val exportedAt: String,
    val offices: List<BackupOffice>
)

@Serializable
data class BackupOffice(
    val officeName: String,
    val diaTurns1: String? = null,
    val diaTurns2: String? = null,
    val subTurns: String? = null,
    val diaSelects: String? = null,
    val diaTurns3: String? = null,  // 운휴 근무 (공휴일/일요일 포함 시 휴무 계산에 사용)
    val createdAt: Long = 0,
    val dias: List<BackupDia> = emptyList()
)

@Serializable
data class BackupDia(
    val diaId: String,
    val typeName: String? = null,
    val firstTime: String? = null,
    val numTr1: String? = null,
    val numTr2: String? = null,
    val secondTime: String? = null,
    val thirdTime: String? = null,
    val totalTime: String? = null,
    val workTime: String? = null
)

// ===== 전체 앱 데이터 백업용 =====

/**
 * 앱 데이터 백업을 위한 통합 데이터 클래스
 */
@Serializable
data class AppBackupData(
    val version: Int = CURRENT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val userShiftConfig: UserShiftConfigBackup? = null,
    val shiftSchedules: List<ShiftScheduleBackup> = emptyList(),
    val memos: List<MemoBackup> = emptyList(),
    val vacationRecords: List<VacationRecordBackup> = emptyList(),
    val vacationTypes: List<VacationTypeBackup> = emptyList(),
    val shiftSwapRecords: List<ShiftSwapRecordBackup> = emptyList(),
    val shiftInputRecords: List<ShiftInputRecordBackup> = emptyList(),
    val shiftInputTypes: List<ShiftInputTypeBackup> = emptyList(),
    val lateWorkRecords: List<LateWorkRecordBackup> = emptyList(),
    val lateWorkTypes: List<LateWorkTypeBackup> = emptyList(),
    val lateHolidayRecords: List<LateHolidayRecordBackup> = emptyList(),
    val lateHolidayTypes: List<LateHolidayTypeBackup> = emptyList(),
    val localOffices: List<LocalOfficeBackup> = emptyList(),
    val localDias: List<LocalDiaBackup> = emptyList(),
    val chatNotes: List<ChatNoteBackup> = emptyList(),
    val anniversaries: List<AnniversaryBackup> = emptyList(),
    val coworkerGroups: List<CoworkerGroupBackup> = emptyList(),
    val coworkers: List<CoworkerBackup> = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 2
        const val FILE_EXTENSION = "dcbackup"
    }
}

@Serializable
data class UserShiftConfigBackup(
    val officeCode: Int,
    val officeName: String,
    val position: String,
    val shiftPattern: List<String>,
    val startDate: String,
    val todayShift: String,
    val referenceDate: String
)

@Serializable
data class ShiftScheduleBackup(
    val date: String,
    val shiftName: String
)

@Serializable
data class MemoBackup(
    val id: String,
    val date: String,
    val title: String,
    val content: String,
    val colorHex: String,
    val isCompleted: Boolean,
    val position: Long,
    val hasTime: Boolean,
    val hour: Int,
    val minute: Int,
    val endDate: String?,
    val imageBase64: String? = null
)

@Serializable
data class VacationRecordBackup(
    val id: Long,
    val date: String,
    val vacationTypeId: Long,
    val vacationName: String,
    val shortName: String,
    val groupId: String
)

@Serializable
data class VacationTypeBackup(
    val id: Long,
    val name: String,
    val shortName: String,
    val isDefault: Boolean
)

@Serializable
data class ShiftSwapRecordBackup(
    val id: Long,
    val date: String,
    val originalShiftName: String,
    val swappedShiftName: String,
    val groupId: String
)

@Serializable
data class ShiftInputRecordBackup(
    val id: Long,
    val date: String,
    val shiftInputTypeId: Long,
    val shortName: String,
    val colorHex: String,
    val targetShiftName: String,
    val originalShiftName: String,
    val groupId: String
)

@Serializable
data class ShiftInputTypeBackup(
    val id: Long,
    val name: String,
    val shortName: String,
    val colorHex: String,
    val requiresLateWork: Boolean,
    val isDefault: Boolean
)

@Serializable
data class LateWorkRecordBackup(
    val id: Long,
    val date: String,
    val lateWorkTypeId: Long,
    val lateWorkName: String,
    val shortName: String,
    val groupId: String
)

@Serializable
data class LateWorkTypeBackup(
    val id: Long,
    val name: String,
    val shortName: String,
    val isDefault: Boolean
)

@Serializable
data class LateHolidayRecordBackup(
    val id: Long,
    val date: String,
    val lateHolidayTypeId: Long,
    val lateHolidayName: String,
    val shortName: String,
    val groupId: String
)

@Serializable
data class LateHolidayTypeBackup(
    val id: Long,
    val name: String,
    val shortName: String,
    val isDefault: Boolean
)

@Serializable
data class LocalOfficeBackup(
    val id: Long,
    val officeName: String,
    val diaTurns1: String?,
    val diaTurns2: String?,
    val subTurns: String?,
    val diaSelects: String?,
    val diaTurns3: String? = null  // 운휴 근무 (공휴일/일요일 포함 시 휴무 계산에 사용)
)

@Serializable
data class LocalDiaBackup(
    val id: Long,
    val diaId: String,
    val officeName: String,
    val typeName: String?,
    val firstTime: String?,
    val numTr1: String?,
    val numTr2: String?,
    val secondTime: String?,
    val thirdTime: String?,
    val totalTime: String?,
    val workTime: String?
)

@Serializable
data class ChatNoteBackup(
    val id: String,
    val content: String,
    val createdAt: String,
    val isPinned: Boolean = false,
    val imageBase64: String? = null
)

@Serializable
data class AnniversaryBackup(
    val id: Long,
    val name: String,
    val month: Int,
    val day: Int,
    val isLunar: Boolean,
    val createdAt: Long
)

@Serializable
data class CoworkerGroupBackup(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long
)

@Serializable
data class CoworkerBackup(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val groupIds: String,
    val shiftPattern: String,
    val referenceDate: String,
    val referenceShift: String,
    val referenceShiftIndex: Int? = null,
    val createdAt: Long
)
