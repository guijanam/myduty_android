package com.sonbum.diacalendar2.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class SubwayStationNameTest {

    @Test
    fun `서울교통공사 역명의 괄호 별칭과 역 접미사를 정규화한다`() {
        assertEquals("총신대입구", SubwayStationRegistry.normalizeStationName("총신대입구(이수)"))
        assertEquals("서울", SubwayStationRegistry.normalizeStationName("서울역"))
        assertEquals("서울", SubwayStationRegistry.normalizeStationName(" 서 울 "))
    }
}
