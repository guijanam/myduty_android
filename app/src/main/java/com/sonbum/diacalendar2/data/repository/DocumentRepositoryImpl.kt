package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.remote.SupabaseConfig
import com.sonbum.diacalendar2.data.remote.api.SupabaseApi
import com.sonbum.diacalendar2.domain.model.Document
import com.sonbum.diacalendar2.domain.repository.DocumentRepository

class DocumentRepositoryImpl(
    private val api: SupabaseApi
) : DocumentRepository {

    private val apiKey = SupabaseConfig.apiKey
    private val authHeader get() = "Bearer $apiKey"

    override suspend fun getDocuments(): Result<List<Document>> = runCatching {
        api.getDocuments(apiKey, authHeader).map { it.toDomain() }
    }

    override suspend fun getDocument(id: String): Result<Document> = runCatching {
        api.getDocument(apiKey, authHeader, idFilter = "eq.$id")
            .firstOrNull()?.toDomain()
            ?: error("공지를 찾을 수 없습니다")
    }
}
