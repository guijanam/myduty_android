package com.sonbum.diacalendar2.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MenuAnalysisDto(
    val id: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("cafeteria_name")
    val cafeteriaName: String? = null,
    @SerializedName("start_date")
    val startDate: String? = null,
    @SerializedName("end_date")
    val endDate: String? = null,
    @SerializedName("weekly_menus")
    val weeklyMenus: List<DailyMenuDto>? = null,
    @SerializedName("image_hash")
    val imageHash: String? = null
)

data class DailyMenuDto(
    val date: String,
    val meals: MealsDto? = null,
    @SerializedName("day_of_week")
    val dayOfWeek: String? = null
)

data class MealsDto(
    val breakfast: List<String>? = null,
    val lunch: List<String>? = null,
    val dinner: List<String>? = null
)
