package com.sonbum.diacalendar2.data.local.mapper

import com.sonbum.diacalendar2.data.local.entity.LocalDiaEntity
import com.sonbum.diacalendar2.domain.model.Dia
import com.sonbum.diacalendar2.domain.model.LocalDia

// Entity → Domain
fun LocalDiaEntity.toDomain(): LocalDia {
    return LocalDia(
        id = id,
        diaId = diaId,
        localOfficeId = localOfficeId,
        officeName = officeName,
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
fun LocalDia.toEntity(): LocalDiaEntity {
    return LocalDiaEntity(
        id = id,
        diaId = diaId,
        localOfficeId = localOfficeId,
        officeName = officeName,
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

// LocalDia → Dia (DiaTable 호환용)
fun LocalDia.toDia(): Dia {
    return Dia(
        id = -id,
        diaId = diaId,
        officeName = officeName,
        officeId = null,
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
