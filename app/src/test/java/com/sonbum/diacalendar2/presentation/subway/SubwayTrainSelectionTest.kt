package com.sonbum.diacalendar2.presentation.subway

import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SubwayTrainSelectionTest {

	@Test
	fun `근무 문자열에서 가장 앞 열번만 사용한다`() {
		assertEquals("2205", primaryDutyTrainNo("2205", "2205,2239,2273"))
		assertEquals("2205", primaryDutyTrainNo("2205", ""))
	}

	@Test
	fun `현재 근무 열번이 있으면 전 열번을 선택하지 않는다`() {
		val current = train("2205")
		val previous = train("2171")

		val selection = selectTrackedTrain(
			all = listOf(previous, current),
			currentTrainNo = "2205",
			previousTrainNo = "2171"
		)

		assertSame(current, selection.current)
		assertNull(selection.previous)
		assertSame(current, selection.tracked)
	}

	@Test
	fun `현재 근무 열번이 없을 때만 전 열번을 선택한다`() {
		val previous = train("2171")

		val selection = selectTrackedTrain(
			all = listOf(train("3101"), previous),
			currentTrainNo = "2205",
			previousTrainNo = "2171"
		)

		assertNull(selection.current)
		assertSame(previous, selection.previous)
		assertSame(previous, selection.tracked)
	}

	@Test
	fun `현재와 전 열번 모두 없으면 추적 열차가 없다`() {
		val selection = selectTrackedTrain(
			all = listOf(train("3101")),
			currentTrainNo = "2205",
			previousTrainNo = "2171"
		)

		assertNull(selection.current)
		assertNull(selection.previous)
		assertNull(selection.tracked)
	}

	@Test
	fun `뒤 세 자리가 같은 열차보다 완전 일치를 우선한다`() {
		val suffixOnly = train("3059")
		val exact = train("2059")

		val selection = selectTrackedTrain(
			all = listOf(suffixOnly, exact),
			currentTrainNo = "2059",
			previousTrainNo = null
		)

		assertNotNull(selection.current)
		assertSame(exact, selection.current)
	}

	private fun train(number: String) = SubwayPositionDto(
		statnId = "1002000212",
		statnNm = "건대입구",
		trainNo = number,
		updnLine = "0"
	)
}
