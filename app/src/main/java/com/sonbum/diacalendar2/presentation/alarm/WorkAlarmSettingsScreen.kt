package com.sonbum.diacalendar2.presentation.alarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkAlarmSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToList: () -> Unit = {},
    viewModel: WorkAlarmSettingsViewModel = koinViewModel()
) {
    val s by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("근무 알람") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToList) { Text("예정된 알람") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExactAlarmWarning(context)

            AlarmSlotCard(
                title = "출근 알람",
                subtitle = "출근 시각(workTime) 기준",
                enabled = s.commuteEnabled,
                minutesBefore = s.commuteMinutesBefore,
                onToggle = { viewModel.setCommute(it, s.commuteMinutesBefore) },
                onMinutes = { viewModel.setCommute(s.commuteEnabled, it) }
            )
            AlarmSlotCard(
                title = "전반사업 알람",
                subtitle = "전반 작업 시각(firstTime) 기준",
                enabled = s.firstEnabled,
                minutesBefore = s.firstMinutesBefore,
                onToggle = { viewModel.setFirst(it, s.firstMinutesBefore) },
                onMinutes = { viewModel.setFirst(s.firstEnabled, it) }
            )
            AlarmSlotCard(
                title = "후반사업 알람",
                subtitle = "후반 작업 시각(secondTime) 기준",
                enabled = s.secondEnabled,
                minutesBefore = s.secondMinutesBefore,
                onToggle = { viewModel.setSecond(it, s.secondMinutesBefore) },
                onMinutes = { viewModel.setSecond(s.secondEnabled, it) }
            )

            IntensityCard(
                fullScreen = s.fullScreen,
                sound = s.sound,
                vibrate = s.vibrate,
                onChange = { fs, sd, vb -> viewModel.setIntensity(fs, sd, vb) }
            )

            Text(
                text = "비번·휴무처럼 해당 시각이 없는 근무는 자동으로 건너뜁니다.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlarmSlotCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    minutesBefore: Int,
    onToggle: (Boolean) -> Unit,
    onMinutes: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            if (enabled) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (minutesBefore == 0) "정시에 알람" else "${minutesBefore}분 전 알람",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = minutesBefore.toFloat(),
                    onValueChange = { onMinutes(it.toInt()) },
                    valueRange = 0f..120f,
                    steps = 23  // 0,5,10,...,120 (5분 단위)
                )
            }
        }
    }
}

@Composable
private fun IntensityCard(
    fullScreen: Boolean,
    sound: Boolean,
    vibrate: Boolean,
    onChange: (Boolean, Boolean, Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("알람 방식", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            ToggleRow("전체화면 알람 (화면 깨움)", fullScreen) { onChange(it, sound, vibrate) }
            ToggleRow("소리", sound) { onChange(fullScreen, it, vibrate) }
            ToggleRow("진동", vibrate) { onChange(fullScreen, sound, it) }
            if (!fullScreen) {
                Text(
                    "전체화면을 끄면 일반 알림으로만 표시됩니다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Android 12+ 정확 알람 권한이 꺼져 있으면 안내 + 설정 이동 */
@Composable
private fun ExactAlarmWarning(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (alarmManager.canScheduleExactAlarms()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "정확한 알람 권한이 필요합니다",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "이 권한이 없으면 알람이 제때 울리지 않을 수 있습니다.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            }) { Text("권한 설정 열기") }
        }
    }
}
