package com.sonbum.diacalendar2.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DiaDto(
    val id: Long,
    @SerializedName("dia_id")
    val diaId: String,
    @SerializedName("office_name")
    val officeName: String,
    @SerializedName("office_id")
    val officeId: Int?,
    @SerializedName("type_name")
    val typeName: String?,
    @SerializedName("first_time")
    val firstTime: String?,
    @SerializedName("num_tr1")
    val numTr1: String?,
    @SerializedName("num_tr2")
    val numTr2: String?,
    @SerializedName("second_time")
    val secondTime: String?,
    @SerializedName("third_time")
    val thirdTime: String?,
    @SerializedName("total_time")
    val totalTime: String?,
    @SerializedName("work_time")
    val workTime: String?
)
