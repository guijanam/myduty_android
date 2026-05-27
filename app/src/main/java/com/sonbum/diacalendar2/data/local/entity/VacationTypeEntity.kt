package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vacation_types")
data class VacationTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val shortName: String,
    val isDefault: Boolean = false,
    val annualQuota: Int = 0,
    val resetMonthDay: String = "01-01",
    val grantYear: Int = 0,         // 발생년도 (0 = 미설정) — legacy, grantDate로 대체
    val expiryYear: Int = 0,        // 소멸년도 (0 = 미설정) — legacy, expiryDate로 대체
    val grantDate: String = "",     // 발생일 "YYYY-MM-DD" (빈 문자열 = 미설정)
    val expiryDate: String = ""     // 소멸일 "YYYY-MM-DD" (빈 문자열 = 미설정)
)
