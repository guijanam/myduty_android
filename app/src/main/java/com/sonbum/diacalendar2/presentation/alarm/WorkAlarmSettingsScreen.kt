package com.sonbum.diacalendar2.presentation.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    viewModel: WorkAlarmSettingsViewModel = koinViewModel(),
    alarmListViewModel: ScheduledAlarmListViewModel = koinViewModel()
) {
    val s by viewModel.state.collectAsState()
    val context = LocalContext.current
    val alarms by alarmListViewModel.alarms.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(alarmListViewModel) {
        alarmListViewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("근무 알람") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        // 설정 카드와 예정된 알람 목록을 한 LazyColumn에 담아, 목록을 보기 위한 화면 이동을 없앴다.
        ScheduledAlarmEditing(viewModel = alarmListViewModel) { onEditTime, onEditSound ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "settings") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExactAlarmWarning(context)
                        if (s.fullScreen) {
                            FullScreenIntentWarning(context)
                            OverlayPermissionWarning(context)
                        }

                        AlarmSlotCard(
                            title = "출근 알람",
                            subtitle = "출근 시각 기준",
                            enabled = s.commuteEnabled,
                            minutesBefore = s.commuteMinutesBefore,
                            onToggle = { viewModel.setCommute(it, s.commuteMinutesBefore) },
                            onMinutes = { viewModel.setCommute(s.commuteEnabled, it) }
                        )

                        IntensityCard(
                            fullScreen = s.fullScreen,
                            sound = s.sound,
                            vibrate = s.vibrate,
                            onChange = { fs, sd, vb -> viewModel.setIntensity(fs, sd, vb) }
                        )

                        Text(
                            text = "비번·휴무처럼 출근 시각이 없는 근무는 자동으로 건너뜁니다.\n" +
                                "전반·후반사업 알람은 날짜 상세에서 시계 앱 알람으로 설정할 수 있습니다.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item(key = "list_title") {
                    Text(
                        text = "예정된 알람 (5일)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (alarms.isEmpty()) {
                    item(key = "list_empty") {
                        Text(
                            "예정된 알람이 없습니다.\n위에서 근무 알람을 켜주세요.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    scheduledAlarmItems(
                        alarms = alarms,
                        onToggle = { alarm, enabled ->
                            alarmListViewModel.setEnabled(alarm.date, alarm.slot, enabled)
                        },
                        onEditTime = onEditTime,
                        onEditSound = onEditSound
                    )
                }
            }
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
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            if (enabled) {
                Spacer(Modifier.height(4.dp))
                // 몇 시간 몇 분 전에 울릴지를 시계 UI로 직접 고른다.
                var showPicker by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { showPicker = true },
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formatBeforeText(minutesBefore),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showPicker) {
                    MinutesBeforePickerDialog(
                        minutesBefore = minutesBefore,
                        onDismiss = { showPicker = false },
                        onConfirm = {
                            onMinutes(it)
                            showPicker = false
                        }
                    )
                }
            }
        }
    }
}

private fun formatBeforeText(minutesBefore: Int): String = when {
    minutesBefore == 0 -> "정시에 알람"
    minutesBefore < 60 -> "${minutesBefore}분 전 알람"
    minutesBefore % 60 == 0 -> "${minutesBefore / 60}시간 전 알람"
    else -> "${minutesBefore / 60}시간 ${minutesBefore % 60}분 전 알람"
}

/**
 * "출근 몇 시간 몇 분 전"을 시/분 TimePicker로 고르게 한다.
 * 시계의 시=시간, 분=분으로 읽어 총 분으로 환산해 저장한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinutesBeforePickerDialog(
    minutesBefore: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = minutesBefore / 60,
        initialMinute = minutesBefore % 60,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
        title = { Text("출근 몇 시간 전에 알람?") },
        text = {
            Column {
                Text(
                    "시 = 시간, 분 = 분으로 설정됩니다. (예: 1시간 30분 전)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                TimePicker(state = state)
            }
        }
    )
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

/** Android 12+ 정확 알람 권한이 꺼져 있으면 안내 + 설정 이동 (복귀 시 재확인) */
@Composable
private fun ExactAlarmWarning(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    var canSchedule by remember { mutableStateOf(alarmManager.canScheduleExactAlarms()) }

    // 설정 화면 갔다 돌아올 때 권한 재확인
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canSchedule = alarmManager.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (canSchedule) return

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

/**
 * Android 14(API 34)+ 전체화면 알람 권한(USE_FULL_SCREEN_INTENT) 안내.
 * 이 권한이 없으면 setFullScreenIntent가 시스템에 의해 일반 알림으로 강등되어,
 * 화면을 깨우지 못하고 트레이 알림으로만 뜨며(누르면 그제서야 전체화면) 동작한다.
 * 설정에서 켜고 돌아오면 onResume으로 재확인해 카드가 자동으로 사라진다.
 */
@Composable
private fun FullScreenIntentWarning(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    var canUse by remember { mutableStateOf(notificationManager.canUseFullScreenIntent()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canUse = notificationManager.canUseFullScreenIntent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (canUse) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "‘전체 화면 알림’ 권한이 필요합니다",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "이 권한이 꺼져 있으면 알람이 시계 앱처럼 화면을 깨우지 못하고 일반 알림으로만 표시됩니다.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            }) { Text("권한 설정 열기") }
        }
    }
}

/**
 * "다른 앱 위에 표시"(overlay) 권한이 꺼져 있으면 안내 + 바로 그 설정으로 이동.
 * 이 권한이 있어야 사용자가 다른 앱을 쓰는 중에도 알람 액티비티를 직접 표시할 수 있다.
 * 설정에서 켜고 돌아오면 lifecycle(onResume)로 재확인해 카드가 자동으로 사라진다.
 */
@Composable
private fun OverlayPermissionWarning(context: Context) {
    var canDraw by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // 설정 화면 갔다 돌아올 때 권한 재확인
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDraw = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (canDraw) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "‘다른 앱 위에 표시’ 권한이 필요합니다",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "이 권한이 있어야 다른 앱을 사용하는 중에도 알람이 전체 화면으로 표시됩니다. " +
                    "허용하지 않으면 헤드업 알림으로 표시됩니다.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }) { Text("권한 설정 열기") }
        }
    }
}
