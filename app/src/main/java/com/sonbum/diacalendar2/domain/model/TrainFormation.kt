package com.sonbum.diacalendar2.domain.model

/**
 * 근무의 전반/후반 구분.
 * 저장은 enum 이름("FIRST"/"SECOND"), 표시는 label("전반"/"후반").
 */
enum class TrainHalf(val label: String) {
    FIRST("전반"),
    SECOND("후반");

    companion object {
        fun from(raw: String): TrainHalf = entries.firstOrNull { it.name == raw } ?: FIRST
    }
}

/**
 * 열차 편성 기록 Domain 모델.
 *
 * shiftName / numTr 은 기록 당시 스냅샷 — 나중에 교번이 바뀌어도 소급 변조되지 않는다.
 * numTr 은 열번이 없는 승무소(대기근무 등)에서는 빈 문자열일 수 있다.
 */
data class TrainFormation(
    val id: Long = 0,
    val date: String,            // yyyy-MM-dd 형식
    val half: TrainHalf,
    val formationNo: Int,
    val note: String = "",
    val shiftName: String = "",
    val numTr: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
