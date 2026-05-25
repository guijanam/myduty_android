package com.sonbum.diacalendar2.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OfficeDto(
    @SerializedName("office_name")
    val officeName: String,
    @SerializedName("office_code")
    val officeCode: Long,
    @SerializedName("dia_turns1")
    val diaTurns1: String?,
    @SerializedName("dia_turns2")
    val diaTurns2: String?,
    @SerializedName("sub_turns")
    val subTurns: String?,
    @SerializedName("dia_selects")
    val diaSelects: String?,
    @SerializedName("dia_turns3")
    val diaTurns3: String?,  // jsonb -> String으로 받음
    @SerializedName("admin_password")
    val adminPassword: String?
)
