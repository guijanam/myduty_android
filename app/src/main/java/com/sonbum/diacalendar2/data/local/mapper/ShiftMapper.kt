package com.sonbum.diacalendar2.data.local.mapper

import com.sonbum.diacalendar2.data.local.entity.ShiftScheduleEntity
import com.sonbum.diacalendar2.data.local.entity.SubShiftConfigEntity
import com.sonbum.diacalendar2.data.local.entity.UserShiftConfigEntity
import com.sonbum.diacalendar2.domain.model.ShiftSchedule
import com.sonbum.diacalendar2.domain.model.UserShiftConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

// UserShiftConfig 변환
fun UserShiftConfigEntity.toDomain(): UserShiftConfig {
    val position = when (this.position) {
        "기관사" -> UserShiftConfig.Position.ENGINEER
        "차장" -> UserShiftConfig.Position.CONDUCTOR
        "4조2교대" -> UserShiftConfig.Position.FOUR_SHIFT
        else -> UserShiftConfig.Position.ENGINEER
    }

    return UserShiftConfig(
        officeCode = officeCode,
        officeName = officeName,
        position = position,
        shiftPattern = shiftPattern.split(",").map { it.trim() },
        startDate = LocalDate.parse(startDate, dateFormatter),
        todayShift = todayShift,
        todayShiftIndex = todayShiftIndex,
        referenceDate = if (referenceDate.isNotBlank()) LocalDate.parse(referenceDate, dateFormatter) else LocalDate.parse(startDate, dateFormatter),
        createdAt = createdAt
    )
}

fun UserShiftConfig.toEntity(): UserShiftConfigEntity {
    return UserShiftConfigEntity(
        id = 1,
        officeCode = officeCode,
        officeName = officeName,
        position = position.displayName,
        shiftPattern = shiftPattern.joinToString(","),
        startDate = startDate.format(dateFormatter),
        todayShift = todayShift,
        todayShiftIndex = todayShiftIndex,
        referenceDate = referenceDate.format(dateFormatter),
        createdAt = createdAt
    )
}

// SubShiftConfig 변환 (UserShiftConfig 도메인 모델 재사용)
fun SubShiftConfigEntity.toDomain(): UserShiftConfig {
    val position = when (this.position) {
        "기관사" -> UserShiftConfig.Position.ENGINEER
        "차장" -> UserShiftConfig.Position.CONDUCTOR
        "4조2교대" -> UserShiftConfig.Position.FOUR_SHIFT
        else -> UserShiftConfig.Position.ENGINEER
    }

    return UserShiftConfig(
        officeCode = officeCode,
        officeName = officeName,
        position = position,
        shiftPattern = shiftPattern.split(",").map { it.trim() },
        startDate = LocalDate.parse(startDate, dateFormatter),
        todayShift = todayShift,
        todayShiftIndex = todayShiftIndex,
        referenceDate = if (referenceDate.isNotBlank()) LocalDate.parse(referenceDate, dateFormatter) else LocalDate.parse(startDate, dateFormatter),
        createdAt = createdAt
    )
}

fun UserShiftConfig.toSubEntity(): SubShiftConfigEntity {
    return SubShiftConfigEntity(
        id = 1,
        officeCode = officeCode,
        officeName = officeName,
        position = position.displayName,
        shiftPattern = shiftPattern.joinToString(","),
        startDate = startDate.format(dateFormatter),
        todayShift = todayShift,
        todayShiftIndex = todayShiftIndex,
        referenceDate = referenceDate.format(dateFormatter),
        createdAt = createdAt
    )
}

// ShiftSchedule 변환
fun ShiftScheduleEntity.toDomain(): ShiftSchedule {
    return ShiftSchedule(
        date = LocalDate.parse(date, dateFormatter),
        shiftName = shiftName
    )
}

fun ShiftSchedule.toEntity(): ShiftScheduleEntity {
    return ShiftScheduleEntity(
        date = date.format(dateFormatter),
        shiftName = shiftName
    )
}
