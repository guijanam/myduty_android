package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.Anniversary
import kotlinx.coroutines.flow.Flow

interface AnniversaryRepository {
    fun getAll(): Flow<List<Anniversary>>
    suspend fun add(anniversary: Anniversary)
    suspend fun update(anniversary: Anniversary)
    suspend fun delete(id: Long)
    // 특정 연도의 기념일을 양력 날짜("yyyy-MM-dd") → 기념일 이름 맵으로 반환
    suspend fun getAnniversaryMapForYear(year: Int): Map<String, String>
}
