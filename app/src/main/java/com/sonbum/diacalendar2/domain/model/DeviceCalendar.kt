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
    val isPrimary: Boolean = false,
    /** 이벤트를 쓸 수 있는 캘린더인지 (access level >= CONTRIBUTOR) */
    val isWritable: Boolean = false
) {
    /** Google 계정 캘린더 여부 (웹/공유 동기화 대상) */
    val isGoogle: Boolean get() = accountType == "com.google"
}
