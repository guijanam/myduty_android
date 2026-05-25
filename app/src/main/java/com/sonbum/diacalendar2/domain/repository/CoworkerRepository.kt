package com.sonbum.diacalendar2.domain.repository

import com.sonbum.diacalendar2.domain.model.Coworker
import com.sonbum.diacalendar2.domain.model.CoworkerGroup
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CoworkerRepository {
    fun getAllCoworkers(): Flow<List<Coworker>>
    fun getAllGroups(): Flow<List<CoworkerGroup>>
    suspend fun getCoworkerById(id: Long): Coworker?
    suspend fun saveCoworker(coworker: Coworker): Long
    suspend fun deleteCoworker(id: Long)
    suspend fun updateCoworkerSortOrders(ordered: List<Coworker>)
    suspend fun saveGroup(group: CoworkerGroup): Long
    suspend fun updateGroup(group: CoworkerGroup)
    /** 그룹 삭제 시 해당 그룹을 참조하는 동료의 groupIds에서도 제거 */
    suspend fun deleteGroup(id: Long)
    /** 런타임 계산 (DB 저장 없음) — 해당 월의 날짜→근무명 Map 반환 */
    fun calculateScheduleForMonth(coworker: Coworker, year: Int, month: Int): Map<LocalDate, String>
}
