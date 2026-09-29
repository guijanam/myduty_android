package com.sonbum.diacalendar2.presentation.subway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubwayZigzagLayoutTest {

	@Test
	fun `2호선과 지선은 지그재그 표시 대상에서 제외한다`() {
		assertFalse(shouldUseZigzagDiagram(line = 2, segmentId = "main", isLoop = true))
		assertFalse(shouldUseZigzagDiagram(line = 1, segmentId = "gyeongbu", isLoop = false))
		assertFalse(shouldUseZigzagDiagram(line = 5, segmentId = "macheon", isLoop = false))
		assertTrue(shouldUseZigzagDiagram(line = 1, segmentId = "main", isLoop = false))
		assertTrue(shouldUseZigzagDiagram(line = 9, segmentId = "main", isLoop = false))
	}

	@Test
	fun `역 순서를 유지하며 행마다 진행 방향을 뒤집는다`() {
		val layout = buildZigzagLayout(stationCount = 12, stationsPerRow = 5)

		assertEquals(5, layout.columnCount)
		assertEquals(3, layout.rowCount)
		assertEquals((0..4).toList(), layout.nodes.take(5).map { it.column })
		assertEquals((4 downTo 0).toList(), layout.nodes.drop(5).take(5).map { it.column })
		assertEquals(listOf(0, 1), layout.nodes.takeLast(2).map { it.column })
		assertEquals((0 until 12).toList(), layout.nodes.map { it.stationIndex })
	}

	@Test
	fun `마지막 행이 덜 차도 이전 행 끝에서 자연스럽게 이어진다`() {
		val evenLastRow = buildZigzagLayout(stationCount = 7, stationsPerRow = 5)
		assertEquals(4, evenLastRow.nodeOf(5)?.column)
		assertEquals(3, evenLastRow.nodeOf(6)?.column)

		val oddLastRow = buildZigzagLayout(stationCount = 12, stationsPerRow = 5)
		assertEquals(0, oddLastRow.nodeOf(10)?.column)
		assertEquals(1, oddLastRow.nodeOf(11)?.column)
	}

	@Test
	fun `열차 역 인덱스를 확대 중심 좌표로 변환한다`() {
		val first = requireNotNull(zigzagStationFocusFraction(12, 0, 5))
		val firstTurn = requireNotNull(zigzagStationFocusFraction(12, 4, 5))
		val secondRowStart = requireNotNull(zigzagStationFocusFraction(12, 5, 5))
		val secondRowEnd = requireNotNull(zigzagStationFocusFraction(12, 9, 5))

		assertTrue(first.x < firstTurn.x)
		assertEquals(firstTurn.x, secondRowStart.x, 0.001f)
		assertTrue(secondRowEnd.x < secondRowStart.x)
		assertTrue(secondRowStart.y > firstTurn.y)
		assertNull(zigzagStationFocusFraction(12, 12, 5))
	}

	@Test
	fun `빈 노선과 한 역 노선도 안전하게 처리한다`() {
		assertEquals(0, buildZigzagLayout(0).rowCount)
		assertNull(zigzagStationFocusFraction(0, 0))

		val only = requireNotNull(zigzagStationFocusFraction(1, 0))
		assertEquals(0.5f, only.x, 0.001f)
		assertEquals(0.5f, only.y, 0.001f)
	}

	@Test
	fun `지그재그 열차 선두는 수평 구간과 행 전환의 실제 진행 방향을 따른다`() {
		assertEquals(TrainHeading.RIGHT, zigzagTrainHeading(1, 12, "1"))
		assertEquals(TrainHeading.LEFT, zigzagTrainHeading(6, 12, "1"))
		assertEquals(TrainHeading.DOWN, zigzagTrainHeading(4, 12, "1"))
		assertEquals(TrainHeading.UP, zigzagTrainHeading(5, 12, "0"))
	}

	@Test
	fun `2호선 내선과 외선의 선두 방향은 서로 반대다`() {
		assertEquals(TrainHeading.RIGHT, line2TrainHeading(TrainHeading.RIGHT, "0"))
		assertEquals(TrainHeading.LEFT, line2TrainHeading(TrainHeading.RIGHT, "1"))
		assertEquals(TrainHeading.UP, line2TrainHeading(TrainHeading.UP, "0"))
		assertEquals(TrainHeading.DOWN, line2TrainHeading(TrainHeading.UP, "1"))
	}
}
