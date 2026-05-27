package com.sonbum.diacalendar2.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sonbum.diacalendar2.data.local.dao.DiaDao
import com.sonbum.diacalendar2.data.local.dao.HolidayDao
import com.sonbum.diacalendar2.data.local.dao.LocalDiaDao
import com.sonbum.diacalendar2.data.local.dao.LocalOfficeDao
import com.sonbum.diacalendar2.data.local.dao.MemoDao
import com.sonbum.diacalendar2.data.local.dao.OfficeDao
import com.sonbum.diacalendar2.data.local.dao.ShiftScheduleDao
import com.sonbum.diacalendar2.data.local.dao.ShiftSwapRecordDao
import com.sonbum.diacalendar2.data.local.dao.UserShiftConfigDao
import com.sonbum.diacalendar2.data.local.dao.VacationRecordDao
import com.sonbum.diacalendar2.data.local.dao.VacationTypeDao
import com.sonbum.diacalendar2.data.local.dao.LateWorkRecordDao
import com.sonbum.diacalendar2.data.local.dao.LateWorkTypeDao
import com.sonbum.diacalendar2.data.local.dao.LateHolidayRecordDao
import com.sonbum.diacalendar2.data.local.dao.LateHolidayTypeDao
import com.sonbum.diacalendar2.data.local.dao.ShiftInputTypeDao
import com.sonbum.diacalendar2.data.local.dao.ShiftInputRecordDao
import com.sonbum.diacalendar2.data.local.dao.ChatNoteDao
import com.sonbum.diacalendar2.data.local.entity.DiaEntity
import com.sonbum.diacalendar2.data.local.entity.HolidayEntity
import com.sonbum.diacalendar2.data.local.entity.LocalDiaEntity
import com.sonbum.diacalendar2.data.local.entity.LocalOfficeEntity
import com.sonbum.diacalendar2.data.local.entity.MemoEntity
import com.sonbum.diacalendar2.data.local.entity.OfficeEntity
import com.sonbum.diacalendar2.data.local.entity.ShiftScheduleEntity
import com.sonbum.diacalendar2.data.local.entity.ShiftSwapRecordEntity
import com.sonbum.diacalendar2.data.local.entity.UserShiftConfigEntity
import com.sonbum.diacalendar2.data.local.entity.VacationRecordEntity
import com.sonbum.diacalendar2.data.local.entity.VacationTypeEntity
import com.sonbum.diacalendar2.data.local.entity.LateWorkRecordEntity
import com.sonbum.diacalendar2.data.local.entity.LateWorkTypeEntity
import com.sonbum.diacalendar2.data.local.entity.LateHolidayRecordEntity
import com.sonbum.diacalendar2.data.local.entity.LateHolidayTypeEntity
import com.sonbum.diacalendar2.data.local.entity.ShiftInputTypeEntity
import com.sonbum.diacalendar2.data.local.entity.ShiftInputRecordEntity
import com.sonbum.diacalendar2.data.local.entity.ChatNoteEntity
import com.sonbum.diacalendar2.data.local.dao.CustomShiftDao
import com.sonbum.diacalendar2.data.local.dao.OfficeEditBackupDao
import com.sonbum.diacalendar2.data.local.dao.DiaEditBackupDao
import com.sonbum.diacalendar2.data.local.dao.CoworkerDao
import com.sonbum.diacalendar2.data.local.dao.CoworkerGroupDao
import com.sonbum.diacalendar2.data.local.entity.CustomShiftEntity
import com.sonbum.diacalendar2.data.local.entity.OfficeEditBackupEntity
import com.sonbum.diacalendar2.data.local.entity.DiaEditBackupEntity
import com.sonbum.diacalendar2.data.local.entity.CoworkerEntity
import com.sonbum.diacalendar2.data.local.entity.CoworkerGroupEntity
import com.sonbum.diacalendar2.data.local.entity.AnniversaryEntity
import com.sonbum.diacalendar2.data.local.dao.AnniversaryDao

