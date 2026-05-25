package com.sonbum.diacalendar2.data.remote.api

import com.sonbum.diacalendar2.data.remote.dto.DiaDto
import com.sonbum.diacalendar2.data.remote.dto.HolidayDto
import com.sonbum.diacalendar2.data.remote.dto.OfficeDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SupabaseApi {

    @GET("holidays?select=*")
    suspend fun getHolidays(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): List<HolidayDto>

    @GET("office?select=*")
    suspend fun getOffices(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): List<OfficeDto>

    @GET("dia?select=*")
    suspend fun getDias(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): List<DiaDto>

    @GET("dia")
    suspend fun getDiasByOfficeId(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("office_id") officeIdFilter: String,  // eq.{officeId} 형식
        @Query("select") select: String = "*"
    ): List<DiaDto>
}
