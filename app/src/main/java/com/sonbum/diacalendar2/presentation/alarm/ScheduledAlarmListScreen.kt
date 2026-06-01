package com.sonbum.diacalendar2.presentation.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledAlarmListScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScheduledAlarmListViewModel = koinViewModel()
) {
    val alarms by viewModel.alarms.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("예정된 알람 (5일)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "예정된 알람이 없습니다.\n설정에서 근무 알람을 켜주세요.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        val grouped = alarms.groupBy { it.date }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            grouped.forEach { (date, dayAlarms) ->
                item(key = "header_$date") {
                    Text(
                        text = formatDateHeader(date),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(dayAlarms, key = { "${it.date}_${it.slot}" }) { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        onToggle = { enabled ->
                            viewModel.setEnabled(alarm.date, alarm.slot, enabled)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: ScheduledAlarmUi, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${alarm.timeText}  ·  ${alarm.slotLabel}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "근무: ${alarm.shiftName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

private fun formatDateHeader(date: String): String = try {
    val d = LocalDate.parse(date)
    val dow = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    d.format(DateTimeFormatter.ofPattern("M월 d일")) + " ($dow)"
} catch (_: Exception) {
    date
}
