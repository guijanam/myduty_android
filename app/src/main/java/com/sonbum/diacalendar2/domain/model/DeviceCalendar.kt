package com.sonbum.diacalendar2.domain.model

/**
 * 기기에 등록된 캘린더 정보를 나타내는 도메인 모델
 */
data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isPrimary: Boolean = false
)
