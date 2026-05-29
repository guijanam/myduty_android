package com.sonbum.diacalendar2.domain.util

/**
 * 열번(numTr) 문자열 파싱과 서울 지하철 실시간 위치 코드 매핑 헬퍼.
 * iOS SubwayLine.swift 대응. 순수 함수만 둔다(테스트 용이).
 */
object SubwayTrainParser {

    private val PAREN = Regex("\\([^)]*\\)")
    private val WS = Regex("\\s+")

    /**
     * numTr 문자열을 열번 토큰 배열로 분해. 괄호 묶음 제거 후 공백 분리.
     * "(39)2030 2080 2134(23)" -> ["2030","2080","2134"]
     * "2205 2239 2273"          -> ["2205","2239","2273"]
     */
    fun tokens(numTr: String?): List<String> =
        numTr.orEmpty()
            .replace(PAREN, " ")
            .trim()
            .split(WS)
            .filter { it.isNotBlank() }

    /** 내가 교대할 열번 = 항상 첫 토큰. */
    fun firstToken(numTr: String?): String? = tokens(numTr).firstOrNull()

    /** 전체가 숫자인지. */
    fun isNumeric(token: String?): Boolean =
        !token.isNullOrBlank() && token.all { it.isDigit() }

    /** 실시간 위치 버튼을 노출할 자격(첫 토큰이 숫자). */
    fun hasShiftTrain(numTr: String?): Boolean = isNumeric(firstToken(numTr))

    /**
     * 열번 첫 자리 = 호선(1~9). 첫 글자가 숫자가 아니거나 범위 밖이면 null.
     * "2204" -> 2, "K2317" -> null
     */
    fun line(trainNo: String): Int? {
        val first = trainNo.trim().firstOrNull()?.digitToIntOrNull() ?: return null
        return if (first in 1..9) first else null
    }

    /**
     * API trainNo와 비교할 매칭 키. API trainNo는 4자리이고 첫 자리가 호선이 아니므로
     * (1호선은 "0161" 0패딩) 양쪽 모두 뒤 3자리로 맞춘다.
     * "2204" -> "204", "0161" -> "161"
     */
    fun matchKey(trainNo: String): String = trainNo.trim().takeLast(3)

    /** 홀수 열번 여부(2호선 성수 교대 보조 트리거). */
    fun isOdd(trainNo: String): Boolean =
        (trainNo.trim().toIntOrNull() ?: 0) % 2 != 0

    /** 두 열번이 같은 열차인지(앞 호선자리 무시, 뒤 3자리 비교). */
    fun sameTrain(a: String, b: String): Boolean = matchKey(a) == matchKey(b)

    /**
     * 전 열번 조회용: numTr 문자열에서 target이 "마지막 토큰"이면 바로 앞 토큰 반환.
     * tokens("2205 2239 2273"), target="2273" -> "2239"
     */
    fun previousTokenIfLast(numTr: String?, target: String): String? {
        val t = tokens(numTr)
        return if (t.size >= 2 && t.last() == target) t[t.size - 2] else null
    }
}
