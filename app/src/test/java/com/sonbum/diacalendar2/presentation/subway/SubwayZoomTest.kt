package com.sonbum.diacalendar2.presentation.subway

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class SubwayZoomTest {
	@Test
	fun `초기 화면은 축소되고 두번 탭하면 초기 화면의 두 배로 확대된다`() {
		assertEquals(1f, diagramEffectiveScale(SUBWAY_DIAGRAM_OVERVIEW_SCALE), 0.001f)
		assertEquals(2f, diagramEffectiveScale(SUBWAY_DIAGRAM_ZOOM), 0.001f)
	}

	@Test
	fun `확대하지 않았을 때 이동 위치는 중앙으로 고정된다`() {
		val result = constrainZoomOffset(
			offset = Offset(120f, -80f),
			viewportSize = IntSize(1000, 600),
			scale = 1f
		)

		assertEquals(Offset.Zero, result)
	}

	@Test
	fun `확대 중 드래그는 빈 공간이 보이지 않는 범위로 제한된다`() {
		val result = constrainZoomOffset(
			offset = Offset(900f, -700f),
			viewportSize = IntSize(1000, 600),
			scale = 2f
		)

		assertEquals(Offset(500f, -300f), result)
	}

	@Test
	fun `확대 중 범위 안의 드래그 위치는 유지된다`() {
		val result = constrainZoomOffset(
			offset = Offset(-180f, 120f),
			viewportSize = IntSize(1000, 600),
			scale = 2f
		)

		assertEquals(Offset(-180f, 120f), result)
	}

	@Test
	fun `내 열차 지점이 확대 화면 중앙에 오도록 이동값을 계산한다`() {
		val viewport = IntSize(1000, 600)
		assertEquals(
			Offset.Zero,
			zoomOffsetForPoint(Offset(500f, 300f), viewport, effectiveScale = 2f)
		)
		assertEquals(
			Offset(-300f, 200f),
			zoomOffsetForPoint(Offset(800f, 100f), viewport, effectiveScale = 2f)
		)
	}
}
