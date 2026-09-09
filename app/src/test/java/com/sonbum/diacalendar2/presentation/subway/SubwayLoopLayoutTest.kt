package com.sonbum.diacalendar2.presentation.subway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2호선 순환 고리를 네 변으로 자르는 로직 검증.
 * 공식 노선도(시청 상단 좌측 → 시계방향) 형태를 코드로 고정한다.
 */
class SubwayLoopLayoutTest {

	private val line2 = listOf(
		"시청", "을지로입구", "을지로3가", "을지로4가", "동대문역사문화공원", "신당",
		"상왕십리", "왕십리", "한양대", "뚝섬", "성수", "건대입구", "구의", "강변",
		"잠실나루", "잠실", "잠실새내", "종합운동장", "삼성", "선릉", "역삼", "강남",
		"교대", "서초", "방배", "사당", "낙성대", "서울대입구", "봉천", "신림", "신대방",
		"구로디지털단지", "대림", "신도림", "문래", "영등포구청", "당산", "합정",
		"홍대입구", "신촌", "이대", "아현", "충정로"
	).map { SubwayStationUi(statnId = it, name = it, trains = emptyList()) }

	private fun names(list: List<SubwayStationUi>) = list.map { it.name }

	@Test
	fun `네 변이 전체 역을 중복 없이 나눠 가진다`() {
		val l = buildLoopLayout(line2)
		val all = names(l.top) + names(l.right) + names(l.bottom) + names(l.left)

		assertEquals("역이 누락되거나 중복되었습니다", line2.size, all.size)
		assertEquals(line2.map { it.name }.toSet(), all.toSet())
	}

	@Test
	fun `상단은 시청에서 왕십리까지 시계방향이다`() {
		val l = buildLoopLayout(line2)
		assertEquals("시청", names(l.top).first())
		assertEquals("왕십리", names(l.top).last())
		assertEquals("을지로입구", names(l.top)[1])
	}

	@Test
	fun `우측은 왕십리 다음부터 종합운동장 직전까지다`() {
		val l = buildLoopLayout(line2)
		assertEquals("한양대", names(l.right).first())
		assertEquals("잠실새내", names(l.right).last())
	}

	@Test
	fun `하단은 종합운동장에서 신도림까지다`() {
		val l = buildLoopLayout(line2)
		assertEquals("종합운동장", names(l.bottom).first())
		assertEquals("신도림", names(l.bottom).last())
		assertTrue("강남은 하단에 있어야 합니다", names(l.bottom).contains("강남"))
	}

	@Test
	fun `좌측은 신도림 다음부터 충정로까지이며 뒤집으면 시청과 이어진다`() {
		val l = buildLoopLayout(line2)
		assertEquals("문래", names(l.left).first())
		assertEquals("충정로", names(l.left).last())
		// 화면에는 reversed()로 그리므로 최상단이 충정로(= 시청 바로 앞 역)가 된다.
		assertEquals("충정로", names(l.left.reversed()).first())
	}

	@Test
	fun `코너 역명이 없어도 균등 분할로 폴백한다`() {
		val unnamed = (1..20).map {
			SubwayStationUi(statnId = "s$it", name = "역$it", trains = emptyList())
		}
		val l = buildLoopLayout(unnamed)
		val total = l.top.size + l.right.size + l.bottom.size + l.left.size

		assertEquals(unnamed.size, total)
		assertTrue("각 변이 비어 있으면 안 됩니다", l.top.isNotEmpty() && l.bottom.isNotEmpty())
	}
}
