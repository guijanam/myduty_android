package com.sonbum.diacalendar2.data.remote.api

import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface SubwayApi {

    /**
     * 지정 호선의 실시간 열차 위치 전체 조회.
     * 실제 경로: api/subway/{key}/json/realtimePosition/0/{count}/{line}호선
     * "{line}호선"은 한글 인코딩/세그먼트 경계 문제를 피하려고 통째로 lineSeg에 담아 전달한다.
     */
    @GET("api/subway/{key}/json/realtimePosition/0/{count}/{lineSeg}")
    suspend fun getRealtimePosition(
        @Path("key") apiKey: String,
        @Path("count") count: Int,
        @Path("lineSeg") lineSeg: String   // 예: "2호선"
    ): SubwayPositionResponse
}
