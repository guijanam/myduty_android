package com.sonbum.diacalendar2.domain.model

data class CustomShift(
    val id: Long = 0,
    val shiftName: String,
    val shiftPattern: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)
