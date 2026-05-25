package com.sonbum.diacalendar2.domain.model

data class Dia(
    val id: Long,
    val diaId: String,
    val officeName: String,
    val officeId: Int?,
    val typeName: String?,
    val firstTime: String?,
    val numTr1: String?,
    val numTr2: String?,
    val secondTime: String?,
    val thirdTime: String?,
    val totalTime: String?,
    val workTime: String?
)
