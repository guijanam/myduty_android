package com.sonbum.diacalendar2.data.repository

import android.util.Log
import com.sonbum.diacalendar2.data.local.dao.ShiftScheduleDao
import com.sonbum.diacalendar2.data.local.dao.UserShiftConfigDao
import com.sonbum.diacalendar2.data.local.entity.ShiftScheduleEntity
import com.sonbum.diacalendar2.data.local.mapper.toDomain
import com.sonbum.diacalendar2.data.local.mapper.toEntity
import com.sonbum.diacalendar2.domain.model.ShiftSchedule
import com.sonbum.diacalendar2.domain.model.UserShiftConfig
import com.sonbum.diacalendar2.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "ShiftRepository"

class ShiftRepositoryImpl(
    private val userShiftConfigDao: UserShiftConfigDao,
    private val shiftScheduleDao: ShiftScheduleDao
) : ShiftRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // ===== 사용자 설정 =====

    override fun getUserConfig(): Flow<UserShiftConfig?> {
        return userShiftConfigDao.getConfig().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getUserConfigOnce(): UserShiftConfig? {
        return userShiftConfigDao.getConfigOnce()?.toDomain()
    }

    override suspend fun saveUserConfig(config: UserShiftConfig) {
        Log.d(TAG, "saveUserConfig() - 사용자 설정 저장: ${config.officeName}, ${config.position.displayName}")
        userShiftConfigDao.saveConfig(config.toEntity())
    }

    override suspend fun deleteUserConfig() {
        Log.d(TAG, "deleteUserConfig() - 사용자 설정 삭제")
        userShiftConfigDao.deleteConfig()
    }

    // ===== 근무 스케줄 =====

    override fun getAllSchedules(): Flow<List<ShiftSchedule>> {
        return shiftScheduleDao.getAllSchedules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSchedulesByMonth(year: Int, month: Int): Flow<List<ShiftSchedule>> {
        val yearMonth = String.format("%04d-%02d", year, month)
        return shiftScheduleDao.getSchedulesByMonth(yearMonth).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getScheduleMap(): Flow<Map<LocalDate, String>> {
        return shiftScheduleDao.getAllSchedules().map { entities ->
            entities.associate {
                LocalDate.parse(it.date, dateFormatter) to it.shiftName
            }
        }
    }

    override suspend fun getScheduleByDate(date: LocalDate): ShiftSchedule? {
        return shiftScheduleDao.getScheduleByDate(date.format(dateFormatter))?.toDomain()
    }

    override fun observeScheduleByDate(date: LocalDate): Flow<ShiftSchedule?> {
        return shiftScheduleDao.observeScheduleByDate(date.format(dateFormatter))
            .map { it?.toDomain() }
    }

    override suspend fun generateAndSaveSchedules(
        shiftPattern: List<String>,
        startDate: LocalDate,
        todayShift: String,
        referenceDate: LocalDate,
        years: Int,
        todayShiftIndex: Int?
    ): Int {
        Log.d(TAG, "========================================")
        Log.d(TAG, "generateAndSaveSchedules() - 근무 스케줄 생성 시작")
        Log.d(TAG, "교번 패턴: $shiftPattern")
        Log.d(TAG, "시작 날짜: $startDate")
        Log.d(TAG, "기준 근무: $todayShift")
        Log.d(TAG, "기준 날짜: $referenceDate")
        Log.d(TAG, "생성 기간: ${years}년")
        Log.d(TAG, "========================================")

        if (shiftPattern.isEmpty()) {
            Log.e(TAG, "교번 패턴이 비어있습니다!")
            return 0
        }

        // 기준 근무의 인덱스 찾기 (todayShiftIndex가 있으면 그대로 사용, 없으면 indexOf 폴백)
        val refShiftIndex = todayShiftIndex ?: shiftPattern.indexOf(todayShift)
        if (refShiftIndex == -1 || refShiftIndex >= shiftPattern.size) {
            Log.e(TAG, "기준 근무($todayShift, index=$todayShiftIndex)를 패턴에서 찾을 수 없습니다!")
            return 0
        }

        Log.d(TAG, "기준 근무 인덱스: $refShiftIndex")

        // 시작 날짜 이후 기존 스케줄만 삭제 (이전 스케줄은 보존)
        val startDateStr = startDate.format(dateFormatter)
        val existingCount = shiftScheduleDao.getCount()
        Log.d(TAG, "기존 스케줄 총 ${existingCount}개 중 $startDateStr 이후 삭제")
        shiftScheduleDao.deleteFromDate(startDateStr)

        // referenceDate에 todayShift가 오도록 정렬하여 startDate부터 생성
        // referenceDate와 startDate의 차이를 계산하여 시작 오프셋 결정
        val daysFromRefToStart = java.time.temporal.ChronoUnit.DAYS.between(referenceDate, startDate).toInt()

        val endDate = startDate.plusYears(years.toLong())
        val schedules = mutableListOf<ShiftScheduleEntity>()
        var currentDate = startDate
        var dayOffset = 0

        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            // referenceDate 기준으로 순환 인덱스 계산
            // referenceDate에 refShiftIndex가 오도록 하고, startDate와의 차이만큼 오프셋
            val totalOffset = daysFromRefToStart + dayOffset
            val patternSize = shiftPattern.size
            val shiftIndex = ((refShiftIndex + totalOffset) % patternSize + patternSize) % patternSize
            val shiftName = shiftPattern[shiftIndex]

            schedules.add(
                ShiftScheduleEntity(
                    date = currentDate.format(dateFormatter),
                    shiftName = shiftName
                )
            )

            currentDate = currentDate.plusDays(1)
            dayOffset++
        }

        // 배치 저장 (1000개씩)
        schedules.chunked(1000).forEach { chunk ->
            shiftScheduleDao.insertAll(chunk)
        }

        val totalCount = shiftScheduleDao.getCount()
        Log.d(TAG, "========================================")
        Log.d(TAG, "스케줄 생성 완료! 총 ${totalCount}개")
        Log.d(TAG, "========================================")

        // 샘플 로그 (처음 7일)
        Log.d(TAG, "=== 생성된 스케줄 샘플 (처음 7일) ===")
        schedules.take(7).forEachIndexed { index, schedule ->
            Log.d(TAG, "[$index] ${schedule.date}: ${schedule.shiftName}")
        }

        return totalCount
    }

    override suspend fun deleteAllSchedules() {
        val count = shiftScheduleDao.getCount()
        Log.d(TAG, "deleteAllSchedules() - 스케줄 삭제 (${count}개)")
        shiftScheduleDao.deleteAll()
    }

    override suspend fun deleteSchedulesFromDate(fromDate: LocalDate) {
        val fromDateStr = fromDate.format(dateFormatter)
        Log.d(TAG, "deleteSchedulesFromDate() - $fromDateStr 이후 스케줄 삭제")
        shiftScheduleDao.deleteFromDate(fromDateStr)
    }

    override suspend fun getScheduleCount(): Int {
        return shiftScheduleDao.getCount()
    }
}
