package com.sonbum.diacalendar2.domain.model

import java.time.LocalDate

/**
 * 충당 기록
 * @param id 고유 ID
 * @param date 충당 날짜
 * @param shiftInputTypeId 충당 종류 ID
 * @param shiftInputTypeName 충당 종류 이름 스냅샷
 * @param shortName 짧은 이름 스냅샷
 * @param colorHex 색상 헥스 코드 스냅샷
 * @param targetShiftName 교체할 교번 이름
 * @param originalShiftName 원래 교번 이름 스냅샷
 * @param groupId 그룹 ID (한번의 충당 작업으로 생성된 레코드들을 그룹화)
 */
data class ShiftInputRecord(
    val id: Long = 0,
    val date: LocalDate,
    val shiftInputTypeId: Long,
    val shiftInputTypeName: String,
    val shortName: String,
    val colorHex: String,
    val targetShiftName: String,
    val originalShiftName: String,
    val groupId: String
)
