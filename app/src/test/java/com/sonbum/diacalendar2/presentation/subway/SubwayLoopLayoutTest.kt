package com.sonbum.diacalendar2.presentation.subway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2호선 순환 고리를 세로 두 열로 배치하는 로직 검증.
 * TOPIS 열번위치 화면(시청 정상단, 양옆으로 갈라 내려감)을 코드로 고정한다.
 */
class SubwayLoopLayoutTest {

	private val line2Names = listOf(
		"시청", "을지로입구", "을지로3가", "을지로4가", "동대문역사문화공원", "신당",
		"상왕십리", "왕십리", "한양대", "뚝섬", "성수", "건대입구", "구의", "강변",
		"잠실나루", "잠실", "잠실새내", "종합운동장", "삼성", "선릉", "역삼", "강남",
		"교대", "서초", "방배", "사당", "낙성대", "서울대입구", "봉천", "신림", "신대방",
		"구로디지털단지", "대림", "신도림", "문래", "영등포구청", "당산", "합정",
		"홍대입구", "신촌", "이대", "아현", "충정로"
	)

	private val line2 = line2Names.map {
		SubwayStationUi(statnId = it, name = it, trains = emptyList())
	}

	private fun names(list: List<SubwayStationUi>) = list.map { it.name }

	@Test
	fun `시청이 정상단에 온다`() {
		assertEquals("시청", buildLoopLayout(line2).top.name)
	}

	@Test
	fun `모든 역이 중복 없이 배치된다`() {
		val l = buildLoopLayout(line2)
		val all = listOf(l.top.name) + names(l.left) + names(l.right) + names(l.bottom)

		assertEquals("역이 누락되거나 중복되었습니다", line2.size, all.size)
		assertEquals(line2Names.toSet(), all.toSet())
	}

	@Test
	fun `오른쪽 열은 시청 다음부터 시계방향으로 내려간다`() {
		val right = names(buildLoopLayout(line2).right)
		assertEquals("을지로입구", right[0])
		assertEquals("을지로3가", right[1])
		assertTrue("성수는 오른쪽 열에 있어야 합니다", right.contains("성수"))
	}

	@Test
	fun `왼쪽 열은 충정로부터 역방향으로 내려간다`() {
		val left = names(buildLoopLayout(line2).left)
		assertEquals("충정로", left[0])
		assertEquals("아현", left[1])
		assertTrue("신도림은 왼쪽 열에 있어야 합니다", left.contains("신도림"))
	}

	@Test
	fun `양 열의 길이가 같아 나란히 놓인다`() {
		val l = buildLoopLayout(line2)
		assertEquals(l.left.size, l.right.size)
	}

	@Test
	fun `하단은 두 열을 잇는 가운데 구간이다`() {
		val bottom = names(buildLoopLayout(line2).bottom)
		// 43역이면 좌우 20역씩, 가운데 교대·강남이 하단 연결부가 된다.
		assertEquals(listOf("교대", "강남"), bottom)
	}

	@Test
	fun `내 열차의 원본 인덱스가 화면 행으로 환산된다`() {
		val l = buildLoopLayout(line2)
		assertEquals(0, l.rowOf(0))                       // 시청 = 최상단
		assertEquals(1, l.rowOf(1))                       // 을지로입구 = 1행(우)
		assertEquals(1, l.rowOf(line2.size - 1))          // 충정로 = 1행(좌)
		// 같은 행에 마주보는 두 역은 행 번호가 같아야 한다.
		assertEquals(l.rowOf(2), l.rowOf(line2.size - 2))
	}

	@Test
	fun `역이 적어도 안전하게 나뉜다`() {
		listOf(1, 2, 3, 4, 5, 20).forEach { size ->
			val small = (1..size).map {
				SubwayStationUi(statnId = "s$it", name = "역$it", trains = emptyList())
			}
			val l = buildLoopLayout(small)
			val total = 1 + l.left.size + l.right.size + l.bottom.size
			assertEquals("역 ${size}개 배치 실패", size, total)
			assertEquals(l.left.size, l.right.size)
		}
	}
}
