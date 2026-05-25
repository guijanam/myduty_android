package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.remote.MenuSupabaseConfig
import com.sonbum.diacalendar2.data.remote.api.SupabaseMenuApi
import com.sonbum.diacalendar2.domain.model.CafeteriaMenu
import com.sonbum.diacalendar2.domain.repository.MenuRepository

class MenuRepositoryImpl(
    private val menuApi: SupabaseMenuApi
) : MenuRepository {

    override suspend fun getMenusForDate(dateString: String): Result<List<CafeteriaMenu>> {
        return try {
            val response = menuApi.getMenuForDate(
                apiKey = MenuSupabaseConfig.apiKey,
                authorization = "Bearer ${MenuSupabaseConfig.apiKey}",
                startDateFilter = "lte.$dateString",
                endDateFilter = "gte.$dateString"
            )

            val menus = response.mapNotNull { analysis ->
                val dailyMenu = analysis.weeklyMenus?.find { it.date == dateString }
                    ?: return@mapNotNull null

                CafeteriaMenu(
                    cafeteriaName = analysis.cafeteriaName ?: "알 수 없는 식당",
                    date = dailyMenu.date,
                    dayOfWeek = dailyMenu.dayOfWeek ?: "",
                    breakfast = dailyMenu.meals?.breakfast ?: emptyList(),
                    lunch = dailyMenu.meals?.lunch ?: emptyList(),
                    dinner = dailyMenu.meals?.dinner ?: emptyList()
                )
            }

            Result.success(menus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllCafeteriaNames(): Result<List<String>> {
        return try {
            val response = menuApi.getAllMenuAnalyses(
                apiKey = MenuSupabaseConfig.apiKey,
                authorization = "Bearer ${MenuSupabaseConfig.apiKey}"
            )
            val names = response.mapNotNull { it.cafeteriaName }.distinct().sorted()
            Result.success(names)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
