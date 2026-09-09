package com.sonbum.diacalendar2.presentation.subway

import com.sonbum.diacalendar2.domain.model.SubwayStationAsset
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.io.File

/**
 * assets/subway_stations.json 무결성 검증.
 *
 * 이 asset이 틀리면 열차가 노선도에서 "조용히" 사라지므로(크래시 없음),
 * 스키마와 statnId 형식을 테스트로 고정한다.
 * 유닛 테스트에서는 assets를 열 수 없어 파일을 직접 읽는다.
 */
class SubwayStationAssetTest {

	private val json = Json { ignoreUnknownKeys = true }

	private val asset: SubwayStationAsset by lazy {
		val file = File("src/main/assets/subway_stations.json")
		assertTrue("asset 파일이 없습니다: ${file.absolutePath}", file.exists())
		json.decodeFromString<SubwayStationAsset>(file.readText())
	}

	@Test
	fun `1호선부터 9호선까지 모두 존재한다`() {
		val lines = asset.lines.map { it.line }.sorted()
		assertEquals((1..9).toList(), lines)
	}

	@Test
	fun `모든 statnId는 10자리이며 해당 호선의 subwayId로 시작한다`() {
		asset.lines.forEach { line ->
			line.segments.forEach { seg ->
				seg.stations.forEach { station ->
					assertEquals(
						"statnId 길이 오류: ${line.line}호선 ${seg.id} ${station.name} ${station.statnId}",
						10, station.statnId.length
					)
					assertTrue(
						"subwayId 불일치: ${station.name} ${station.statnId} != ${line.subwayId}",
						station.statnId.startsWith(line.subwayId)
					)
				}
			}
		}
	}

	@Test
	fun `구간 안에서 statnId는 중복되지 않는다`() {
		asset.lines.forEach { line ->
			line.segments.forEach { seg ->
				val ids = seg.stations.map { it.statnId }
				assertEquals(
					"${line.line}호선 ${seg.id} 구간에 중복 statnId가 있습니다",
					ids.size, ids.distinct().size
				)
			}
		}
	}

	@Test
	fun `2호선은 본선 순환과 두 개의 지선을 가진다`() {
		val line2 = asset.lines.first { it.line == 2 }
		val main = line2.segments.first { it.id == "main" }

		assertTrue("2호선 본선은 순환선이어야 합니다", main.loop)
		assertEquals("시청", main.stations.first().name)
		assertEquals("충정로", main.stations.last().name)

		// 스크린샷 탭 구성과 동일: 내선/외선 + 성수지선 + 신정지선
		assertEquals(
			listOf("내선순환", "외선순환"),
			main.directions.map { it.label }
		)
		assertNotNull(line2.segments.firstOrNull { it.id == "seongsu" })
		assertNotNull(line2.segments.firstOrNull { it.id == "sinjeong" })
	}

	@Test
	fun `지선은 본선 분기역을 첫 역으로 포함한다`() {
		val line2 = asset.lines.first { it.line == 2 }
		val seongsu = line2.segments.first { it.id == "seongsu" }
		val sinjeong = line2.segments.first { it.id == "sinjeong" }

		assertEquals("성수", seongsu.stations.first().name)
		assertEquals("신설동", seongsu.stations.last().name)
		assertEquals("신도림", sinjeong.stations.first().name)
		assertEquals("까치산", sinjeong.stations.last().name)
	}

	@Test
	fun `분기역은 본선과 지선 양쪽에 같은 statnId로 존재한다`() {
		val line2 = asset.lines.first { it.line == 2 }
		val mainSeongsu = line2.segments.first { it.id == "main" }
			.stations.first { it.name == "성수" }
		val branchSeongsu = line2.segments.first { it.id == "seongsu" }
			.stations.first { it.name == "성수" }

		assertEquals(mainSeongsu.statnId, branchSeongsu.statnId)
	}

	@Test
	fun `1호선 서동탄은 지선의 재지선 형식으로 인코딩된다`() {
		// P157-1 -> 1001801571 (실시간 API 실측값). 이 규칙이 깨지면 열차가 사라진다.
		val line1 = asset.lines.first { it.line == 1 }
		val seodongtan = line1.segments
			.flatMap { it.stations }
			.first { it.name == "서동탄" }
		assertEquals("1001801571", seodongtan.statnId)
	}

	@Test
	fun `본선 이외 구간은 순환선이 아니다`() {
		asset.lines.forEach { line ->
			line.segments.filter { it.id != "main" }.forEach { seg ->
				assertFalse("${line.line}호선 ${seg.id}은 순환선이 아니어야 합니다", seg.loop)
			}
		}
	}
}
