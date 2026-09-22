package com.sonbum.diacalendar2.data.remote.parser

import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionDto

/** 서울교통공사 노선도 HTML에서 숫자 열번인 공사 구간 열차만 추출한다. */
internal object SeoulMetroTrainParser {

    fun parse(
        html: String,
        line: Int,
        stationIdForName: (String) -> String?
    ): List<SubwayPositionDto> {
        if (line !in 1..8) return emptyList()
        val metroHtml = metroSection(html, line) ?: return emptyList()

        return TRAIN_DIV.findAll(metroHtml).mapNotNull { match ->
            val className = match.groupValues[1]
            val trainNo = match.groupValues[2]
            val detail = DETAILS.matchEntire(match.groupValues[3].trim()) ?: return@mapNotNull null
            val stationName = detail.groupValues[1].trim()
            val stationId = stationIdForName(stationName) ?: return@mapNotNull null
            val directionCode = directionCode(className) ?: return@mapNotNull null
            val direction = direction(directionCode, line) ?: return@mapNotNull null

            SubwayPositionDto(
                subwayId = (1000 + line).toString(),
                subwayNm = "${line}호선",
                statnId = stationId,
                statnNm = stationName,
                trainNo = trainNo,
                updnLine = direction,
                statnTnm = destination(detail.groupValues[3], line),
                trainSttus = STATUS_CODES[detail.groupValues[2]],
                routeSegmentId = routeSegmentId(directionCode, line)
            )
        }.toList()
    }

    private fun metroSection(html: String, line: Int): String? {
        val marker = "<div class=\"${line}line_metro\">"
        val start = html.indexOf(marker)
        if (start < 0) return null

        val section = html.substring(start + marker.length)
        val korailMarker = "<div class=\"${line}line_korail\">"
        val end = section.indexOf(korailMarker)
        return if (end >= 0) section.substring(0, end) else section
    }

    /**
     * 1~4호선 CSS 방향 코드는 1/2(본선), 3/4·5/6(2호선 지선)이고
     * 5~8호선은 마지막 코드가 TOPIS와 같은 0/1이다.
     */
    private fun directionCode(className: String): Int? {
        val cssClass = className.substringBefore(' ')
        val parts = cssClass.split('_')
        return parts.getOrNull(parts.lastIndex - 1)?.toIntOrNull()
    }

    private fun direction(raw: Int, line: Int): String? {
        return if (line <= 4) {
            if (raw % 2 == 1) "0" else "1"
        } else {
            raw.takeIf { it == 0 || it == 1 }?.toString()
        }
    }

    /** 2호선 HTML 방향코드: 1/2 본선, 3/4 성수지선, 5/6 신정지선. */
    private fun routeSegmentId(raw: Int, line: Int): String? =
        if (line != 2) null else when (raw) {
            1, 2 -> "main"
            3, 4 -> "seongsu"
            5, 6 -> "sinjeong"
            else -> null
        }

    private fun destination(raw: String, line: Int): String {
        val value = raw.trim()
        if (line == 2 && value in LOOP_DIRECTIONS) return "성수종착"
        return value.removeSuffix("행")
    }

    private val TRAIN_DIV = Regex(
        pattern = """<div\s+class="([^"]*\btip\b[^"]*)"[^>]*\btitle="(\d+)열차\s+([^"]+)"[^>]*>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val DETAILS = Regex("""^(.+?)\s+(접근|도착|출발|이동)\s+(.+)$""")
    private val STATUS_CODES = mapOf(
        "접근" to "0",
        "도착" to "1",
        "출발" to "2",
        "이동" to "3"
    )
    private val LOOP_DIRECTIONS = setOf("내선순환", "외선순환")
}
