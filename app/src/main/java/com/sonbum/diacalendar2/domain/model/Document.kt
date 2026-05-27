package com.sonbum.diacalendar2.domain.model

data class Document(
    val id: String,
    val title: String,
    val description: String,
    val fileUrl: String?,
    val fileName: String?,
    val isRequired: Boolean,
    val expiresAt: String?,
    val createdAt: String
)