@Database(
    entities = [
        MemoEntity::class,
        HolidayEntity::class,
        OfficeEntity::class,
        DiaEntity::class,
        UserShiftConfigEntity::class,
        ShiftScheduleEntity::class,
        VacationTypeEntity::class,
        VacationRecordEntity::class,
        LocalOfficeEntity::class,
        LocalDiaEntity::class,
        ShiftSwapRecordEntity::class,
        LateWorkRecordEntity::class,
        LateWorkTypeEntity::class,
        LateHolidayRecordEntity::class,
        LateHolidayTypeEntity::class,
        ShiftInputTypeEntity::class,
        ShiftInputRecordEntity::class,
        ChatNoteEntity::class,
        CustomShiftEntity::class,
        OfficeEditBackupEntity::class,
        DiaEditBackupEntity::class,
        CoworkerEntity::class,
        CoworkerGroupEntity::class,
        AnniversaryEntity::class
    ],
    version = 26,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun holidayDao(): HolidayDao
    abstract fun officeDao(): OfficeDao
    abstract fun diaDao(): DiaDao
    abstract fun userShiftConfigDao(): UserShiftConfigDao
    abstract fun shiftScheduleDao(): ShiftScheduleDao
    abstract fun vacationTypeDao(): VacationTypeDao
    abstract fun vacationRecordDao(): VacationRecordDao
    abstract fun localOfficeDao(): LocalOfficeDao
    abstract fun localDiaDao(): LocalDiaDao
    abstract fun shiftSwapRecordDao(): ShiftSwapRecordDao
    abstract fun lateWorkRecordDao(): LateWorkRecordDao
    abstract fun lateWorkTypeDao(): LateWorkTypeDao
    abstract fun lateHolidayRecordDao(): LateHolidayRecordDao
    abstract fun lateHolidayTypeDao(): LateHolidayTypeDao
    abstract fun shiftInputTypeDao(): ShiftInputTypeDao
    abstract fun shiftInputRecordDao(): ShiftInputRecordDao
    abstract fun chatNoteDao(): ChatNoteDao
    abstract fun customShiftDao(): CustomShiftDao
    abstract fun officeEditBackupDao(): OfficeEditBackupDao
    abstract fun diaEditBackupDao(): DiaEditBackupDao
    abstract fun coworkerDao(): CoworkerDao
    abstract fun coworkerGroupDao(): CoworkerGroupDao
    abstract fun anniversaryDao(): AnniversaryDao

    companion object {
        // 버전 2 → 3: holidays 테이블에 isUserCreated 컬럼 추가
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE holidays ADD COLUMN isUserCreated INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 버전 3 → 4: offices, dias 테이블 추가
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // offices 테이블 생성
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS offices (
                        officeCode INTEGER PRIMARY KEY NOT NULL,
                        officeName TEXT NOT NULL,
                        diaTurns1 TEXT,
                        diaTurns2 TEXT,
                        subTurns TEXT,
                        diaSelects TEXT,
                        diaTurns3 TEXT,
                        adminPassword TEXT
                    )
                """)

                // dias 테이블 생성
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS dias (
                        id INTEGER PRIMARY KEY NOT NULL,
                        diaId TEXT NOT NULL,
                        officeName TEXT NOT NULL,
                        officeId INTEGER,
                        typeName TEXT,
                        firstTime TEXT,
                        numTr1 TEXT,
                        numTr2 TEXT,
                        secondTime TEXT,
                        thirdTime TEXT,
                        totalTime TEXT,
                        workTime TEXT,
                        FOREIGN KEY (officeId) REFERENCES offices(officeCode) ON DELETE CASCADE
                    )
                """)

                // dias 테이블 인덱스 생성
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dias_officeId ON dias(officeId)")
            }
        }

        // 버전 4 → 5: user_shift_config, shift_schedules 테이블 추가
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // user_shift_config 테이블 생성
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_shift_config (
                        id INTEGER PRIMARY KEY NOT NULL,
                        officeCode INTEGER NOT NULL,
                        officeName TEXT NOT NULL,
                        position TEXT NOT NULL,
                        shiftPattern TEXT NOT NULL,
                        startDate TEXT NOT NULL,
                        todayShift TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)

                // shift_schedules 테이블 생성
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shift_schedules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        shiftName TEXT NOT NULL
                    )
                """)

                // shift_schedules 테이블 인덱스 생성
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shift_schedules_date ON shift_schedules(date)")
            }
        }

        // 버전 5 → 6: user_shift_config에 referenceDate 컬럼 추가
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_shift_config ADD COLUMN referenceDate TEXT NOT NULL DEFAULT ''")
            }
        }

        // 버전 6 → 7: vacation_types 테이블 추가
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS vacation_types (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        shortName TEXT NOT NULL DEFAULT '',
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        // 버전 7 → 8: vacation_records 테이블 추가
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS vacation_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        vacationTypeId INTEGER NOT NULL,
                        vacationName TEXT NOT NULL,
                        shortName TEXT NOT NULL
                    )
                """)
            }
        }

        // 버전 8 → 9: local_offices, local_dias 테이블 추가
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS local_offices (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        officeName TEXT NOT NULL,
                        diaTurns1 TEXT,
                        diaTurns2 TEXT,
                        subTurns TEXT,
                        diaSelects TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS local_dias (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        diaId TEXT NOT NULL,
                        localOfficeId INTEGER NOT NULL,
                        officeName TEXT NOT NULL,
                        typeName TEXT,
                        firstTime TEXT,
                        numTr1 TEXT,
                        numTr2 TEXT,
                        secondTime TEXT,
                        thirdTime TEXT,
                        totalTime TEXT,
                        workTime TEXT,
                        FOREIGN KEY (localOfficeId) REFERENCES local_offices(id) ON DELETE CASCADE
                    )
                """)

                db.execSQL("CREATE INDEX IF NOT EXISTS index_local_dias_localOfficeId ON local_dias(localOfficeId)")
            }
        }

        // 버전 9 → 10: shift_swap_records 테이블 추가
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shift_swap_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        originalShiftName TEXT NOT NULL,
                        swappedShiftName TEXT NOT NULL,
                        groupId TEXT NOT NULL
                    )
                """)
            }
        }

        // 버전 10 → 11: LateWork/LateHoliday 관련 테이블 4개 추가
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // late_work_types
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS late_work_types (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        shortName TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                """)
                // late_work_records
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS late_work_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        lateWorkTypeId INTEGER NOT NULL,
                        lateWorkName TEXT NOT NULL,
                        shortName TEXT NOT NULL
                    )
                """)
                // late_holiday_types
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS late_holiday_types (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        shortName TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                """)
                // late_holiday_records
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS late_holiday_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        lateHolidayTypeId INTEGER NOT NULL,
                        lateHolidayName TEXT NOT NULL,
                        shortName TEXT NOT NULL
                    )
                """)
            }
        }

        // 버전 11 → 12: ShiftInput 관련 테이블 2개 추가 (충당 기능)
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // shift_input_types (충당 종류: 대기충당, 휴무충당, 지근충당)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shift_input_types (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        shortName TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        requiresLateWork INTEGER NOT NULL DEFAULT 0
                    )
                """)
                // shift_input_records (충당 기록)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shift_input_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        shiftInputTypeId INTEGER NOT NULL,
                        shiftInputTypeName TEXT NOT NULL,
                        shortName TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        targetShiftName TEXT NOT NULL,
                        originalShiftName TEXT NOT NULL,
                        groupId TEXT NOT NULL
                    )
                """)
            }
        }

        // 버전 12 → 13: local_offices 테이블에 diaTurns3 컬럼 추가 (운휴 근무)
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_offices ADD COLUMN diaTurns3 TEXT")
            }
        }

        // 버전 13 → 14: chat_notes 테이블 추가 (개인 채팅 메모)
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_notes (
                        id TEXT PRIMARY KEY NOT NULL,
                        content TEXT NOT NULL,
                        createdAt TEXT NOT NULL,
                        isPinned INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        // 버전 14 → 15: memos 테이블에 알림 관련 컬럼 추가
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memos ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE memos ADD COLUMN reminderTimeMillis INTEGER")
            }
        }

        // 버전 16 → 17: memos 테이블에 imagePath 컬럼 추가
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memos ADD COLUMN imagePath TEXT")
            }
        }

        // 버전 17 → 18: chat_notes 테이블에 imagePath 컬럼 추가
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_notes ADD COLUMN imagePath TEXT")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_shifts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shiftName TEXT NOT NULL,
                        shiftPattern TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // 버전 19 → 20: user_shift_config에 todayShiftIndex 컬럼 추가 (중복 근무명 구분용)
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_shift_config ADD COLUMN todayShiftIndex INTEGER")
            }
        }

        // 버전 20 → 21: memos 테이블에 isAllDay 컬럼 추가
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memos ADD COLUMN isAllDay INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 버전 21 → 22: 동료근무 테이블 추가 (coworker_groups, coworkers)
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS coworker_groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS coworkers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        groupIds TEXT NOT NULL DEFAULT '',
                        shiftPattern TEXT NOT NULL DEFAULT '',
                        referenceDate TEXT NOT NULL DEFAULT '',
                        referenceShift TEXT NOT NULL DEFAULT '',
                        referenceShiftIndex INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        // 버전 22 → 23: vacation_types에 annualQuota, resetMonthDay 컬럼 추가
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vacation_types ADD COLUMN annualQuota INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vacation_types ADD COLUMN resetMonthDay TEXT NOT NULL DEFAULT '01-01'")
            }
        }

        // 버전 23 → 24: anniversaries 테이블 추가
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS anniversaries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        month INTEGER NOT NULL,
                        day INTEGER NOT NULL,
                        isLunar INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // 버전 24 → 25: vacation_types에 grantYear, expiryYear 컬럼 추가
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vacation_types ADD COLUMN grantYear INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vacation_types ADD COLUMN expiryYear INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 버전 25 → 26: vacation_types에 grantDate, expiryDate 컬럼 추가 (월/일 포함 날짜)
        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vacation_types ADD COLUMN grantDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vacation_types ADD COLUMN expiryDate TEXT NOT NULL DEFAULT ''")
            }
        }

        // 버전 18 → 19: 서버 근무표 편집 백업 테이블 추가
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS office_edit_backups (
                        officeCode INTEGER NOT NULL PRIMARY KEY,
                        officeName TEXT NOT NULL,
                        diaTurns1 TEXT,
                        diaTurns2 TEXT,
                        subTurns TEXT,
                        diaSelects TEXT,
                        diaTurns3 TEXT,
                        adminPassword TEXT,
                        backupTimestamp INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS dia_edit_backups (
                        id INTEGER NOT NULL PRIMARY KEY,
                        diaId TEXT NOT NULL,
                        officeName TEXT NOT NULL,
                        officeId INTEGER,
                        typeName TEXT,
                        firstTime TEXT,
                        numTr1 TEXT,
                        numTr2 TEXT,
                        secondTime TEXT,
                        thirdTime TEXT,
                        totalTime TEXT,
                        workTime TEXT,
                        backupTimestamp INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}
