package com.sonbum.diacalendar2.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 서울 열린데이터 광장 지하철 실시간 위치 API 응답.
 * API 필드명이 이미 camelCase라 @SerializedName은 동일명으로 둔다.
 */
data class SubwayPositionResponse(
    @SerializedName("errorMessage") val errorMessage: SubwayErrorMessage? = null,
    @SerializedName("realtimePositionList") val realtimePositionList: List<SubwayPositionDto>? = null
)

data class SubwayErrorMessage(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("code") val code: String? = null,      // "INFO-000" == 정상
    @SerializedName("message") val message: String? = null,
    @SerializedName("total") val total: Int? = null
)

data class SubwayPositionDto(
    @SerializedName("subwayId") val subwayId: String? = null,    // "1002" = 2호선
    @SerializedName("subwayNm") val subwayNm: String? = null,
    @SerializedName("statnId") val statnId: String? = null,      // 끝 4자리 = 역 순번
    @SerializedName("statnNm") val statnNm: String? = null,      // 현재 역
    @SerializedName("trainNo") val trainNo: String? = null,      // 열번
    @SerializedName("updnLine") val updnLine: String? = null,    // "0" 상행/내선, "1" 하행/외선
    @SerializedName("statnTnm") val statnTnm: String? = null,    // 종착역
    @SerializedName("trainSttus") val trainSttus: String? = null, // 0진입 1도착 2출발 3전역출발
    @SerializedName("directAt") val directAt: String? = null,    // "1" 급행
    @SerializedName("lstcarAt") val lstcarAt: String? = null      // "1" 막차
)
