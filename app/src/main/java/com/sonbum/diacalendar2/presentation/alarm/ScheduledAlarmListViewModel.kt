package com.sonbum.diacalendar2.presentation.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.core.notification.AlarmScheduler
import com.sonbum.diacalendar2.core.notification.ShiftReminderWorker
import com.sonbum.diacalendar2.data.local.dao.ScheduledAlarmDao
import com.sonbum.diacalendar2.data.local.datastore.NotificationPreferences
import com.sonbum.diacalendar2.data.local.entity.ScheduledAlarmEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun triggerAtSelectedTime(
    currentTriggerAtMillis: Long,
    selectedTime: LocalTime,
    zoneId: ZoneId
): Long = Instant.ofEpochMilli(currentTriggerAtMillis)
    .atZone(zoneId)
    .toLocalDate()
    .atTime(selectedTime)
    .atZone(zoneId)
    .toInstant()
    .toEpochMilli()

data class ScheduledAlarmUi(
    val date: String,
    val slot: Int,
    val slotLabel: String,
    val shiftName: String,
    val timeText: String,       // 근무 시각 (HH:mm)
    val alarmTimeText: String,  // 실제 알람이 울릴 시각 (분전 적용, HH:mm)
    val triggerAtMillis: Long,
    val customTime: Boolean,
    val soundUri: String?,
    val soundTitle: String,
    val enabled: Boolean        // !dismissed
)

class ScheduledAlarmListViewModel(
    private val dao: ScheduledAlarmDao,
    private val alarmScheduler: AlarmScheduler,
    private val notificationPreferences: NotificationPreferences,
    private val appContext: Context
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

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

    /** 선택한 시각을 해당 알람의 현재 발화 날짜에 적용하고 즉시 재등록한다. */
    fun setAlarmTime(date: String, slot: Int, hour: Int, minute: Int) {
        viewModelScope.launch {
            val alarm = dao.getByDateSlot(date, slot) ?: return@launch
            val selectedTime = runCatching { LocalTime.of(hour, minute) }.getOrNull()
                ?: return@launch
            val triggerAtMillis = triggerAtSelectedTime(
                currentTriggerAtMillis = alarm.triggerAtMillis,
                selectedTime = selectedTime,
                zoneId = ZoneId.systemDefault()
            )
            if (triggerAtMillis <= System.currentTimeMillis()) {
                _messages.emit("지난 시간으로는 알람을 설정할 수 없습니다.")
                return@launch
            }

            val updated = alarm.copy(
                triggerAtMillis = triggerAtMillis,
                customTriggerAtMillis = triggerAtMillis
            )
            dao.upsert(updated)
            reschedule(updated)
        }
    }

    /** 시스템 알람음 선택기에서 고른 URI를 저장하고 예약 인텐트에 즉시 반영한다. */
    fun setAlarmSound(date: String, slot: Int, soundUri: String) {
        viewModelScope.launch {
            val alarm = dao.getByDateSlot(date, slot) ?: return@launch
            val updated = alarm.copy(soundUri = soundUri)
            dao.upsert(updated)
            reschedule(updated)
        }
    }

    private suspend fun reschedule(alarm: ScheduledAlarmEntity) {
        alarmScheduler.cancelShiftAlarm(alarm.date, alarm.slot)
        if (alarm.dismissed || alarm.triggerAtMillis <= System.currentTimeMillis()) return

        val prefs = notificationPreferences.workAlarmPrefs.first()
        alarmScheduler.scheduleShiftAlarm(
            dateString = alarm.date,
            shiftName = alarm.shiftName,
            triggerAtMillis = alarm.triggerAtMillis,
            slot = alarm.slot,
            fullScreen = prefs.fullScreen,
            sound = prefs.sound,
            soundUri = alarm.soundUri,
            vibrate = prefs.vibrate
        )
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
        triggerAtMillis = triggerAtMillis,
        customTime = customTriggerAtMillis != null,
        soundUri = soundUri,
        soundTitle = resolveSoundTitle(soundUri),
        enabled = !dismissed
    )

    private fun formatAlarmTime(millis: Long): String =
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm"))

    private fun resolveSoundTitle(soundUri: String?): String {
        val uri = soundUri
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getActualDefaultRingtoneUri(
                appContext,
                RingtoneManager.TYPE_ALARM
            )
        return uri?.let {
            runCatching {
                RingtoneManager.getRingtone(appContext, it)?.getTitle(appContext)
            }.getOrNull()
        } ?: "기본 알람음"
    }
}
