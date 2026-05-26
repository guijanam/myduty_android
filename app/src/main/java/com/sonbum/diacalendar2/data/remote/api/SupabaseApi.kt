package com.sonbum.diacalendar2.data.remote.api

import com.sonbum.diacalendar2.data.remote.dto.DiaDto
import com.sonbum.diacalendar2.data.remote.dto.HolidayDto
import com.sonbum.diacalendar2.data.remote.dto.OfficeDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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

    // coworker_list.device_id로 VIP 여부만 조회 (true/false 반환)
    @POST("rpc/check_device_vip")
    suspend fun checkDeviceVip(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>  // {"p_device_id": "<ssaid>"}
    ): Boolean
}
