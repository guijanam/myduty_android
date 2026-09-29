package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 열차 편성 기록 Entity
 *
 * (date, half) 당 여러 행 허용 (근무 중 편성 교체 대응).
 *
 * shiftName / numTr 은 스냅샷이다. Dia 조회는 날짜 키가 아니라
 * (diaId, officeName, typeName) 이므로, 나중에 Dia 가 수정되거나
 * 유효 교번이 교번교체/충당으로 바뀌어도 기록 당시 값이 보존되어야 한다.
 * numTr 은 없는 승무소(대기근무 등)도 있으므로 빈 문자열일 수 있다.
 */
@Entity(
    tableName = "train_formations",
    indices = [
        Index(value = ["date"]),
        Index(value = ["formationNo"])
    ]
)
data class TrainFormationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,            // yyyy-MM-dd 형식
    val half: String,            // "FIRST" | "SECOND"
    val formationNo: Int,        // 편성번호
    val note: String = "",       // 자유 메모
    val shiftName: String = "",  // 스냅샷: 기록 당시 유효 교번
    val numTr: String = "",      // 스냅샷: 기록 당시 해당 half 의 열번 (없을 수 있음)
    val sortOrder: Int = 0,      // 같은 (date, half) 내 순서 (편성 교체 순서)
    val createdAt: Long = System.currentTimeMillis()
)
