package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.Document

interface DocumentRepository {
    suspend fun getDocuments(): Result<List<Document>>
    suspend fun getDocument(id: String): Result<Document>
}
