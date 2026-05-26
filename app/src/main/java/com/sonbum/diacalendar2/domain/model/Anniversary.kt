package com.sonbum.diacalendar2.domain.model

data class Anniversary(
    val id: Long = 0,
    val name: String,
    val month: Int,
    val day: Int,
    val isLunar: Boolean
)
