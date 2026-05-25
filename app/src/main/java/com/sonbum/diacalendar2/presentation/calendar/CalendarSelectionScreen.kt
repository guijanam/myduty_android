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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    modifier: Modifier = Modifier
) {
    // 계정별로 그룹화
    val groupedCalendars = calendars.groupBy { it.accountName }

    LazyColumn(
        modifier = modifier
    ) {
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
