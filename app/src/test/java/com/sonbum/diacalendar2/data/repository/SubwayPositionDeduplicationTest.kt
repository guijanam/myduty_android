package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SubwayPositionDeduplicationTest {

    @Test
    fun `TOPIS에 없는 서울교통공사 열차를 추가한다`() {
        val topis = train("4168", "1004000429", "2026-09-12 21:54:59")
        val metro = train("4169", "1004000408", "")

        assertEquals(listOf("4168", "4169"), mergeLinePositions(listOf(topis), listOf(metro)).map { it.trainNo })
    }

    @Test
    fun `뒤 세자리가 같은 열차는 서울교통공사 정보로 교체하고 TOPIS 메타데이터는 보존한다`() {
        val topis = train("1685", "1002002114", "2026-09-12 21:49:44", "1002").copy(
            directAt = "1",
            lstcarAt = "0"
        )
        val metro = train("2685", "1002002113", "", "1002")

        val result = mergeLinePositions(listOf(topis), listOf(metro)).single()

        assertEquals("2685", result.trainNo)
        assertEquals("1002002113", result.statnId)
        assertEquals("1", result.directAt)
        assertEquals("0", result.lstcarAt)
        assertEquals("2026-09-12 21:49:44", result.recptnDt)
    }

    @Test
    fun `같은 열번의 2호선 본선과 두 지선 위치를 모두 보존한다`() {
        val topis = train("1638", "1002000211", "2026-09-15 17:30:00", "1002").copy(
            directAt = "1",
            lstcarAt = "0"
        )
        val main = train("2638", "1002000211", "", "1002").copy(
            statnNm = "성수",
            routeSegmentId = "main"
        )
        val seongsu = train("2638", "1002000246", "", "1002").copy(
            statnNm = "신설동",
            routeSegmentId = "seongsu"
        )
        val sinjeong = train("2638", "1002000200", "", "1002").copy(
            statnNm = "까치산",
            routeSegmentId = "sinjeong"
        )

        val result = mergeLinePositions(
            topisPositions = listOf(topis),
            seoulMetroPositions = listOf(main, seongsu, sinjeong)
        )

        assertEquals(listOf("main", "seongsu", "sinjeong"), result.map { it.routeSegmentId })
        assertEquals(listOf("성수", "신설동", "까치산"), result.map { it.statnNm })
        assertEquals(listOf("1", "1", "1"), result.map { it.directAt })
        assertEquals(listOf("0", "0", "0"), result.map { it.lstcarAt })
        assertEquals(
            listOf("2026-09-15 17:30:00", "2026-09-15 17:30:00", "2026-09-15 17:30:00"),
            result.map { it.recptnDt }
        )
    }

    @Test
    fun `TOPIS 역 ID가 asset에 없으면 역명으로 복구한다`() {
        val topis = train("0712", "1001080175", "2026-09-12 22:00:00", "1001").copy(
            statnNm = "온양온천"
        )

        val result = repairUnmappedStationIds(
            positions = listOf(topis),
            isMapped = { it == "1001080176" },
            stationIdForName = { name -> if (name == "온양온천") "1001080176" else null }
        )

        assertEquals("1001080176", result.single().statnId)
    }

    @Test
    fun `같은 열번의 이전 위치 대신 최신 위치만 남긴다`() {
        val stale = train(
            number = "4070",
            stationId = "1004000431",
            receivedAt = "2026-09-12 12:33:04"
        )
        val latest = train(
            number = "4070",
            stationId = "1004000430",
            receivedAt = "2026-09-12 12:37:30"
        )

        val result = latestPositionPerTrain(listOf(stale, latest))

        assertEquals(1, result.size)
        assertSame(latest, result.single())
    }

    @Test
    fun `같은 열번의 완전 중복은 하나만 남긴다`() {
        val duplicate = train(
            number = "4568",
            stationId = "1004000433",
            receivedAt = "2026-09-12 12:37:07"
        )

        val result = latestPositionPerTrain(listOf(duplicate, duplicate))

        assertEquals(listOf(duplicate), result)
    }

    @Test
    fun `열번이 같아도 호선이 다르면 합치지 않는다`() {
        val lineFour = train("4312", "1004000432", "2026-09-12 12:37:29", "1004")
        val lineFive = train("4312", "1005000525", "2026-09-12 12:37:30", "1005")

        assertEquals(
            listOf(lineFour, lineFive),
            latestPositionPerTrain(listOf(lineFour, lineFive))
        )
    }

    @Test
    fun `열번이 없는 항목은 임의로 합치지 않는다`() {
        val first = train(null, "1004000431", "2026-09-12 12:33:04")
        val second = train(null, "1004000432", "2026-09-12 12:37:29")

        assertEquals(
            listOf(first, second),
            latestPositionPerTrain(listOf(first, second))
        )
    }

    private fun train(
        number: String?,
        stationId: String,
        receivedAt: String,
        subwayId: String = "1004"
    ) = SubwayPositionDto(
        subwayId = subwayId,
        statnId = stationId,
        trainNo = number,
        recptnDt = receivedAt
    )
}
