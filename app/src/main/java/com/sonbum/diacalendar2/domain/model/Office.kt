package com.sonbum.diacalendar2.domain.model

data class Office(
    val officeCode: Long,
    val officeName: String,
    val diaTurns1: String?,
    val diaTurns2: String?,
    val subTurns: String?,
    val diaSelects: String?,
    val diaTurns3: String?,
    val adminPassword: String?
)
