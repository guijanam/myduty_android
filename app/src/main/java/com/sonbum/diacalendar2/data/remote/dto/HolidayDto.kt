package com.sonbum.diacalendar2.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HolidayDto(
    val id: String,
    val locdate: String,
    @SerializedName("date_name")
    val dateName: String,
    @SerializedName("is_holiday")
    val isHoliday: String = "Y"
)
