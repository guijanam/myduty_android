package com.sonbum.diacalendar2.data.repository

import com.github.usingsky.calendar.KoreanLunarCalendar
import com.sonbum.diacalendar2.data.local.dao.AnniversaryDao
import com.sonbum.diacalendar2.data.local.entity.AnniversaryEntity
import com.sonbum.diacalendar2.domain.model.Anniversary
import com.sonbum.diacalendar2.domain.repository.AnniversaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AnniversaryRepositoryImpl(
    private val dao: AnniversaryDao
) : AnniversaryRepository {

    override fun getAll(): Flow<List<Anniversary>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun add(anniversary: Anniversary) {
        dao.insert(anniversary.toEntity())
    }

    override suspend fun update(anniversary: Anniversary) {
        dao.update(anniversary.toEntity())
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun getAnniversaryMapForYear(year: Int): Map<String, String> {
        val list = dao.getAll().first()
        val result = mutableMapOf<String, String>()
        val lunar = KoreanLunarCalendar.getInstance()

        for (entity in list) {
            val dateKey: String? = if (entity.isLunar) {
                lunarToSolar(lunar, year, entity.month, entity.day)
            } else {
                val dayStr = entity.day.toString().padStart(2, '0')
                val monthStr = entity.month.toString().padStart(2, '0')
                "$year-$monthStr-$dayStr"
            }
            if (dateKey != null) {
                result[dateKey] = if (result.containsKey(dateKey))
                    "${result[dateKey]}, ${entity.name}"
                else
                    entity.name
            }
        }
        return result
    }

    private fun lunarToSolar(lunar: KoreanLunarCalendar, year: Int, month: Int, day: Int): String? {
        return try {
            lunar.setLunarDate(year, month, day, false)
            val sy = lunar.solarYear
            val sm = lunar.solarMonth
            val sd = lunar.solarDay
            if (sy == 0 && sm == 0 && sd == 0) null
            else "%04d-%02d-%02d".format(sy, sm, sd)
        } catch (e: Exception) {
            null
        }
    }

    private fun AnniversaryEntity.toDomain() = Anniversary(id, name, month, day, isLunar)
    private fun Anniversary.toEntity() = AnniversaryEntity(id, name, month, day, isLunar)
}
