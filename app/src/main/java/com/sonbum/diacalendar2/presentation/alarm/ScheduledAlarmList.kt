package com.sonbum.diacalendar2.presentation.alarm

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 예정된 알람 목록을 다른 화면(근무 알람 설정)에서도 그대로 쓰기 위한 LazyColumn 아이템들.
 * 날짜별로 묶어 헤더 + 알람 카드를 넣는다.
 */
fun LazyListScope.scheduledAlarmItems(
    alarms: List<ScheduledAlarmUi>,
    onToggle: (ScheduledAlarmUi, Boolean) -> Unit,
    onEditTime: (ScheduledAlarmUi) -> Unit,
    onEditSound: (ScheduledAlarmUi) -> Unit
) {
    alarms.groupBy { it.date }.forEach { (date, dayAlarms) ->
        item(key = "header_$date") {
            Text(
                text = formatDateHeader(date),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
        }
        items(dayAlarms, key = { "${it.date}_${it.slot}" }) { alarm ->
            AlarmRow(
                alarm = alarm,
                onToggle = { enabled -> onToggle(alarm, enabled) },
                onEditTime = { onEditTime(alarm) },
                onEditSound = { onEditSound(alarm) }
            )
        }
    }
}

/**
 * 알람 시각/알람음 편집(시간 선택 다이얼로그 + 시스템 알람음 선택기)을 담당하는 래퍼.
 * content에 편집 요청 콜백 두 개를 넘겨주므로, 목록을 어디에 그리든 편집 동작은 동일하다.
 */
@Composable
fun ScheduledAlarmEditing(
    viewModel: ScheduledAlarmListViewModel,
    content: @Composable (
        onEditTime: (ScheduledAlarmUi) -> Unit,
        onEditSound: (ScheduledAlarmUi) -> Unit
    ) -> Unit
) {
    var timeEditAlarm by remember { mutableStateOf<ScheduledAlarmUi?>(null) }
    var soundEditAlarm by remember { mutableStateOf<ScheduledAlarmUi?>(null) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val alarm = soundEditAlarm
        val pickedUri = result.data?.pickedRingtoneUri()
        if (result.resultCode == Activity.RESULT_OK && alarm != null && pickedUri != null) {
            viewModel.setAlarmSound(alarm.date, alarm.slot, pickedUri.toString())
        }
        soundEditAlarm = null
    }

    content(
        { alarm -> timeEditAlarm = alarm },
        { alarm ->
            soundEditAlarm = alarm
            ringtonePickerLauncher.launch(alarm.ringtonePickerIntent())
        }
    )

    timeEditAlarm?.let { alarm ->
        AlarmTimePickerDialog(
            initialTime = Instant.ofEpochMilli(alarm.triggerAtMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalTime(),
            onDismiss = { timeEditAlarm = null },
            onConfirm = { time ->
                viewModel.setAlarmTime(alarm.date, alarm.slot, time.hour, time.minute)
                timeEditAlarm = null
            }
        )
    }
}

@Composable
private fun AlarmRow(
    alarm: ScheduledAlarmUi,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit,
    onEditSound: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TextButton(
                    onClick = onEditTime,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${alarm.alarmTimeText} 알람",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // 출근 시각과 근무명은 노안 사용자도 읽을 수 있게 본문 크기로 키운다.
                Text(
                    text = "${alarm.slotLabel} ${alarm.timeText}(근무:${alarm.shiftName})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (alarm.customTime) {
                    Text(
                        text = "사용자 지정 시각",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    onClick = onEditSound,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = alarm.soundTitle,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
        title = { Text("알람 시각 변경") },
        text = { TimePicker(state = state) }
    )
}

private fun ScheduledAlarmUi.ringtonePickerIntent(): Intent {
    val existingUri = soundUri
        ?.takeIf { it.isNotBlank() }
        ?.let(Uri::parse)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "알람음 선택")
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        putExtra(
            RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        )
        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
    }
}

private fun Intent.pickedRingtoneUri(): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }

private fun formatDateHeader(date: String): String = try {
    val d = LocalDate.parse(date)
    val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    d.format(DateTimeFormatter.ofPattern("M월 d일")) + " ($dow)"
} catch (_: Exception) {
    date
}
