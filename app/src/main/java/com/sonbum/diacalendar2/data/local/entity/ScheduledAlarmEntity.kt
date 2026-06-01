package com.sonbum.diacalendar2.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * 예정된 근무 알람 1건. (date, slot)이 유일.
 * - 워커가 5일치 유효교번을 계산해 이 테이블을 단일 출처로 갱신한다.
 * - dismissed=true 이면 사용자가 개별로 끈 알람 → 재등록 시에도 건너뛴다.
 */
@Entity(
    tableName = "scheduled_alarms",
    indices = [Index(value = ["date", "slot"], unique = true)]
)
data class ScheduledAlarmEntity(
    val date: String,            // yyyy-MM-dd
    val slot: Int,               // 0=출근, 1=전반, 2=후반
    val shiftName: String,       // 유효교번 (스냅샷)
    val timeText: String,        // 기준 시각 HH:mm (스냅샷)
    val triggerAtMillis: Long,   // 실제 발화 시각 (분전 적용 후)
    val dismissed: Boolean = false
) {
    // (date, slot)로 안정적 PK 생성 — 위젯/스케줄러 request code와 정합
    @androidx.room.PrimaryKey
    var id: Long = makeId(date, slot)

    companion object {
        fun makeId(date: String, slot: Int): Long =
            slot * 1_000_000_000L + date.hashCode().toLong().and(0xFFFFFFFFL)
    }
}
