package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.CafeteriaMenu

interface MenuRepository {
    suspend fun getMenusForDate(dateString: String): Result<List<CafeteriaMenu>>
    suspend fun getAllCafeteriaNames(): Result<List<String>>
}
