package com.sonbum.diacalendar2.data.local.mapper

import com.sonbum.diacalendar2.data.local.entity.HolidayEntity
import com.sonbum.diacalendar2.domain.model.Holiday
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun HolidayEntity.toDomain(): Holiday {
    return Holiday(
        id = id,
        date = LocalDate.parse(locdate, dateFormatter),
        name = dateName,
        isHoliday = isHoliday == "Y",
        isUserCreated = isUserCreated
    )
}

fun Holiday.toEntity(): HolidayEntity {
    return HolidayEntity(
        id = id,
        locdate = date.format(dateFormatter),
        dateName = name,
        isHoliday = if (isHoliday) "Y" else "N",
        isUserCreated = isUserCreated
    )
}
