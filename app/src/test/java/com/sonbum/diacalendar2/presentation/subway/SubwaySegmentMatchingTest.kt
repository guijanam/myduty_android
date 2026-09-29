package com.sonbum.diacalendar2.presentation.subway

import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionDto
import com.sonbum.diacalendar2.domain.model.StationRef
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubwaySegmentMatchingTest {

	private val junctionRefs = listOf(
		StationRef(segmentId = "main", index = 0),
		StationRef(segmentId = "seongsu", index = 4)
	)

	@Test
	fun `성수역의 본선 열차는 성수지선에 표시하지 않는다`() {
		val train = train(routeSegmentId = "main")

		assertTrue(matchesSubwaySegment(train, "main", "0", junctionRefs))
		assertFalse(matchesSubwaySegment(train, "seongsu", null, junctionRefs))
	}

	@Test
	fun `성수역의 지선 열차는 본선이 아니라 성수지선에 표시한다`() {
		val train = train(routeSegmentId = "seongsu")

		assertFalse(matchesSubwaySegment(train, "main", "0", junctionRefs))
		assertTrue(matchesSubwaySegment(train, "seongsu", null, junctionRefs))
	}

	@Test
	fun `신도림역의 본선 열차는 신정지선에 표시하지 않는다`() {
		val train = train(routeSegmentId = "main")
		val sindorimRefs = listOf(
			StationRef(segmentId = "main", index = 27),
			StationRef(segmentId = "sinjeong", index = 4)
		)

		assertTrue(matchesSubwaySegment(train, "main", "0", sindorimRefs))
		assertFalse(matchesSubwaySegment(train, "sinjeong", null, sindorimRefs))
	}

	@Test
	fun `구간 힌트가 없는 분기역 열차는 지선에 표시하지 않는다`() {
		val train = train(routeSegmentId = null)

		assertTrue(matchesSubwaySegment(train, "main", "0", junctionRefs))
		assertFalse(matchesSubwaySegment(train, "seongsu", null, junctionRefs))
	}

	@Test
	fun `지선 전용역은 구간 힌트가 없어도 기존처럼 표시한다`() {
		val train = train(routeSegmentId = null)
		val branchOnly = listOf(StationRef(segmentId = "seongsu", index = 2))

		assertTrue(matchesSubwaySegment(train, "seongsu", null, branchOnly))
	}

	private fun train(routeSegmentId: String?) = SubwayPositionDto(
		subwayId = "1002",
		statnId = "1002000211",
		statnNm = "성수",
		trainNo = "2558",
		updnLine = "0",
		routeSegmentId = routeSegmentId
	)
}
