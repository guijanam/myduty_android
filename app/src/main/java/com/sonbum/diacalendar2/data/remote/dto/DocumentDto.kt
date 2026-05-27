package com.sonbum.diacalendar2.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.sonbum.diacalendar2.domain.model.Document

data class DocumentDto(
    val id: String,
    val title: String,
    val description: String?,
    @SerializedName("file_url") val fileUrl: String?,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("is_required") val isRequired: Boolean = false,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("created_at") val createdAt: String
) {
    fun toDomain() = Document(
        id = id,
        title = title,
        description = description ?: "",
        fileUrl = fileUrl,
        fileName = fileName,
        isRequired = isRequired,
        expiresAt = expiresAt,
        createdAt = createdAt
    )
}
