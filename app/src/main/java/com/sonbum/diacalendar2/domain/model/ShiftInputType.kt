package com.sonbum.diacalendar2.domain.model

import androidx.compose.ui.graphics.Color

/**
 * 충당 종류 (대기충당, 휴무충당, 지근충당)
 * @param id 고유 ID
 * @param name 충당 이름 (예: "대기충당", "휴무충당", "지근충당")
 * @param shortName 짧은 이름 (예: "대기", "휴무", "지근")
 * @param colorHex 색상 헥스 코드 (예: "#4CAF50")
 * @param isDefault 기본값 여부
 * @param requiresLateWork 지근이 설정된 날짜에만 사용 가능 여부 (지근충당에 해당)
 */
data class ShiftInputType(
    val id: Long = 0,
    val name: String,
    val shortName: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val requiresLateWork: Boolean = false
) {
    companion object {
        // 기본 충당 타입들
        val DEFAULT_STANDBY = ShiftInputType(
            id = 1,
            name = "대기충당",
            shortName = "대기",
            colorHex = "#4CAF50", // 초록색
            isDefault = true,
            requiresLateWork = false
        )

        val DEFAULT_HOLIDAY = ShiftInputType(
            id = 2,
            name = "휴무충당",
            shortName = "휴무",
            colorHex = "#9C27B0", // 보라색
            isDefault = false,
            requiresLateWork = false
        )

        val DEFAULT_LATE_WORK = ShiftInputType(
            id = 3,
            name = "지근충당",
            shortName = "지근",
            colorHex = "#03A9F4", // 하늘색
            isDefault = false,
            requiresLateWork = true // 지근이 설정된 날짜에만 사용 가능
        )

        val DEFAULT_TYPES = listOf(DEFAULT_STANDBY, DEFAULT_HOLIDAY, DEFAULT_LATE_WORK)
    }
}
