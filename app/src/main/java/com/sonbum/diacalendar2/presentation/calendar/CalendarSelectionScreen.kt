package com.sonbum.diacalendar2.presentation.calendar

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.sonbum.diacalendar2.domain.model.DeviceCalendar
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CalendarSelectionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarSelectionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 동기화 결과 메시지를 스낵바로 표시
    LaunchedEffect(state.syncMessage) {
        state.syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSyncMessage()
        }
    }

    // 캘린더 읽기/쓰기 권한 요청
    val calendarPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    )

    // 모든 권한이 부여되었는지 확인
    val allPermissionsGranted = calendarPermissionsState.allPermissionsGranted

    // 권한이 부여되면 캘린더 새로고침
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            viewModel.setPermissionGranted()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "캘린더 연동 설정",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    if (state.calendars.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (state.selectedCalendarIds.size == state.calendars.size) {
                                    viewModel.deselectAllCalendars()
                                } else {
                                    viewModel.selectAllCalendars()
                                }
                            }
                        ) {
                            Text(
                                text = if (state.selectedCalendarIds.size == state.calendars.size)
                                    "전체 해제"
                                else
                                    "전체 선택"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        when {
            // 권한이 없는 경우
            !allPermissionsGranted -> {
                PermissionRequestContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    shouldShowRationale = calendarPermissionsState.shouldShowRationale,
                    onRequestPermission = { calendarPermissionsState.launchMultiplePermissionRequest() }
                )
            }

            // 로딩 중
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // 에러 발생
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.error ?: "알 수 없는 오류가 발생했습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refreshCalendars() }) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            // 캘린더가 없는 경우
            state.calendars.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "표시할 캘린더가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // 캘린더 목록 표시
            else -> {
                CalendarList(
                    calendars = state.calendars,
                    selectedCalendarIds = state.selectedCalendarIds,
                    onToggleSelection = viewModel::toggleCalendarSelection,
                    shiftSyncEnabled = state.shiftSyncEnabled,
                    isSyncing = state.isSyncing,
                    syncableCalendars = state.syncableCalendars,
                    shiftSyncCalendarId = state.shiftSyncCalendarId,
                    shiftSyncLabel = state.shiftSyncLabel,
                    onToggleShiftSync = viewModel::toggleShiftSync,
                    onSelectSyncCalendar = viewModel::selectSyncCalendar,
                    onApplyLabel = viewModel::setSyncLabel,
                    onSyncNow = viewModel::syncNow,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun PermissionRequestContent(
    modifier: Modifier = Modifier,
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (shouldShowRationale) {
                    "캘린더 일정을 표시하려면\n캘린더 접근 권한이 필요합니다."
                } else {
                    "기기에 저장된 캘린더를 연동하려면\n캘린더 읽기 권한이 필요합니다."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("권한 허용하기")
            }
        }
    }
}

@Composable
private fun CalendarList(
    calendars: List<DeviceCalendar>,
    selectedCalendarIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    shiftSyncEnabled: Boolean,
    isSyncing: Boolean,
    syncableCalendars: List<DeviceCalendar>,
    shiftSyncCalendarId: Long,
    shiftSyncLabel: String,
    onToggleShiftSync: (Boolean) -> Unit,
    onSelectSyncCalendar: (Long) -> Unit,
    onApplyLabel: (String) -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 계정별로 그룹화
    val groupedCalendars = calendars.groupBy { it.accountName }

    LazyColumn(
        modifier = modifier
    ) {
        // 근무 동기화 카드 (맨 위)
        item(key = "shift_sync_card") {
            ShiftSyncCard(
                enabled = shiftSyncEnabled,
                isSyncing = isSyncing,
                syncableCalendars = syncableCalendars,
                selectedSyncCalendarId = shiftSyncCalendarId,
                syncLabel = shiftSyncLabel,
                onToggle = onToggleShiftSync,
                onSelectSyncCalendar = onSelectSyncCalendar,
                onApplyLabel = onApplyLabel,
                onSyncNow = onSyncNow
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        groupedCalendars.forEach { (accountName, accountCalendars) ->
            // 계정 헤더
            item(key = "header_$accountName") {
                AccountHeader(accountName = accountName)
            }

            // 해당 계정의 캘린더들
            items(
                items = accountCalendars,
                key = { it.id }
            ) { calendar ->
                CalendarItem(
                    calendar = calendar,
                    isSelected = selectedCalendarIds.contains(calendar.id),
                    onToggleSelection = { onToggleSelection(calendar.id) }
                )
            }

            // 구분선
            item(key = "divider_$accountName") {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftSyncCard(
    enabled: Boolean,
    isSyncing: Boolean,
    syncableCalendars: List<DeviceCalendar>,
    selectedSyncCalendarId: Long,
    syncLabel: String,
    onToggle: (Boolean) -> Unit,
    onSelectSyncCalendar: (Long) -> Unit,
    onApplyLabel: (String) -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "근무 캘린더 동기화",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "내 근무를 선택한 Google 캘린더에 일정으로 등록합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 대상 캘린더 선택 드롭다운
        var expanded by remember { mutableStateOf(false) }
        val selectedCalendar = syncableCalendars.firstOrNull { it.id == selectedSyncCalendarId }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedCalendar?.let { "${it.displayName} (${it.accountName})" }
                    ?: "근무를 등록할 캘린더 선택",
                onValueChange = {},
                readOnly = true,
                label = { Text("등록 캘린더") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (syncableCalendars.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("쓸 수 있는 Google 캘린더가 없습니다") },
                        onClick = { expanded = false }
                    )
                }
                syncableCalendars.forEach { cal ->
                    DropdownMenuItem(
                        text = { Text("${cal.displayName} (${cal.accountName})") },
                        onClick = {
                            onSelectSyncCalendar(cal.id)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 제목 라벨 편집: 이벤트가 "근무명 (라벨)" 로 표시됨. 비우면 근무명만.
        var labelInput by remember(syncLabel) { mutableStateOf(syncLabel) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = labelInput,
                onValueChange = { labelInput = it },
                singleLine = true,
                label = { Text("제목 라벨") },
                placeholder = { Text("예: 동대문승무소 (비우면 근무명만)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = { onApplyLabel(labelInput.trim()) },
                enabled = labelInput.trim() != syncLabel.trim()
            ) {
                Text("적용")
            }
        }
        Text(
            text = "이벤트 제목 예시: 주간" +
                (labelInput.trim().takeIf { it.isNotEmpty() }?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "동료와 공유하려면 Google 캘린더에서 이 캘린더를 공유 설정하세요. " +
                    "웹(calendar.google.com)에도 동기화됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSyncNow, enabled = !isSyncing) {
                    Text("지금 다시 동기화")
                }
                if (isSyncing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun AccountHeader(
    accountName: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = accountName,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun CalendarItem(
    calendar: DeviceCalendar,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelection)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 캘린더 색상 인디케이터
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(calendar.color))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 캘린더 이름
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = calendar.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (calendar.isPrimary) {
                Text(
                    text = "기본 캘린더",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // 선택 체크박스
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection() }
        )
    }
}
