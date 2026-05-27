package com.sonbum.diacalendar2.domain.model

data class VacationType(
    val id: Long = 0,
    val name: String,
    val shortName: String,
    val isDefault: Boolean = false,
    val annualQuota: Int = 0,
    val resetMonthDay: String = "01-01",
    val grantYear: Int = 0,       // legacy — grantDate의 년도 부분
    val expiryYear: Int = 0,      // legacy — expiryDate의 년도 부분
    val grantDate: String = "",   // 발생일 "YYYY-MM-DD" (빈 문자열 = 미설정)
    val expiryDate: String = ""   // 소멸일 "YYYY-MM-DD" (빈 문자열 = 미설정)
) {
    /** 발생일~소멸일이 설정된 다년도 근태 여부 */
    val isMultiYear: Boolean get() = grantDate.isNotEmpty() && expiryDate.isNotEmpty()

    /** 표시용 발생년도 (grantDate 우선, 없으면 legacy grantYear) */
    val effectiveGrantYear: Int get() = grantDate.take(4).toIntOrNull() ?: grantYear

    /** 표시용 소멸년도 (expiryDate 우선, 없으면 legacy expiryYear) */
    val effectiveExpiryYear: Int get() = expiryDate.take(4).toIntOrNull() ?: expiryYear
}
