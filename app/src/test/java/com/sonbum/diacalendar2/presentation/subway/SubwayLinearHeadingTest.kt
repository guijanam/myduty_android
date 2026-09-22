package com.sonbum.diacalendar2.presentation.subway

import org.junit.Assert.assertEquals
import org.junit.Test

class SubwayLinearHeadingTest {

	@Test
	fun `성수지선 열차는 성수행이 아래쪽 신설동행이 위쪽을 향한다`() {
		assertEquals(TrainHeading.DOWN, linearTrainHeading("seongsu", "0"))
		assertEquals(TrainHeading.UP, linearTrainHeading("seongsu", "1"))
	}

	@Test
	fun `다른 지선의 기존 방향은 유지한다`() {
		assertEquals(TrainHeading.UP, linearTrainHeading("sinjeong", "0"))
		assertEquals(TrainHeading.DOWN, linearTrainHeading("sinjeong", "1"))
		assertEquals(TrainHeading.DOWN, linearTrainHeading("seongsu", null))
	}
}
