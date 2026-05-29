package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionDto

interface SubwayRepository {
    /** 지정 호선의 현재 운행 열차 전체. 논리 오류/데이터 없음은 빈 리스트로 성공 처리. */
    suspend fun getLinePositions(line: Int): Result<List<SubwayPositionDto>>
}
