package com.sonbum.diacalendar2.data.local

import android.content.Context
import android.util.Log
import com.sonbum.diacalendar2.domain.model.StationRef
import com.sonbum.diacalendar2.domain.model.SubwayLineDef
import com.sonbum.diacalendar2.domain.model.SubwaySegmentDef
import com.sonbum.diacalendar2.domain.model.SubwayStationAsset
import kotlinx.serialization.json.Json

/**
 * assets/subway_stations.json 을 읽어 노선도 배경(전체 역 목록)을 제공한다.
 *
 * 실시간 위치 API의 statnId를 그대로 키로 쓰며, 코드에서 statnId를 파싱하거나
 * 순번을 계산하지 않는다. 지선 코드 블록 규칙이 바뀌어도 asset만 갱신하면 된다.
 */
class SubwayStationRegistry(private val context: Context) {

	private val asset: SubwayStationAsset by lazy { load() }

	/** line -> 정의 */
	private val lineIndex: Map<Int, SubwayLineDef> by lazy {
		asset.lines.associateBy { it.line }
	}

	/** line -> (statnId -> 소속 구간들). 분기역은 여러 구간에 속한다. */
	private val stationIndex: Map<Int, Map<String, List<StationRef>>> by lazy {
		asset.lines.associate { lineDef ->
			val map = mutableMapOf<String, MutableList<StationRef>>()
			lineDef.segments.forEach { seg ->
				seg.stations.forEachIndexed { index, station ->
					map.getOrPut(station.statnId) { mutableListOf() }
						.add(StationRef(seg.id, index))
				}
			}
			lineDef.line to map
		}
	}

	fun line(line: Int): SubwayLineDef? = lineIndex[line]

	fun segments(line: Int): List<SubwaySegmentDef> = lineIndex[line]?.segments.orEmpty()

	fun segment(line: Int, segmentId: String): SubwaySegmentDef? =
		lineIndex[line]?.segments?.firstOrNull { it.id == segmentId }

	/** 해당 호선에 역 데이터가 있는지. 없으면 화면은 기존(열차 있는 역만) 방식으로 폴백한다. */
	fun isLoaded(line: Int): Boolean = !lineIndex[line]?.segments.isNullOrEmpty()

	/**
	 * statnId가 속한 구간들을 반환. 분기역(성수 등)은 복수 반환된다.
	 * 비어 있으면 asset에 없는 역 → 호출부에서 경고 로그를 남긴다.
	 */
	fun locate(line: Int, statnId: String?): List<StationRef> {
		if (statnId.isNullOrBlank()) return emptyList()
		return stationIndex[line]?.get(statnId).orEmpty()
	}

	private fun load(): SubwayStationAsset {
		return try {
			context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
				JSON.decodeFromString<SubwayStationAsset>(reader.readText())
			}
		} catch (e: Exception) {
			Log.w(TAG, "Failed to load $ASSET_NAME", e)
			SubwayStationAsset()
		}
	}

	companion object {
		private const val ASSET_NAME = "subway_stations.json"
		private const val TAG = "SubwayStationRegistry"
		private val JSON = Json { ignoreUnknownKeys = true }
	}
}
