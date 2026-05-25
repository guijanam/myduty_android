package com.sonbum.diacalendar2.data.local.mapper

import com.sonbum.diacalendar2.data.local.entity.OfficeEntity
import com.sonbum.diacalendar2.data.remote.dto.OfficeDto
import com.sonbum.diacalendar2.domain.model.Office

// DTO → Entity
fun OfficeDto.toEntity(): OfficeEntity {
    return OfficeEntity(
        officeCode = officeCode,
        officeName = officeName,
        diaTurns1 = diaTurns1,
        diaTurns2 = diaTurns2,
        subTurns = subTurns,
        diaSelects = diaSelects,
        diaTurns3 = diaTurns3,
        adminPassword = adminPassword
    )
}

// Entity → Domain
fun OfficeEntity.toDomain(): Office {
    return Office(
        officeCode = officeCode,
        officeName = officeName,
        diaTurns1 = diaTurns1,
        diaTurns2 = diaTurns2,
        subTurns = subTurns,
        diaSelects = diaSelects,
        diaTurns3 = diaTurns3,
        adminPassword = adminPassword
    )
}

// Domain → Entity
fun Office.toEntity(): OfficeEntity {
    return OfficeEntity(
        officeCode = officeCode,
        officeName = officeName,
        diaTurns1 = diaTurns1,
        diaTurns2 = diaTurns2,
        subTurns = subTurns,
        diaSelects = diaSelects,
        diaTurns3 = diaTurns3,
        adminPassword = adminPassword
    )
}
