package com.sonbum.diacalendar2.domain.model

import kotlinx.serialization.Serializable

/**
 * assets/subway_stations.json 구조.
 *
 * 실시간 위치 API(realtimePosition)는 "열차가 있는 역"만 응답하므로,
 * 노선도의 배경이 되는 전체 역 목록을 앱에 동봉한다.
 * 생성 스크립트: docs/tools/gen_subway_stations.py
 *
 * 역 순서는 배열 순서로만 표현한다. statnId에서 순번을 계산하지 않는다
 * (지선은 코드 블록이 불연속이라 숫자 정렬이 성립하지 않음).
 */
@Serializable
data class SubwayStationAsset(
	val version: Int = 1,
	val lines: List<SubwayLineDef> = emptyList()
)

@Serializable
data class SubwayLineDef(
	val line: Int,
	val subwayId: String,
	val name: String,
	val colorHex: String,
	val segments: List<SubwaySegmentDef> = emptyList()
)

/** 본선/지선 등 하나의 운행 구간. 하단 탭 하나에 대응한다. */
@Serializable
data class SubwaySegmentDef(
	val id: String,
	val title: String,
	/** 2호선 본선처럼 순환선이면 true. */
	val loop: Boolean = false,
	val directions: List<SubwayDirectionDef> = emptyList(),
	val stations: List<SubwayStationDef> = emptyList()
)

/** updnLine 코드 -> 표시 라벨(내선순환/외선순환 또는 상행/하행). */
@Serializable
data class SubwayDirectionDef(
	val updnLine: String,
	val label: String
)

@Serializable
data class SubwayStationDef(
	val statnId: String,
	val name: String
)

/**
 * 역 위치 참조. 분기역(예: 성수)은 여러 구간에 속하므로 조회 결과가 복수일 수 있다.
 */
data class StationRef(
	val segmentId: String,
	val index: Int
)
