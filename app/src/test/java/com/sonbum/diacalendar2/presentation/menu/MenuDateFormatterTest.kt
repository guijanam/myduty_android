package com.sonbum.diacalendar2.presentation.menu

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuDateFormatterTest {

    @Test
    fun validIsoDate_isFormattedInKorean() {
        assertEquals("9월 7일 월요일", formatMenuDate("2026-09-07"))
    }

    @Test
    fun invalidDate_isReturnedUnchanged() {
        assertEquals("날짜 미정", formatMenuDate("날짜 미정"))
    }

    @Test
    fun emptyDate_isReturnedUnchanged() {
        assertEquals("", formatMenuDate(""))
    }
}
