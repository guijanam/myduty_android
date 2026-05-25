package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coworkers")
data class CoworkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    /** 소속 그룹 ID 목록 (CSV, 예: "1,2,3") */
    val groupIds: String = "",
    /** 근무 패턴 (CSV, 예: "주,야,비,휴") */
    val shiftPattern: String = "",
    /** 기준 날짜 (yyyy-MM-dd) */
    val referenceDate: String = "",
    /** 기준 근무명 */
    val referenceShift: String = "",
    /** 기준 근무의 패턴 내 인덱스 (중복 근무명 구분용) */
    val referenceShiftIndex: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
