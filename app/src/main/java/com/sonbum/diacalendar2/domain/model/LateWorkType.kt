package com.sonbum.diacalendar2.domain.model

data class LateWorkType(
    val id: Long = 0,
    val name: String,
    val shortName: String,
    val isDefault: Boolean = false
)
