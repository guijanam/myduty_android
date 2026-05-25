package com.sonbum.diacalendar2.data.remote.api

import com.sonbum.diacalendar2.data.remote.dto.MenuAnalysisDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SupabaseMenuApi {

    @GET("rest/v1/menu_analyses")
    suspend fun getMenuForDate(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("start_date") startDateFilter: String,
        @Query("end_date") endDateFilter: String,
        @Query("select") select: String = "*"
    ): List<MenuAnalysisDto>

    @GET("rest/v1/menu_analyses")
    suspend fun getAllMenuAnalyses(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "cafeteria_name"
    ): List<MenuAnalysisDto>
}
