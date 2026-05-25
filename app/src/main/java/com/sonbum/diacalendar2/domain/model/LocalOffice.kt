package com.sonbum.diacalendar2.domain.model

data class LocalOffice(
    val id: Long = 0,
    val officeName: String,
    val diaTurns1: String?,
    val diaTurns2: String?,
    val subTurns: String?,
    val diaSelects: String?,
    val diaTurns3: String?,  // 운휴 근무 (공휴일/일요일 포함 시 휴무 계산에 사용)
    val createdAt: Long = System.currentTimeMillis()
)
