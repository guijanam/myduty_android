package com.sonbum.diacalendar2.domain.model

data class LocalDia(
    val id: Long = 0,
    val diaId: String,
    val localOfficeId: Long,
    val officeName: String,
    val typeName: String?,
    val firstTime: String?,
    val numTr1: String?,
    val numTr2: String?,
    val secondTime: String?,
    val thirdTime: String?,
    val totalTime: String?,
    val workTime: String?
)
