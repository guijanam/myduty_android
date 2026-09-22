package com.sonbum.diacalendar2.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * 서울교통공사 실시간 열차 운행정보 화면이 사용하는 HTML 조각 엔드포인트.
 * 공식 Open API가 아니므로 호출 실패나 HTML 변경은 기존 TOPIS 데이터로 폴백한다.
 */
interface SeoulMetroTrainApi {

    @FormUrlEncoded
    @POST("traininfo/traininfoUserMap.do")
    suspend fun getTrainMap(
        @Field("line") line: Int,
        @Field("isCb") isColorBlindMap: String = "N"
    ): ResponseBody
}
