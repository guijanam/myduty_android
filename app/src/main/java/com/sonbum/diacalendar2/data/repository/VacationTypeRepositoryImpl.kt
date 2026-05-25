package com.sonbum.diacalendar2.data.repository

import com.sonbum.diacalendar2.data.local.dao.VacationTypeDao
import com.sonbum.diacalendar2.data.local.entity.VacationTypeEntity
import com.sonbum.diacalendar2.domain.model.VacationType
import com.sonbum.diacalendar2.domain.repository.VacationTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VacationTypeRepositoryImpl(
    private val vacationTypeDao: VacationTypeDao
) : VacationTypeRepository {

    companion object {
        // name to shortName
        val DEFAULT_VACATION_TYPES = listOf(
            "촉진연차" to "촉연",
            "연차" to "연차",
            "대체휴가" to "대휴",
            "학습휴가" to "학휴",
            "자녀돌봄휴가" to "돌휴",
            "청원휴가" to "청휴",
            "출산휴가" to "출휴",
            "보건휴가" to "보건",
	        "만근휴가" to "만휴",
	        "반차" to "반차",
	        "가연차" to "가연",
	        "반반차" to "반반",
	        "장기재직휴가" to "장재",
	        "임금피크휴가" to "임피",
            "임신검진동행휴가" to "동휴",
            "난임치료동행휴가" to "난휴",
	        "공가" to "공가",
	        "병가" to "병가",
	        "근무협조" to "근협",
	        "회행" to "회행"

        )
    }

    override fun getAllVacationTypes(): Flow<List<VacationType>> {
        return vacationTypeDao.getAllVacationTypes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addVacationType(name: String, shortName: String): Long {
        return vacationTypeDao.insert(
            VacationTypeEntity(
                name = name,
                shortName = shortName,
                isDefault = false
            )
        )
    }

    override suspend fun deleteVacationType(id: Long) {
        vacationTypeDao.deleteById(id)
    }

    override suspend fun updateShortName(id: Long, shortName: String) {
        vacationTypeDao.updateShortName(id, shortName)
    }

    override suspend fun updateVacationType(id: Long, name: String, shortName: String) {
        vacationTypeDao.updateNameAndShortName(id, name, shortName)
    }

    override suspend fun ensureDefaultsExist() {
        val count = vacationTypeDao.getCount()
        if (count == 0) {
            val defaults = DEFAULT_VACATION_TYPES.map { (name, short) ->
                VacationTypeEntity(name = name, shortName = short, isDefault = true)
            }
            vacationTypeDao.insertAll(defaults)
        }
    }

    private fun VacationTypeEntity.toDomain(): VacationType {
        return VacationType(
            id = id,
            name = name,
            shortName = shortName,
            isDefault = isDefault
        )
    }
}
