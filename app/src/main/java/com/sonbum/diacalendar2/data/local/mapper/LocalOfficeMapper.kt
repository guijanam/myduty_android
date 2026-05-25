package com.sonbum.diacalendar2.data.local.mapper

import com.sonbum.diacalendar2.data.local.entity.LocalOfficeEntity
import com.sonbum.diacalendar2.domain.model.LocalOffice
import com.sonbum.diacalendar2.domain.model.Office

// Entity → Domain
fun LocalOfficeEntity.toDomain(): LocalOffice {
    return LocalOffice(
        id = id,
        officeName = officeName,
        diaTurns1 = diaTurns1,
        diaTurns2 = diaTurns2,
        subTurns = subTurns,
        diaSelects = diaSelects,
        diaTurns3 = diaTurns3,
        createdAt = createdAt
    )
}

// Domain → Entity
fun LocalOffice.toEntity(): LocalOfficeEntity {
    return LocalOfficeEntity(
        id = id,
        officeName = officeName,
        diaTurns1 = diaTurns1,
        diaTurns2 = diaTurns2,
        subTurns = subTurns,
        diaSelects = diaSelects,
        diaTurns3 = diaTurns3,
        createdAt = createdAt
    )
}

// LocalOffice → Office (ShiftSelectionScreen 호환용)
// officeCode에 음수 id를 사용하여 서버 PK와 구분
fun LocalOffice.toOffice(): Office {
    return Office(
        officeCode = -id,
        officeName = officeName,
        diaTurns1 = diaTurns1,
        diaTurns2 = diaTurns2,
        subTurns = subTurns,
        diaSelects = diaSelects,
        diaTurns3 = diaTurns3,
        adminPassword = null
    )
}
