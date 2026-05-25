package com.sonbum.diacalendar2.domain.model

data class CafeteriaMenu(
    val cafeteriaName: String,
    val date: String,
    val dayOfWeek: String,
    val breakfast: List<String>,
    val lunch: List<String>,
    val dinner: List<String>
)
