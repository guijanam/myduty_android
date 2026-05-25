package com.sonbum.diacalendar2.data.local.mapper

import com.sonbum.diacalendar2.data.local.entity.DiaEntity
import com.sonbum.diacalendar2.data.remote.dto.DiaDto
import com.sonbum.diacalendar2.domain.model.Dia

// DTO → Entity
fun DiaDto.toEntity(): DiaEntity {
    return DiaEntity(
        id = id,
        diaId = diaId,
        officeName = officeName,
        officeId = officeId,
        typeName = typeName,
        firstTime = firstTime,
        numTr1 = numTr1,
        numTr2 = numTr2,
        secondTime = secondTime,
        thirdTime = thirdTime,
        totalTime = totalTime,
        workTime = workTime
    )
}

// Entity → Domain
fun DiaEntity.toDomain(): Dia {
    return Dia(
        id = id,
        diaId = diaId,
        officeName = officeName,
        officeId = officeId,
        typeName = typeName,
        firstTime = firstTime,
        numTr1 = numTr1,
        numTr2 = numTr2,
        secondTime = secondTime,
        thirdTime = thirdTime,
        totalTime = totalTime,
        workTime = workTime
    )
}

// Domain → Entity
fun Dia.toEntity(): DiaEntity {
    return DiaEntity(
        id = id,
        diaId = diaId,
        officeName = officeName,
        officeId = officeId,
        typeName = typeName,
        firstTime = firstTime,
        numTr1 = numTr1,
        numTr2 = numTr2,
        secondTime = secondTime,
        thirdTime = thirdTime,
        totalTime = totalTime,
        workTime = workTime
    )
}
