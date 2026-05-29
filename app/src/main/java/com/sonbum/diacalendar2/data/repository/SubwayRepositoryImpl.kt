package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.remote.SubwayApiConfig
import com.sonbum.diacalendar2.data.remote.api.SubwayApi
import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionDto
import com.sonbum.diacalendar2.domain.repository.SubwayRepository

class SubwayRepositoryImpl(
    private val api: SubwayApi
) : SubwayRepository {

    override suspend fun getLinePositions(line: Int): Result<List<SubwayPositionDto>> =
        runCatching {
            val resp = api.getRealtimePosition(
                apiKey = SubwayApiConfig.API_KEY,
                count = SubwayApiConfig.DEFAULT_COUNT,
                lineSeg = "${line}호선"
            )
            // HTTP 200이어도 errorMessage.code로 논리 오류(데이터 없음 등)를 표현.
            // 비정상 코드는 빈 리스트로 처리해 화면에서 "운행 중 아님" 빈 상태를 보이게 한다.
            val code = resp.errorMessage?.code
            if (code != null && code != "INFO-000") {
                emptyList()
            } else {
                resp.realtimePositionList.orEmpty()
            }
        }
}
