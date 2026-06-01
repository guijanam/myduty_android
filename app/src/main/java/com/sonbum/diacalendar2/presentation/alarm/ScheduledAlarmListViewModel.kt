package com.sonbum.diacalendar2.presentation.alarm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.core.notification.AlarmScheduler
import com.sonbum.diacalendar2.core.notification.ShiftReminderWorker
import com.sonbum.diacalendar2.data.local.dao.ScheduledAlarmDao
import com.sonbum.diacalendar2.data.local.entity.ScheduledAlarmEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduledAlarmUi(
    val date: String,
    val slot: Int,
    val slotLabel: String,
    val shiftName: String,
    val timeText: String,       // 근무 시각 (HH:mm)
    val alarmTimeText: String,  // 실제 알람이 울릴 시각 (분전 적용, HH:mm)
    val enabled: Boolean        // !dismissed
)

class ScheduledAlarmListViewModel(
    private val dao: ScheduledAlarmDao,
    private val alarmScheduler: AlarmScheduler,
    private val appContext: Context
) : ViewModel() {

    val alarms: StateFlow<List<ScheduledAlarmUi>> = dao.observeAll()
        .map { list -> list.map { it.toUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // 화면 진입 시 최신 상태로 한번 재계산
        ShiftReminderWorker.enqueue(appContext)
    }

    /** 개별 on/off. 끄면 그 알람만 즉시 해제, 켜면 워커가 재등록 */
    fun setEnabled(date: String, slot: Int, enabled: Boolean) {
        viewModelScope.launch {
            dao.setDismissed(date, slot, dismissed = !enabled)
            if (!enabled) {
                alarmScheduler.cancelShiftAlarm(date, slot)
            } else {
                ShiftReminderWorker.enqueue(appContext)
            }
        }
    }

    private fun ScheduledAlarmEntity.toUi() = ScheduledAlarmUi(
        date = date,
        slot = slot,
        slotLabel = when (slot) {
            AlarmScheduler.SLOT_FIRST -> "전반사업"
            AlarmScheduler.SLOT_SECOND -> "후반사업"
            else -> "출근"
        },
        shiftName = shiftName,
        timeText = timeText,
        alarmTimeText = formatAlarmTime(triggerAtMillis),
        enabled = !dismissed
    )

    private fun formatAlarmTime(millis: Long): String =
        java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
}
