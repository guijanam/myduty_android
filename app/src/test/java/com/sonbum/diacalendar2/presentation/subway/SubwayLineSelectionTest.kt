package com.sonbum.diacalendar2.presentation.subway

import org.junit.Assert.assertEquals
import org.junit.Test

class SubwayLineSelectionTest {

	@Test
	fun `근무 열번 호선이 있으면 저장된 호선보다 우선한다`() {
		assertEquals(
			2,
			resolveInitialSubwayLine(savedLine = 4, dutyLine = 2, fallbackLine = 1)
		)
	}

	@Test
	fun `근무 열번 호선이 없으면 저장된 호선을 사용한다`() {
		assertEquals(
			4,
			resolveInitialSubwayLine(savedLine = 4, dutyLine = null, fallbackLine = 1)
		)
	}

	@Test
	fun `근무 열번과 저장된 호선이 없으면 전달된 호선을 사용한다`() {
		assertEquals(
			2,
			resolveInitialSubwayLine(savedLine = null, dutyLine = null, fallbackLine = 2)
		)
	}

	@Test
	fun `저장값과 추론값이 유효하지 않으면 1호선을 사용한다`() {
		assertEquals(
			DEFAULT_SUBWAY_LINE,
			resolveInitialSubwayLine(savedLine = 0, dutyLine = null, fallbackLine = 0)
		)
		assertEquals(
			DEFAULT_SUBWAY_LINE,
			resolveInitialSubwayLine(savedLine = 10, dutyLine = 10, fallbackLine = 10)
		)
	}

	@Test
	fun `1호선은 성수지선 표시를 위해 2호선도 함께 조회한다`() {
		assertEquals(listOf(1, 2), requestedSubwayLines(selectedLine = 1))
	}

	@Test
	fun `다른 호선은 선택한 호선만 조회한다`() {
		assertEquals(listOf(2), requestedSubwayLines(selectedLine = 2))
		assertEquals(listOf(9), requestedSubwayLines(selectedLine = 9))
	}

	@Test
	fun `내 열차 행은 화면 세로 중앙에 오도록 스크롤한다`() {
		assertEquals(-272, centeredTrainScrollOffset(viewportHeightPx = 600, itemHeightPx = 56))
		assertEquals(0, centeredTrainScrollOffset(viewportHeightPx = 40, itemHeightPx = 56))
	}

	@Test
	fun `해당 열번이 있으면 첫 화면부터 내 열차 행에서 시작한다`() {
		assertEquals(17, initialTrainListIndex(myTrainIndex = 17, stationCount = 45))
		assertEquals(0, initialTrainListIndex(myTrainIndex = null, stationCount = 45))
		assertEquals(0, initialTrainListIndex(myTrainIndex = 17, stationCount = 0))
	}
}
