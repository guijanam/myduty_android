package com.sonbum.diacalendar2.data.remote.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class SeoulMetroTrainParserTest {

    @Test
    fun `4호선 공사 열차를 DTO로 변환하고 코레일 열차는 제외한다`() {
        val html = """
            <div class="4line_metro">
              <div class="T0408_Y_2_v2 tip"
                   title="4169열차  별내별가람 도착 사당행"
                   data-statnTcd="0433"></div>
            </div>
            <div class="4line_korail">
              <div class="T1756_Y_2_v2 tip"
                   title="K4327열차  중앙 도착 안산행"></div>
            </div>
        """.trimIndent()

        val result = SeoulMetroTrainParser.parse(html, 4) { name ->
            if (name == "별내별가람") "1004000408" else null
        }

        assertEquals(1, result.size)
        assertEquals("4169", result.single().trainNo)
        assertEquals("1004000408", result.single().statnId)
        assertEquals("1", result.single().updnLine)
        assertEquals("1", result.single().trainSttus)
        assertEquals("사당", result.single().statnTnm)
    }

    @Test
    fun `2호선 지선 방향과 순환선 종착을 변환한다`() {
        val html = """
            <div class="2line_metro">
              <div class="T0240_Y_1_v2 tip"
                   title="2390열차  신촌 도착 내선순환"></div>
              <div class="T0246_Y_4_v2 tip"
                   title="2685열차  신설동 출발 신설동행"></div>
              <div class="T0234_Y_6_v2 tip"
                   title="2553열차  신도림 도착 신도림행"></div>
            </div>
        """.trimIndent()

        val stations = mapOf(
            "신촌" to "1002000240",
            "신설동" to "1002002114",
            "신도림" to "1002000234"
        )
        val result = SeoulMetroTrainParser.parse(html, 2, stations::get)

        assertEquals(listOf("0", "1", "1"), result.map { it.updnLine })
        assertEquals(listOf("성수종착", "신설동", "신도림"), result.map { it.statnTnm })
        assertEquals(listOf("1", "2", "1"), result.map { it.trainSttus })
        assertEquals(listOf("main", "seongsu", "sinjeong"), result.map { it.routeSegmentId })
    }

    @Test
    fun `5호선 이후 CSS 방향 코드는 TOPIS 0과 1을 그대로 사용한다`() {
        val html = """
            <div class="5line_metro">
              <div class="T2517_1_0_v2 tip"
                   title="5152열차  우장산 도착 방화행"></div>
              <div class="T2517_1_1_v2 tip"
                   title="5665열차  우장산 도착 마천행"></div>
            </div>
        """.trimIndent()

        val result = SeoulMetroTrainParser.parse(html, 5) { "1005000516" }

        assertEquals(listOf("0", "1"), result.map { it.updnLine })
        assertEquals(listOf("방화", "마천"), result.map { it.statnTnm })
    }

    @Test
    fun `역을 찾지 못한 항목은 기존 TOPIS 데이터를 덮지 않도록 제외한다`() {
        val html = """
            <div class="7line_metro">
              <div class="T9999_1_0_v2 tip"
                   title="7314열차  알수없는역 도착 도봉산행"></div>
            </div>
        """.trimIndent()

        assertEquals(emptyList<Any>(), SeoulMetroTrainParser.parse(html, 7) { null })
    }
}
