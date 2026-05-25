package com.sonbum.diacalendar2.presentation.home

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonbum.diacalendar2.data.local.OfficeWebsiteRegistry
import com.sonbum.diacalendar2.domain.model.CalendarEvent
import com.sonbum.diacalendar2.domain.model.DeviceCalendar
import com.sonbum.diacalendar2.domain.model.Dia
import com.sonbum.diacalendar2.domain.model.Memo
import com.sonbum.diacalendar2.domain.model.ShiftSwapRecord
import com.sonbum.diacalendar2.domain.model.VacationType
import com.sonbum.diacalendar2.domain.model.LateWorkRecord
import com.sonbum.diacalendar2.domain.model.LateWorkType
import com.sonbum.diacalendar2.domain.model.LateHolidayRecord
import com.sonbum.diacalendar2.domain.model.LateHolidayType
import com.sonbum.diacalendar2.domain.model.ShiftInputRecord
import com.sonbum.diacalendar2.domain.model.ShiftInputType
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.rounded.Note
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.rounded.Note
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.io.File
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.core.graphics.toColorInt as androidToColorInt
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.graphics.toColorInt

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDetailScreen(
	dateString: String,
	onBack: () -> Unit,
	onAddMemo: () -> Unit,
	onEditMemo: (String) -> Unit,
	onNavigateToMenu: () -> Unit = {},
	onNavigateToOfficeWebsite: (String, String) -> Unit = { _, _ -> },
	openEventDialogOnStart: Boolean = false,
	modifier: Modifier = Modifier,
	viewModel: DateDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 캘린더 이벤트 다이얼로그 상태
    var showEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<CalendarEvent?>(null) }

    // 공휴일 편집 다이얼로그 상태
    var showHolidayDialog by remember { mutableStateOf(false) }
    var showHolidayDeleteConfirm by remember { mutableStateOf(false) }

    // 휴가 입력 다이얼로그 상태
    var showVacationDialog by remember { mutableStateOf(false) }

    // 지근/지휴 입력 다이얼로그 상태 (제거됨 - 바로 토글)

    // 교번교체 다이얼로그 상태
    var showShiftSwapDialog by remember { mutableStateOf(false) }

    // 충당 다이얼로그 상태
    var showShiftInputDialog by remember { mutableStateOf(false) }

    // 승무소 사이트 비밀번호 다이얼로그 상태
    val officeWebsiteRegistry: OfficeWebsiteRegistry = koinInject()
    var showWebsitePasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(dateString) {
        viewModel.initialize(dateString)
    }

    // openEventDialogOnStart가 true면 캘린더 이벤트 다이얼로그 자동 열기
    LaunchedEffect(openEventDialogOnStart) {
        if (openEventDialogOnStart) {
            showEventDialog = true
        }
    }

    // 요일 계산
    val dayOfWeek = state.date.dayOfWeek.value // 1(월) ~ 7(일)
    val dayOfWeekText = when (dayOfWeek) {
        1 -> "월"
        2 -> "화"
        3 -> "수"
        4 -> "목"
        5 -> "금"
        6 -> "토"
        7 -> "일"
        else -> ""
    }

    // 공휴일 또는 일요일이면 빨간색, 토요일이면 파란색
    val isHoliday = state.holidayName != null
    val isSunday = dayOfWeek == 7
    val isSaturday = dayOfWeek == 6
    val titleColor = when {
        isHoliday || isSunday -> MaterialTheme.colorScheme.error
        isSaturday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.date.year}년 ${state.date.monthValue}월 ${state.date.dayOfMonth}일($dayOfWeekText)",
                            style = MaterialTheme.typography.titleMedium,
                            color = titleColor
                        )
                        // 공휴일 이름 표시 (클릭하여 편집 가능)
                        if (state.holidayName != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.holidayName!!,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { showHolidayDialog = true }
                            )
                        }
                    }
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
                    // 공휴일 추가/편집 버튼
                    IconButton(onClick = { showHolidayDialog = true }) {
                        Icon(
                            imageVector = if (state.holidayName != null) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (state.holidayName != null) "공휴일 편집" else "공휴일 추가",
                            tint = if (state.holidayName != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        val listState = rememberLazyListState()
        val hapticFeedback = LocalHapticFeedback.current

        // LazyColumn 내 메모 아이템 이전의 고정 아이템 수 계산
        // work_time(조건부), calendar_events_title(조건부), calendar items(조건부), divider(조건부), memo_title, empty(조건부)
        val headerItemCount = remember(state.shiftName, state.calendarEvents.size, state.memos.isEmpty()) {
            var count = 0
            if (state.shiftName != null) count++ // work_time
            if (state.calendarEvents.isNotEmpty()) {
                count++ // calendar_events_title
                count += state.calendarEvents.size // calendar event items
                count++ // divider
            }
            count++ // memo_title
            if (state.memos.isEmpty()) count++ // empty message
            count
        }

        val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
            // 헤더 아이템 수를 빼서 실제 메모 인덱스 계산
            val fromIndex = from.index - headerItemCount
            val toIndex = to.index - headerItemCount

            // 유효한 인덱스 범위 내에서만 reorder 수행
            if (fromIndex >= 0 && toIndex >= 0 && fromIndex < state.memos.size && toIndex < state.memos.size) {
                viewModel.reorderMemos(fromIndex, toIndex)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

	    Column(
		    modifier = Modifier
			    .fillMaxSize()
			    .padding(innerPadding) // 전체에 padding 적용
	    ) {
		  when (state.viewMode) {
		    DayViewMode.TIMEBLOCK -> {
		        DayTimeblockView(
		            state = state,
		            onEventClick = { event ->
		                editingEvent = event
		                showEventDialog = true
		            },
		            onMemoClick = { memo -> onEditMemo(memo.objectId) },
		            onMemoTimeChanged = { memoId, newStart, newEnd ->
		                viewModel.updateMemoTime(memoId, newStart, newEnd)
		            },
		            onEventTimeChanged = { eventId, newStart, newEnd ->
		                viewModel.updateCalendarEventTime(eventId, newStart, newEnd)
		            },
		            modifier = Modifier.weight(1f)
		        )
		    }
		    DayViewMode.LIST -> {
		    LazyColumn(
			    state = listState,
			    modifier = Modifier
				    .fillMaxWidth()
				    .weight(1f), // 중요: 남은 공간 모두 차지
			    contentPadding = PaddingValues(10.dp),
			    verticalArrangement = Arrangement.spacedBy(6.dp)
		    ) {
			    // 근무 시간 카드
			    //if (state.shiftName != null) {
				    item(key = "work_time") {
					    WorkTimeCard(
						    shiftName = state.effectiveShiftName ?: state.shiftName ?: "",
						    originalShiftName = state.shiftName ?: "",
						    dia = state.shiftDia,
						    vacationRecord = state.vacationRecord,
						    onVacationClick = { showVacationDialog = true },
						    shiftSwapRecord = state.shiftSwapRecord,
						    lateWorkRecord = state.lateWorkRecord,
						    onLateWorkClick = { viewModel.toggleLateWork() },
						    lateHolidayRecord = state.lateHolidayRecord,
						    onLateHolidayClick = { viewModel.toggleLateHoliday() },
						    shiftInputRecord = state.shiftInputRecord,
						    isHolidayWork = state.effectiveShiftName != null &&
						        state.holidayWorkShifts.contains(state.effectiveShiftName) &&
						        (isHoliday || isSaturday || isSunday)
					    )
				    }
			    //}

			    // 캘린더 이벤트 섹션 (연동된 캘린더 일정이 있을 때만 표시)
			    if (state.calendarEvents.isNotEmpty()) {
				    item(key = "calendar_events_title") {
					    Row(
						    modifier = Modifier
							    .fillMaxWidth()
							    .padding(top = 8.dp),
						    horizontalArrangement = Arrangement.SpaceBetween,
						    verticalAlignment = Alignment.CenterVertically
					    ) {
						    Row(verticalAlignment = Alignment.CenterVertically) {
							    Icon(
								    imageVector = Icons.Default.CalendarMonth,
								    contentDescription = null,
								    modifier = Modifier.size(20.dp),
								    tint = MaterialTheme.colorScheme.primary
							    )
							    Spacer(modifier = Modifier.width(8.dp))
							    Text(
								    text = "캘린더 일정",
								    style = MaterialTheme.typography.titleMedium
							    )
						    }
						    Text(
							    text = "${state.calendarEvents.size}개",
							    style = MaterialTheme.typography.labelLarge,
							    color = MaterialTheme.colorScheme.outline
						    )
					    }
				    }

				    items(
					    items = state.calendarEvents,
					    key = { "event_${it.id}_${it.startTime}" }
				    ) { event ->
					    CalendarEventCard(
						    event = event,
						    onClick = {
							    editingEvent = event
							    showEventDialog = true
						    },
						    onDelete = { showDeleteConfirm = event }
					    )
				    }

				    item(key = "divider") {
					    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
				    }
			    }

			    // 메모 섹션 타이틀
			    item(key = "memo_title") {
				    Row(
					    modifier = Modifier
						    .fillMaxWidth()
						    .padding(top = 8.dp),
					    horizontalArrangement = Arrangement.SpaceBetween,
					    verticalAlignment = Alignment.CenterVertically
				    ) {
					    Row(verticalAlignment = Alignment.CenterVertically) {
						    Icon(
							    imageVector = Icons.AutoMirrored.Rounded.Note,
							    contentDescription = null,
							    modifier = Modifier.size(20.dp),
							    tint = MaterialTheme.colorScheme.primary
						    )
						    Spacer(modifier = Modifier.width(8.dp))
						    Text(
							    text = "오늘의 메모(ToDo)",
							    style = MaterialTheme.typography.titleMedium
						    )
					    }

					    if (state.memos.isNotEmpty()) {
						    Text(
							    text = "왼쪽 핸들을 길게 눌러 순서변경",
							    style = MaterialTheme.typography.labelSmall,
							    color = MaterialTheme.colorScheme.outline
						    )
					    }
				    }
			    }

			    // 메모가 없을 때
			    if (state.memos.isEmpty()) {
				    item(key = "empty") {
					    Box(
						    modifier = Modifier
							    .fillMaxWidth()
							    .padding(32.dp),
						    contentAlignment = Alignment.Center
					    ) {
						    Text(
							    text = "메모가 없습니다.\n+ 버튼을 눌러 추가하세요.",
							    style = MaterialTheme.typography.bodyMedium,
							    color = MaterialTheme.colorScheme.outline
						    )
					    }
				    }
			    }

			    // 메모 리스트 with reorderable drag and drop
			    items(
				    items = state.memos,
				    key = { memo -> memo.objectId }
			    ) { memo ->
				    ReorderableItem(
					    state = reorderableLazyListState,
					    key = memo.objectId
				    ) { isDragging ->
					    val elevation by animateDpAsState(
						    targetValue = if (isDragging) 8.dp else 2.dp,
						    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
						    label = "elevation"
					    )

					    ReorderableMemoCard(
						    memo = memo,
						    isDragging = isDragging,
						    elevation = elevation,
						    onClick = { onEditMemo(memo.objectId) },
						    onToggleComplete = { viewModel.toggleMemoComplete(memo) },
						    modifier = Modifier.draggableHandle(
							    onDragStarted = {
								    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
							    },
							    onDragStopped = {
								    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							    }
						    )
					    )
				    }
			    }
		    }//LazyColumn End
		    } // LIST end
		  } // when end

		    // 메모 / 캘린더 / 식당메뉴 / 타임블록 버튼
		    Row(
			    modifier = Modifier
				    .fillMaxWidth()
				    .padding(horizontal = 5.dp, vertical = 1.dp),
			    horizontalArrangement = Arrangement.spacedBy(6.dp),
		    ) {
			    // 메모 추가
			    ModernShiftButton(
				    text = "",
				    icon = Icons.Default.Create,
				    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
				    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
				    onClick = onAddMemo,
				    modifier = Modifier.weight(1f)
			    )
			    // 캘린더 일정 추가 (연동된 캘린더가 있을 때만 표시)
			    if (state.availableCalendars.isNotEmpty()) {
				    ModernShiftButton(
					    text = "캘린더",
					    icon = Icons.Default.Event,
					    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
					    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
					    onClick = {
						    editingEvent = null
						    showEventDialog = true
					    },
					    modifier = Modifier.weight(1f)
				    )
			    }
			    // 식당메뉴
			    ModernShiftButton(
				    text = "식당",
				    icon = Icons.Default.Restaurant,
				    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
				    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
				    onClick = onNavigateToMenu,
				    modifier = Modifier.weight(1f)
			    )
			    // 뷰 모드 토글 (타임블록 / 리스트)
			    ModernShiftButton(
				    text = if (state.viewMode == DayViewMode.LIST) "타임블록" else "리스트",
				    icon = if (state.viewMode == DayViewMode.LIST)
					    Icons.Default.FormatListNumbered else Icons.AutoMirrored.Filled.Notes,
				    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
				    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
				    onClick = { viewModel.toggleViewMode() },
				    modifier = Modifier.weight(1f)
			    )
		    }

		    if (state.shiftName != null && !state.isCustomShift) {
			    val isDarkTheme = isSystemInDarkTheme()
			    val hasSwap = state.shiftSwapRecord != null
			    val hasShiftInput = state.shiftInputRecord != null
			    val websiteUrl = state.officeWebsiteUrl

			    Row(
				    modifier = Modifier
					    .fillMaxWidth()
					    .padding(horizontal = 10.dp, vertical = 6.dp),
				    horizontalArrangement = Arrangement.spacedBy(8.dp)
			    ) {
				    // --- 1. 교번교체 버튼 ---
				    val swapBaseColor = if (isDarkTheme) Color(0xFFF57C00) else Color(0xFFFF9800)
				    val swapBg = if (hasSwap) swapBaseColor else MaterialTheme.colorScheme.secondaryContainer
				    val swapContentColor = when {
					    hasSwap && isDarkTheme -> Color.White
					    hasSwap -> Color.Black
					    else -> MaterialTheme.colorScheme.onSecondaryContainer
				    }

				    ModernShiftButton(
					    text = if (hasSwap) "교체:${state.shiftSwapRecord!!.swappedShiftName}" else "교번교체",
					    icon = Icons.Default.SwapHoriz, // 교체 아이콘
					    backgroundColor = swapBg,
					    contentColor = swapContentColor,
					    onClick = { showShiftSwapDialog = true },
					    modifier = Modifier.weight(1f)
				    )

				    // --- 2. 충당 버튼 ---
				    val shiftInputBaseColor = if (hasShiftInput) {
					    try { Color(state.shiftInputRecord!!.colorHex.androidToColorInt()) }
					    catch (e: Exception) { MaterialTheme.colorScheme.tertiary }
				    } else {
					    MaterialTheme.colorScheme.tertiaryContainer
				    }

				    val shiftInputContentColor = when {
					    hasShiftInput && isDarkTheme -> Color.White
					    hasShiftInput -> Color.Black
					    else -> MaterialTheme.colorScheme.onTertiaryContainer
				    }

				    ModernShiftButton(
					    text = if (hasShiftInput) "${state.shiftInputRecord!!.shortName}:${state.shiftInputRecord!!.originalShiftName}" else "충당",
					    icon = Icons.Default.AddCircleOutline, // 충당/추가 아이콘
					    backgroundColor = shiftInputBaseColor,
					    contentColor = shiftInputContentColor,
					    onClick = { showShiftInputDialog = true },
					    modifier = Modifier.weight(1f)
				    )

				    // --- 3. 승무소 사이트 버튼 (URL 매핑된 승무소만) ---
				    if (websiteUrl != null) {
					    ModernShiftButton(
						    text = "근무자",
						    icon = Icons.Default.Groups,
						    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
						    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
						    onClick = {
							    val officeName = state.officeName ?: ""
							    val urlWithDate = appendDateParam(websiteUrl, state.date)
							    val needsPassword = officeWebsiteRegistry.requiresPassword(officeName) &&
								    !officeWebsiteRegistry.isAuthenticated(officeName)
							    if (needsPassword) {
								    showWebsitePasswordDialog = true
							    } else {
								    onNavigateToOfficeWebsite(urlWithDate, officeName)
							    }
						    },
						    modifier = Modifier.weight(1f)
					    )
				    }
			    }
		    }

	    }


    }

    // 승무소 사이트 비밀번호 다이얼로그
    if (showWebsitePasswordDialog) {
        val officeName = state.officeName ?: ""
        val websiteUrl = state.officeWebsiteUrl
        OfficeWebsitePasswordDialog(
            officeName = officeName,
            onDismiss = { showWebsitePasswordDialog = false },
            onConfirm = { input ->
                if (officeWebsiteRegistry.verifyPassword(officeName, input)) {
                    officeWebsiteRegistry.setAuthenticated(officeName)
                    showWebsitePasswordDialog = false
                    if (websiteUrl != null) {
                        onNavigateToOfficeWebsite(appendDateParam(websiteUrl, state.date), officeName)
                    }
                    true
                } else {
                    false
                }
            }
        )
    }

    // 캘린더 이벤트 추가/수정 다이얼로그
    if (showEventDialog) {
        CalendarEventEditDialog(
            event = editingEvent,
            date = state.date,
            availableCalendars = state.availableCalendars,
            onDismiss = {
                showEventDialog = false
                editingEvent = null
            },
            onSave = { event ->
                if (editingEvent != null) {
                    viewModel.updateCalendarEvent(event)
                } else {
                    viewModel.createCalendarEvent(event)
                }
                showEventDialog = false
                editingEvent = null
            }
        )
    }

    // 삭제 확인 다이얼로그
    showDeleteConfirm?.let { event ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("일정 삭제") },
            text = { Text("\"${event.title}\" 일정을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCalendarEvent(event.id)
                        showDeleteConfirm = null
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("취소")
                }
            }
        )
    }

    // 공휴일 편집 다이얼로그
    if (showHolidayDialog) {
        HolidayEditDialog(
            currentName = state.holidayName,
            isUserCreated = state.isUserCreatedHoliday,
            onDismiss = { showHolidayDialog = false },
            onSave = { name ->
                if (state.holidayName != null) {
                    viewModel.updateHoliday(name)
                } else {
                    viewModel.addHoliday(name)
                }
                showHolidayDialog = false
            },
            onDelete = {
                showHolidayDialog = false
                showHolidayDeleteConfirm = true
            }
        )
    }

    // 공휴일 삭제 확인 다이얼로그
    if (showHolidayDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showHolidayDeleteConfirm = false },
            title = { Text("공휴일 삭제") },
            text = { Text("\"${state.holidayName}\" 공휴일을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHoliday()
                        showHolidayDeleteConfirm = false
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHolidayDeleteConfirm = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 휴가 입력 다이얼로그
    if (showVacationDialog) {
        VacationInputDialog(
            date = state.date,
            vacationTypes = state.vacationTypes,
            existingRecord = state.vacationRecord,
            onConfirm = { vacationType, days ->
                viewModel.addVacation(vacationType, days)
                showVacationDialog = false
            },
            onDelete = {
                viewModel.deleteVacation()
                showVacationDialog = false
            },
            onDismiss = { showVacationDialog = false }
        )
    }

    // 교번교체 다이얼로그
    if (showShiftSwapDialog) {
        ShiftSwapInputDialog(
            date = state.date,
            currentShiftName = state.shiftName ?: "",
            availableShifts = state.availableShifts,
            existingSwapRecord = state.shiftSwapRecord,
            onConfirm = { targetShift, days ->
                viewModel.addShiftSwap(targetShift, days)
                showShiftSwapDialog = false
            },
            onDelete = {
                viewModel.deleteShiftSwap()
                showShiftSwapDialog = false
            },
            onDismiss = { showShiftSwapDialog = false }
        )
    }

    // 충당 다이얼로그
    if (showShiftInputDialog) {
        ShiftInputDialog(
            date = state.date,
            currentShiftName = state.shiftName ?: "",
            availableShifts = state.availableShifts,
            shiftPattern = state.shiftPattern,
            shiftInputTypes = state.shiftInputTypes,
            existingRecord = state.shiftInputRecord,
            hasLateWork = state.lateWorkRecord != null,
            onConfirm = { shiftInputType, targetShift, days ->
                viewModel.addShiftInput(shiftInputType, targetShift, days)
                showShiftInputDialog = false
            },
            onDelete = {
                viewModel.deleteShiftInput()
                showShiftInputDialog = false
            },
            onDismiss = { showShiftInputDialog = false }
        )
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun WorkTimeCard(
    shiftName: String,
    originalShiftName: String = shiftName,
    dia: Dia?,
    vacationRecord: com.sonbum.diacalendar2.domain.model.VacationRecord? = null,
    onVacationClick: () -> Unit = {},
    shiftSwapRecord: ShiftSwapRecord? = null,
    lateWorkRecord: LateWorkRecord? = null,
    onLateWorkClick: () -> Unit = {},
    lateHolidayRecord: LateHolidayRecord? = null,
    onLateHolidayClick: () -> Unit = {},
    shiftInputRecord: ShiftInputRecord? = null,
    isHolidayWork: Boolean = false
) {
    val hasVacation = vacationRecord != null
    val hasSwap = shiftSwapRecord != null
    val hasLateWork = lateWorkRecord != null
    val hasLateHoliday = lateHolidayRecord != null
    val hasShiftInput = shiftInputRecord != null

    // 휴가, 지휴가 있으면 내용은 흐리게 처리
    // 지근은 충당이 없을 때만 흐리게 처리 (지근충당의 경우 충당이 우선이므로 흐리게 하지 않음)
    val shiftContentAlpha = when {
        hasVacation || hasLateHoliday -> 0.35f
        hasLateWork && !hasShiftInput -> 0.35f  // 지근만 있고 충당이 없을 때
        else -> 1f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 1행: 근무이름 + (원래이름) + 근무타입 | 지근 버튼 | 지휴 버튼 | 휴가 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer { alpha = shiftContentAlpha }
                ) {
                    // 교번 뱃지 - 충당인 경우 충당 유형별 색상 적용
                    val isDarkTheme = isSystemInDarkTheme()
                    val (badgeBackgroundColor, badgeTextColor) = when {
                        isHolidayWork -> {
                            if (isDarkTheme) Color(0xFFC62828) to Color.White
                            else Color(0xFFFFCDD2) to Color.Black
                        }
                        hasShiftInput -> {
                            // 충당 유형별 색상 (다크/라이트 모드 가독성 개선)
                            when (shiftInputRecord.colorHex.uppercase()) {
                                "#4CAF50" -> // 대기충당 (초록)
                                    if (isDarkTheme) Color(0xFF388E3C) to Color.White
                                    else Color(0xFFC8E6C9) to Color(0xFF1B5E20)
                                "#9C27B0" -> // 휴무충당 (보라)
                                    if (isDarkTheme) Color(0xFF7B1FA2) to Color.White
                                    else Color(0xFFE1BEE7) to Color(0xFF4A148C)
                                "#03A9F4" -> // 지근충당 (하늘색)
                                    if (isDarkTheme) Color(0xFF0288D1) to Color.White
                                    else Color(0xFFB3E5FC) to Color(0xFF01579B)
                                else -> {
                                    val baseColor = try {
                                        Color(shiftInputRecord.colorHex.androidToColorInt())
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.tertiary
                                    }
                                    if (isDarkTheme) baseColor to Color.White
                                    else baseColor.copy(alpha = 0.3f) to Color.Black
                                }
                            }
                        }
                        else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBackgroundColor)
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shiftName,
                            color = badgeTextColor,
	                        fontSize = 20.sp,
//                            style = MaterialTheme.typography.titleMedium
//		                            fontSize = 15.sp,
	                        style = TextStyle(
		                        platformStyle = PlatformTextStyle(includeFontPadding = false),
		                        fontWeight = FontWeight.Bold
	                        ),
                        )
                    }

                    if (dia?.typeName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = dia.typeName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (hasSwap) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(본:${originalShiftName})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

	                
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 지근 버튼
                    val lateWorkColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF0288D1) else Color(0xFF81D4FA)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (hasLateWork)
                                    lateWorkColor
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            )
                            .clickable { onLateWorkClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
	                    Text(
		                    text = if (hasLateWork) "지근삭제" else "지근",
		                    color = if (hasLateWork) {
			                    if (androidx.compose.foundation.isSystemInDarkTheme()) {
				                    Color.White
			                    } else {
				                    Color.Black
			                    }
		                    } else {
			                    MaterialTheme.colorScheme.onTertiaryContainer
		                    },
		                    fontWeight = FontWeight.Bold,
		                    style = MaterialTheme.typography.labelMedium
	                    )
                    }

                    // 지휴 버튼
                    val lateHolidayColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFC62828) else Color(0xFFFFCDD2)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (hasLateHoliday)
                                    lateHolidayColor
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            )
                            .clickable { onLateHolidayClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hasLateHoliday) "지휴삭제" else "지휴",
                            color = if (hasLateHoliday)
                                if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White else Color.Black
                                // Light Red (FFCDD2) -> Black Text
                                // Dark Red (C62828) -> White Text
                            else
                                MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // 휴가 버튼
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (hasVacation)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer
                            )
                            .clickable { onVacationClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = vacationRecord?.shortName ?: "근태",
                            color = if (hasVacation)
                                MaterialTheme.colorScheme.onTertiary
                            else
                                MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (dia != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
						.graphicsLayer { alpha = shiftContentAlpha }
                ) {
                    // 출근 시간
					Row(
						modifier = Modifier
							.fillMaxWidth(),
						verticalAlignment = Alignment.CenterVertically

					) {
						if (!dia.workTime.isNullOrBlank()) {
							ShiftInfoRow(label = "출근", value = dia.workTime)
						}


						Spacer(modifier = Modifier.weight(1f))
						if (!dia.thirdTime.isNullOrBlank()) {
							val isImageUrl = dia.thirdTime.startsWith("http://") || dia.thirdTime.startsWith("https://")
							if (isImageUrl) {
								var showImageDialog by remember { mutableStateOf(false) }
								val coroutineScope = rememberCoroutineScope()
								Spacer(modifier = Modifier.width(10.dp))
								Icon(
									imageVector = Icons.Filled.Image,
									contentDescription = "근무표 이미지",
									modifier = Modifier
										.size(30.dp)
										.clickable { showImageDialog = true },
									tint = MaterialTheme.colorScheme.primary
								)
								if (showImageDialog) {
									Dialog(
										onDismissRequest = { showImageDialog = false },
										properties = DialogProperties(usePlatformDefaultWidth = false)
									) {
										val backgroundColor = Color.White
										val scale = remember { Animatable(1f) }
										val offsetX = remember { Animatable(0f) }
										val offsetY = remember { Animatable(0f) }

										BoxWithConstraints(
											modifier = Modifier
												.fillMaxSize()
												.background(backgroundColor)
										) {
											val maxWidthPx = constraints.maxWidth.toFloat()
											val maxHeightPx = constraints.maxHeight.toFloat()

											Box(
												modifier = Modifier
													.matchParentSize()
													.clickable(
														indication = null,
														interactionSource = remember { MutableInteractionSource() }
													) { showImageDialog = false }
											)

											AsyncImage(
												model = dia.thirdTime,
												contentDescription = "근무표 이미지",
												contentScale = ContentScale.Fit,
												modifier = Modifier
													.fillMaxSize()
													.graphicsLayer(
														scaleX = scale.value,
														scaleY = scale.value,
														translationX = offsetX.value,
														translationY = offsetY.value
													)
													.pointerInput(Unit) {
														detectTransformGestures { _, pan, zoom, _ ->
															coroutineScope.launch {
																val newScale = (scale.value * zoom).coerceIn(1f, 3f)
																scale.snapTo(newScale)
															}
															val maxX = (maxWidthPx * (scale.value - 1)) / 2
															val maxY = (maxHeightPx * (scale.value - 1)) / 2
															val newOffsetX = offsetX.value + pan.x * scale.value
															val newOffsetY = offsetY.value + pan.y * scale.value
															coroutineScope.launch {
																offsetX.snapTo(newOffsetX.coerceIn(-maxX, maxX))
																offsetY.snapTo(newOffsetY.coerceIn(-maxY, maxY))
															}
														}
													}
													.pointerInput(Unit) {
														detectTapGestures(
															onDoubleTap = { tapOffset ->
																coroutineScope.launch {
																	if (scale.value > 1f) {
																		launch { scale.animateTo(1f) }
																		launch {
																			offsetX.animateTo(0f)
																			offsetY.animateTo(0f)
																		}
																	} else {
																		val targetScale = 3f
																		val zoomFactor = targetScale / scale.value
																		val imageCenter = Offset(maxWidthPx / 2, maxHeightPx / 2)
																		val delta = tapOffset - imageCenter
																		val newOffsetX = offsetX.value - delta.x * (zoomFactor - 1)
																		val newOffsetY = offsetY.value - delta.y * (zoomFactor - 1)
																		launch { scale.animateTo(targetScale) }
																		launch {
																			offsetX.animateTo(newOffsetX)
																			offsetY.animateTo(newOffsetY)
																		}
																	}
																}
															}
														)
													}
											)

											IconButton(
												onClick = { showImageDialog = false },
												modifier = Modifier
													.align(Alignment.TopEnd)
													.padding(16.dp)
													.size(40.dp)
													.clip(CircleShape)
													.background(Color.Gray)
											) {
												Icon(
													imageVector = Icons.Default.Close,
													contentDescription = "닫기",
													tint = Color.White
												)
											}


										}
									}
								}
							} else {
								Spacer(modifier = Modifier.width(10.dp))
								Text(
									text = "-${dia.thirdTime}-",
									style = MaterialTheme.typography.labelLarge,
									color = MaterialTheme.colorScheme.onPrimaryContainer
								)
							}
						}

						if (!dia.totalTime.isNullOrBlank()) {
							val isImageUrl = dia.totalTime.startsWith("http://") || dia.totalTime.startsWith("https://")
							if (isImageUrl) {
								var showImageDialog by remember { mutableStateOf(false) }
								val coroutineScope = rememberCoroutineScope()
								Spacer(modifier = Modifier.width(10.dp))
								Icon(
									imageVector = Icons.Default.ContentPaste,
									contentDescription = "근무표 이미지",
									modifier = Modifier
										.size(30.dp)
										.clickable { showImageDialog = true },
									tint = MaterialTheme.colorScheme.primary
								)
								if (showImageDialog) {
									Dialog(
										onDismissRequest = { showImageDialog = false },
										properties = DialogProperties(usePlatformDefaultWidth = false)
									) {
										val backgroundColor = Color.White
										val scale = remember { Animatable(1f) }
										val offsetX = remember { Animatable(0f) }
										val offsetY = remember { Animatable(0f) }

										BoxWithConstraints(
											modifier = Modifier
												.fillMaxSize()
												.background(backgroundColor)
										) {
											val maxWidthPx = constraints.maxWidth.toFloat()
											val maxHeightPx = constraints.maxHeight.toFloat()

											Box(
												modifier = Modifier
													.matchParentSize()
													.clickable(
														indication = null,
														interactionSource = remember { MutableInteractionSource() }
													) { showImageDialog = false }
											)

											AsyncImage(
												model = dia.totalTime,
												contentDescription = "근무표 이미지",
												contentScale = ContentScale.Fit,
												modifier = Modifier
													.fillMaxSize()
													.graphicsLayer(
														scaleX = scale.value,
														scaleY = scale.value,
														translationX = offsetX.value,
														translationY = offsetY.value
													)
													.pointerInput(Unit) {
														detectTransformGestures { _, pan, zoom, _ ->
															coroutineScope.launch {
																val newScale = (scale.value * zoom).coerceIn(1f, 3f)
																scale.snapTo(newScale)
															}
															val maxX = (maxWidthPx * (scale.value - 1)) / 2
															val maxY = (maxHeightPx * (scale.value - 1)) / 2
															val newOffsetX = offsetX.value + pan.x * scale.value
															val newOffsetY = offsetY.value + pan.y * scale.value
															coroutineScope.launch {
																offsetX.snapTo(newOffsetX.coerceIn(-maxX, maxX))
																offsetY.snapTo(newOffsetY.coerceIn(-maxY, maxY))
															}
														}
													}
													.pointerInput(Unit) {
														detectTapGestures(
															onDoubleTap = { tapOffset ->
																coroutineScope.launch {
																	if (scale.value > 1f) {
																		launch { scale.animateTo(1f) }
																		launch {
																			offsetX.animateTo(0f)
																			offsetY.animateTo(0f)
																		}
																	} else {
																		val targetScale = 3f
																		val zoomFactor = targetScale / scale.value
																		val imageCenter = Offset(maxWidthPx / 2, maxHeightPx / 2)
																		val delta = tapOffset - imageCenter
																		val newOffsetX = offsetX.value - delta.x * (zoomFactor - 1)
																		val newOffsetY = offsetY.value - delta.y * (zoomFactor - 1)
																		launch { scale.animateTo(targetScale) }
																		launch {
																			offsetX.animateTo(newOffsetX)
																			offsetY.animateTo(newOffsetY)
																		}
																	}
																}
															}
														)
													}
											)

											IconButton(
												onClick = { showImageDialog = false },
												modifier = Modifier
													.align(Alignment.TopEnd)
													.padding(16.dp)
													.size(40.dp)
													.clip(CircleShape)
													.background(Color.Gray)
											) {
												Icon(
													imageVector = Icons.Default.Close,
													contentDescription = "닫기",
													tint = Color.White
												)
											}


										}
									}
								}
							} else {
								Spacer(modifier = Modifier.width(10.dp))
								Text(
									text = "-${dia.totalTime}-",
									style = MaterialTheme.typography.labelLarge,
									color = MaterialTheme.colorScheme.onPrimaryContainer
								)
							}
						}
						Spacer(modifier = Modifier.weight(1f))

					}

	                Row(
		                modifier = Modifier
			                .fillMaxWidth(),
		                verticalAlignment = Alignment.CenterVertically

	                ) {
		                // 전반
		                if (!dia.firstTime.isNullOrBlank()) {
			                ShiftInfoRow(label = "전반", value = dia.firstTime)
		                }
	                }

	                Row(
		                modifier = Modifier
			                .fillMaxWidth(),
		                verticalAlignment = Alignment.CenterVertically

	                ) {
		                // 후반
		                if (!dia.secondTime.isNullOrBlank()) {
			                ShiftInfoRow(label = "후반", value = dia.secondTime)
		                }
	                }




                }
            }
        }
    }
}

@Composable
private fun ShiftInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            //.fillMaxWidth()
            .padding(vertical = 4.dp),
	    horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.width(35.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun TrNumRow(label: String, value: String) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 2.dp),
		horizontalArrangement = Arrangement.Start,
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = label,
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
			modifier = Modifier.width(40.dp)
		)
		Text(
			text = value,
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.SemiBold,
			color = MaterialTheme.colorScheme.onPrimaryContainer
		)
	}
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VacationInputDialog(
    date: LocalDate,
    vacationTypes: List<VacationType>,
    existingRecord: com.sonbum.diacalendar2.domain.model.VacationRecord?,
    onConfirm: (VacationType, Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(vacationTypes.firstOrNull()) }
    var days by remember { mutableStateOf("1") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("M월 d일")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "근태 입력",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 시작 날짜 표시
                Text(
                    text = "시작일: ${date.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 기존 휴가 표시
                if (existingRecord != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "현재: ${existingRecord.vacationName} [${existingRecord.shortName}]",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // 휴가 종류 선택
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType?.let { "${it.name} [${it.shortName}]" } ?: "선택",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("근태 종류") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
	                        // 수정된 부분: AnchorType과 enabled를 명시적으로 전달
	                        .menuAnchor(
		                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
		                        enabled = true
	                        )
                    )

                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        vacationTypes.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text("${type.name} [${type.shortName}]")
                                },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 일수 입력
                OutlinedTextField(
                    value = days,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            days = newValue
                        }
                    },
                    label = { Text("일수") },
                    supportingText = {
                        val daysInt = days.toIntOrNull() ?: 0
                        if (daysInt > 1) {
                            val endDate = date.plusDays((daysInt - 1).toLong())
                            Text("${date.format(dateFormatter)} ~ ${endDate.format(dateFormatter)}")
                        }
                    },
                    singleLine = true,
                    suffix = { Text("일") },
	                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // 이 부분을 추가합니다
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val type = selectedType ?: return@TextButton
                    val daysInt = days.toIntOrNull() ?: return@TextButton
                    onConfirm(type, daysInt)
                },
                enabled = selectedType != null && (days.toIntOrNull() ?: 0) > 0
            ) {
                Text("입력")
            }
        },
        dismissButton = {
            Row {
                if (existingRecord != null) {
                    TextButton(onClick = onDelete) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftSwapInputDialog(
    date: LocalDate,
    currentShiftName: String,
    availableShifts: List<String>,
    existingSwapRecord: ShiftSwapRecord?,
    onConfirm: (targetShift: String, days: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedShift by remember { mutableStateOf(availableShifts.firstOrNull() ?: "") }
    var selectedDays by remember { mutableIntStateOf(1) }
    var shiftDropdownExpanded by remember { mutableStateOf(false) }
    var daysDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("M월 d일")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "교번교체",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 현재 교번 + 시작일 표시
                Text(
                    text = "현재 교번: $currentShiftName | 시작일: ${date.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 기존 교체 정보 표시
                if (existingSwapRecord != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "현재 교체: ${existingSwapRecord.originalShiftName} → ${existingSwapRecord.swappedShiftName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // 교체할 교번 선택 (기준교번 선택 리스트 = diaSelects)
                ExposedDropdownMenuBox(
                    expanded = shiftDropdownExpanded,
                    onExpandedChange = { shiftDropdownExpanded = !shiftDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedShift,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("교체할 교번") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
	                        // 수정된 부분: AnchorType과 enabled를 명시적으로 전달
	                        .menuAnchor(
		                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
		                        enabled = true
	                        )
                    )

                    ExposedDropdownMenu(
                        expanded = shiftDropdownExpanded,
                        onDismissRequest = { shiftDropdownExpanded = false }
                    ) {
                        availableShifts.forEach { shift ->
                            DropdownMenuItem(
                                text = { Text(shift) },
                                onClick = {
                                    selectedShift = shift
                                    shiftDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 교체일수 드롭다운 (1~15일)
                ExposedDropdownMenuBox(
                    expanded = daysDropdownExpanded,
                    onExpandedChange = { daysDropdownExpanded = !daysDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedDays}일",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("교체일수") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = daysDropdownExpanded) },
                        supportingText = {
                            if (selectedDays > 1) {
                                val endDate = date.plusDays((selectedDays - 1).toLong())
                                Text("${date.format(dateFormatter)} ~ ${endDate.format(dateFormatter)}")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
	                        // 수정된 부분: AnchorType과 enabled를 명시적으로 전달
	                        .menuAnchor(
		                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
		                        enabled = true
	                        )
                    )

                    ExposedDropdownMenu(
                        expanded = daysDropdownExpanded,
                        onDismissRequest = { daysDropdownExpanded = false }
                    ) {
                        (1..15).forEach { day ->
                            DropdownMenuItem(
                                text = { Text("${day}일") },
                                onClick = {
                                    selectedDays = day
                                    daysDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedShift, selectedDays)
                },
                enabled = selectedShift.isNotBlank()
            ) {
                Text("교체")
            }
        },
        dismissButton = {
            Row {
                if (existingSwapRecord != null) {
                    TextButton(onClick = onDelete) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReorderableMemoCard(
	memo: Memo,
	isDragging: Boolean,
	elevation: Dp,
	onClick: () -> Unit,
	onToggleComplete: () -> Unit,
	modifier: Modifier,
) {
	val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
	var showFullImage by remember { mutableStateOf(false) }

	// 전체화면 이미지 다이얼로그
	if (showFullImage && memo.imagePath != null) {
		FullScreenImageDialog(
			imagePath = memo.imagePath,
			onDismiss = { showFullImage = false }
		)
	}

	// 스케일 애니메이션
	val scale by animateFloatAsState(
		targetValue = if (isDragging) 1.02f else 1f,
		animationSpec = spring(
			dampingRatio = 0.95f,
			stiffness = Spring.StiffnessVeryLow
		),
		label = "scale"
	)

	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(all = 2.dp)
			.graphicsLayer {
				scaleX = scale
				scaleY = scale
			}
			.clickable(onClick = onClick),
		shape = RoundedCornerShape(10.dp),
		//elevation = CardDefaults.cardElevation(defaultElevation = elevation),
		colors = CardDefaults.cardColors(
			containerColor = if (isDragging)
				MaterialTheme.colorScheme.surfaceContainerHigh
			else
				Color(memo.hexColorString.toColorInt()).copy(alpha = 0.2f)
		)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 4.dp, horizontal = 8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Spacer(modifier = Modifier.width(5.dp))
			// 1. 드래그 핸들 - reorderable 라이브러리의 draggableHandle Modifier 적용
			Icon(
				imageVector = Icons.Default.DragHandle,
				contentDescription = "드래그하여 순서 변경",
				tint = if (isDragging)
					MaterialTheme.colorScheme.primary
				else
					MaterialTheme.colorScheme.outlineVariant,
				modifier = Modifier
					.size(30.dp)
					.then(modifier)
			)
			Spacer(modifier = Modifier.width(10.dp))

			// 2. 체크박스
			CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
				Checkbox(
					checked = memo.isCompleted,
					onCheckedChange = { onToggleComplete() },
					modifier = Modifier.padding(horizontal = 4.dp)
				)
			}

			// 3. 색상 인디케이터
			Box(
				modifier = Modifier
					.size(6.dp)
					.clip(CircleShape)
					.background(Color(memo.hexColorString.toColorInt()))
			)

			// 4. 내용 (제목 + 시간 한 줄 배치로 높이 절약)
			Column(
				modifier = Modifier
					.weight(1f)
					.padding(start = 8.dp, end = 4.dp)
			) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Text(
						text = memo.title,
						style = MaterialTheme.typography.bodyMedium.copy(
							fontWeight = FontWeight.SemiBold,
							platformStyle = PlatformTextStyle(includeFontPadding = false)
						),
						textDecoration = if (memo.isCompleted) TextDecoration.LineThrough else null,
						color = if (memo.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
						maxLines = 1,
						//overflow = TextOverflow.Ellipsis,
						modifier = Modifier.weight(1f)
					)

					Text(
						text = if (memo.isAllDay) "종일" else "${memo.startTime.format(timeFormatter)} - ${memo.endTime.format(timeFormatter)}",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.outline,
						modifier = Modifier.padding(start = 4.dp)
					)
				}

				// 내용이 있을 때만 표시
				if (memo.content.isNotBlank()) {
					Text(
						text = memo.content,
						style = MaterialTheme.typography.bodySmall.copy(
							platformStyle = PlatformTextStyle(includeFontPadding = false)
						),
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						maxLines = 1,
						//overflow = TextOverflow.Ellipsis,
						modifier = Modifier.padding(top = 2.dp)
					)
				}

				// 이미지 썸네일
				if (memo.imagePath != null) {
					val context = LocalContext.current
					AsyncImage(
						model = ImageRequest.Builder(context)
							.data(File(memo.imagePath))
							.crossfade(true)
							.build(),
						contentDescription = "메모 이미지",
						contentScale = ContentScale.Crop,
						modifier = Modifier
							.fillMaxWidth()
							.height(80.dp)
							.padding(top = 4.dp)
							.clip(RoundedCornerShape(6.dp))
							.clickable { showFullImage = true }
					)
				}
			}
		}
	}
}

@Composable
private fun CalendarEventCard(
	event: CalendarEvent,
	onClick: () -> Unit,
	onDelete: () -> Unit
) {
	val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
	val eventColor = Color(event.color)

	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface
		)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// 색상 인디케이터
			Box(
				modifier = Modifier
					.width(4.dp)
					.height(40.dp)
					.clip(RoundedCornerShape(2.dp))
					.background(eventColor)
			)

			Spacer(modifier = Modifier.width(12.dp))

			// 내용
			Column(
				modifier = Modifier.weight(1f)
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = event.title,
						style = MaterialTheme.typography.bodyMedium,
						fontWeight = FontWeight.SemiBold,
						maxLines = 1,
						//overflow = TextOverflow.Ellipsis
					)
					Spacer(modifier = Modifier.width(4.dp))
					Text(
						text = if (event.isAllDay) {
							"종일"
						} else {
							"${event.startTime.format(timeFormatter)} - ${event.endTime.format(timeFormatter)}"
						},
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.outline
					)

					if (event.location.isNotBlank()) {
						Spacer(modifier = Modifier.width(8.dp))
						Icon(
							imageVector = Icons.Default.LocationOn,
							contentDescription = null,
							modifier = Modifier.size(12.dp),
							tint = MaterialTheme.colorScheme.outline
						)
						Text(
							text = event.location,
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.outline,
							maxLines = 1,
							//overflow = TextOverflow.Ellipsis,
							modifier = Modifier.weight(1f, fill = false)
						)
					}
				}

				Spacer(modifier = Modifier.height(2.dp))
				Text(
					text = event.description,
					style = MaterialTheme.typography.labelSmall,
					//color = eventColor,
					maxLines = 2,
					//overflow = TextOverflow.Ellipsis
				)
				Spacer(modifier = Modifier.height(2.dp))

				// 캘린더 이름
				Text(
					text = event.calendarDisplayName,
					style = MaterialTheme.typography.labelSmall,
					color = eventColor,
					maxLines = 1,
					//overflow = TextOverflow.Ellipsis
				)
			}

			// 삭제 버튼
			IconButton(
				onClick = onDelete,
				modifier = Modifier.size(32.dp)
			) {
				Icon(
					imageVector = Icons.Default.Delete,
					contentDescription = "삭제",
					tint = MaterialTheme.colorScheme.error,
					modifier = Modifier.size(18.dp)
				)
			}
		}
	}
}

// 반복 옵션 enum
private enum class RepeatOption(val label: String, val freq: String?) {
    NONE("반복 안함", null),
    DAILY("매일", "DAILY"),
    WEEKLY("매주", "WEEKLY"),
    MONTHLY("매월", "MONTHLY"),
    YEARLY("매년", "YEARLY")
}

// 반복 종료 조건 enum
private enum class RepeatEndOption(val label: String) {
    FOREVER("계속 반복"),
    COUNT("반복 횟수"),
    UNTIL("종료 날짜")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarEventEditDialog(
	event: CalendarEvent?,
	date: LocalDate,
	availableCalendars: List<DeviceCalendar>,
	onDismiss: () -> Unit,
	onSave: (CalendarEvent) -> Unit
) {
	var title by remember { mutableStateOf(event?.title ?: "") }
	var description by remember { mutableStateOf(event?.description ?: "") }
	var location by remember { mutableStateOf(event?.location ?: "") }
	var selectedCalendarId by remember {
		mutableLongStateOf(event?.calendarId ?: availableCalendars.firstOrNull()?.id ?: 0L)
	}
	var startDate by remember { mutableStateOf(event?.startDate ?: date) }
	var endDate by remember { mutableStateOf(event?.endDate ?: date) }
	var startTime by remember {
		mutableStateOf(event?.startTime?.toLocalTime() ?: LocalTime.of(9, 0))
	}
	var endTime by remember {
		mutableStateOf(event?.endTime?.toLocalTime() ?: LocalTime.of(10, 0))
	}
	var isAllDay by remember { mutableStateOf(event?.isAllDay ?: false) }

	// RRULE 파싱하여 초기값 설정
	val parsedRrule = remember(event?.rrule) { parseRrule(event?.rrule) }
	var repeatOption by remember { mutableStateOf(parsedRrule.repeatOption) }
	var repeatEndOption by remember { mutableStateOf(parsedRrule.endOption) }
	var repeatCount by remember { mutableStateOf(parsedRrule.count.toString()) }
	var repeatUntilDate by remember { mutableStateOf(parsedRrule.untilDate ?: date.plusMonths(1)) }

	var showStartDatePicker by remember { mutableStateOf(false) }
	var showEndDatePicker by remember { mutableStateOf(false) }
	var showStartTimePicker by remember { mutableStateOf(false) }
	var showEndTimePicker by remember { mutableStateOf(false) }
	var showRepeatUntilDatePicker by remember { mutableStateOf(false) }
	var calendarDropdownExpanded by remember { mutableStateOf(false) }
	var repeatDropdownExpanded by remember { mutableStateOf(false) }
	var repeatEndDropdownExpanded by remember { mutableStateOf(false) }

	val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
	val dateFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)")
	val selectedCalendar = availableCalendars.find { it.id == selectedCalendarId }

	Dialog(onDismissRequest = onDismiss) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			shape = RoundedCornerShape(16.dp)
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.verticalScroll(rememberScrollState())
					.padding(16.dp)
			) {
				// 제목
				Text(
					text = if (event != null) "일정 수정" else "새 일정",
					style = MaterialTheme.typography.titleLarge,
					fontWeight = FontWeight.Bold
				)

				Spacer(modifier = Modifier.height(16.dp))

				// 제목 입력
				OutlinedTextField(
					value = title,
					onValueChange = { title = it },
					label = { Text("제목") },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true
				)

				Spacer(modifier = Modifier.height(12.dp))

				// 캘린더 선택
				ExposedDropdownMenuBox(
					expanded = calendarDropdownExpanded,
					onExpandedChange = { calendarDropdownExpanded = !calendarDropdownExpanded }
				) {
					OutlinedTextField(
						value = selectedCalendar?.displayName ?: "캘린더 선택",
						onValueChange = {},
						readOnly = true,
						label = { Text("캘린더") },
						trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = calendarDropdownExpanded) },
						modifier = Modifier
							.fillMaxWidth()
							// 수정된 부분: AnchorType과 enabled를 명시적으로 전달
							.menuAnchor(
								type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
								enabled = true
							),
						leadingIcon = selectedCalendar?.let {
							{
								Box(
									modifier = Modifier
										.size(12.dp)
										.clip(CircleShape)
										.background(Color(it.color))
								)
							}
						}
					)

					ExposedDropdownMenu(
						expanded = calendarDropdownExpanded,
						onDismissRequest = { calendarDropdownExpanded = false }
					) {
						availableCalendars.forEach { calendar ->
							DropdownMenuItem(
								text = {
									Row(verticalAlignment = Alignment.CenterVertically) {
										Box(
											modifier = Modifier
												.size(12.dp)
												.clip(CircleShape)
												.background(Color(calendar.color))
										)
										Spacer(modifier = Modifier.width(8.dp))
										Text(calendar.displayName)
									}
								},
								onClick = {
									selectedCalendarId = calendar.id
									calendarDropdownExpanded = false
								}
							)
						}
					}
				}

				Spacer(modifier = Modifier.height(12.dp))

				// 종일 체크박스
				Row(
					modifier = Modifier.fillMaxWidth(),
					verticalAlignment = Alignment.CenterVertically
				) {
					Checkbox(
						checked = isAllDay,
						onCheckedChange = { isAllDay = it }
					)
					Text("종일")
				}

				Spacer(modifier = Modifier.height(8.dp))

				// 날짜 선택 (시작일 / 종료일)
				Row(
					modifier = Modifier
						.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					// 시작 날짜
					Box(modifier = Modifier.weight(1f)) {
						OutlinedTextField(
							value = startDate.format(dateFormatter),
							onValueChange = {},
							readOnly = true,
							label = { Text("시작일") },
							modifier = Modifier.fillMaxWidth(),
							leadingIcon = {
								Icon(
									imageVector = Icons.Default.DateRange,
									contentDescription = null,
									modifier = Modifier.size(18.dp)
								)
							}
						)
						Box(
							modifier = Modifier
								.matchParentSize()
								.clickable { showStartDatePicker = true }
						)
					}

					// 종료 날짜
					Box(modifier = Modifier.weight(1f)) {
						OutlinedTextField(
							value = endDate.format(dateFormatter),
							onValueChange = {},
							readOnly = true,
							label = { Text("종료일") },
							modifier = Modifier.fillMaxWidth(),
							leadingIcon = {
								Icon(
									imageVector = Icons.Default.DateRange,
									contentDescription = null,
									modifier = Modifier.size(18.dp)
								)
							}
						)
						Box(
							modifier = Modifier
								.matchParentSize()
								.clickable { showEndDatePicker = true }
						)
					}
				}

				// 시간 선택 (종일이 아닐 때만)
				if (!isAllDay) {
					Spacer(modifier = Modifier.height(8.dp))

					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp)
					) {
						// 시작 시간
						Box(modifier = Modifier.weight(1f)) {
							OutlinedTextField(
								value = startTime.format(timeFormatter),
								onValueChange = {},
								readOnly = true,
								label = { Text("시작 시간") },
								modifier = Modifier.fillMaxWidth()
							)
							Box(
								modifier = Modifier
									.matchParentSize()
									.clickable { showStartTimePicker = true }
							)
						}

						// 종료 시간
						Box(modifier = Modifier.weight(1f)) {
							OutlinedTextField(
								value = endTime.format(timeFormatter),
								onValueChange = {},
								readOnly = true,
								label = { Text("종료 시간") },
								modifier = Modifier.fillMaxWidth()
							)
							Box(
								modifier = Modifier
									.matchParentSize()
									.clickable { showEndTimePicker = true }
							)
						}
					}
				}

				Spacer(modifier = Modifier.height(12.dp))

				// 반복 옵션 선택
				ExposedDropdownMenuBox(
					expanded = repeatDropdownExpanded,
					onExpandedChange = { repeatDropdownExpanded = !repeatDropdownExpanded }
				) {
					OutlinedTextField(
						value = repeatOption.label,
						onValueChange = {},
						readOnly = true,
						label = { Text("반복") },
						trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatDropdownExpanded) },
						modifier = Modifier
							.fillMaxWidth()
							// 수정된 부분: AnchorType과 enabled를 명시적으로 전달
							.menuAnchor(
								type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
								enabled = true
							),
						leadingIcon = {
							Icon(
								imageVector = Icons.Default.Repeat,
								contentDescription = null,
								modifier = Modifier.size(18.dp)
							)
						}
					)

					ExposedDropdownMenu(
						expanded = repeatDropdownExpanded,
						onDismissRequest = { repeatDropdownExpanded = false }
					) {
						RepeatOption.entries.forEach { option ->
							DropdownMenuItem(
								text = { Text(option.label) },
								onClick = {
									repeatOption = option
									repeatDropdownExpanded = false
								}
							)
						}
					}
				}

				// 반복 종료 조건 (반복 옵션이 선택된 경우에만 표시)
				if (repeatOption != RepeatOption.NONE) {
					Spacer(modifier = Modifier.height(8.dp))

					// 반복 종료 조건 선택
					ExposedDropdownMenuBox(
						expanded = repeatEndDropdownExpanded,
						onExpandedChange = { repeatEndDropdownExpanded = !repeatEndDropdownExpanded }
					) {
						OutlinedTextField(
							value = repeatEndOption.label,
							onValueChange = {},
							readOnly = true,
							label = { Text("반복 종료") },
							trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatEndDropdownExpanded) },
							modifier = Modifier
								.fillMaxWidth()
								// 수정된 부분: AnchorType과 enabled를 명시적으로 전달
								.menuAnchor(
									type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
									enabled = true
								)
						)

						ExposedDropdownMenu(
							expanded = repeatEndDropdownExpanded,
							onDismissRequest = { repeatEndDropdownExpanded = false }
						) {
							RepeatEndOption.entries.forEach { option ->
								DropdownMenuItem(
									text = { Text(option.label) },
									onClick = {
										repeatEndOption = option
										repeatEndDropdownExpanded = false
									}
								)
							}
						}
					}

					// 반복 횟수 입력 (COUNT 선택 시)
					if (repeatEndOption == RepeatEndOption.COUNT) {
						Spacer(modifier = Modifier.height(8.dp))
						OutlinedTextField(
							value = repeatCount,
							onValueChange = { newValue ->
								// 숫자만 허용
								if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
									repeatCount = newValue
								}
							},
							label = { Text("반복 횟수") },
							modifier = Modifier.fillMaxWidth(),
							singleLine = true,
							suffix = { Text("회") }
						)
					}

					// 종료 날짜 선택 (UNTIL 선택 시)
					if (repeatEndOption == RepeatEndOption.UNTIL) {
						Spacer(modifier = Modifier.height(8.dp))
						Box(modifier = Modifier.fillMaxWidth()) {
							OutlinedTextField(
								value = repeatUntilDate.format(dateFormatter),
								onValueChange = {},
								readOnly = true,
								label = { Text("반복 종료일") },
								modifier = Modifier.fillMaxWidth(),
								leadingIcon = {
									Icon(
										imageVector = Icons.Default.DateRange,
										contentDescription = null,
										modifier = Modifier.size(18.dp)
									)
								}
							)
							Box(
								modifier = Modifier
									.matchParentSize()
									.clickable { showRepeatUntilDatePicker = true }
							)
						}
					}
				}

				Spacer(modifier = Modifier.height(12.dp))

				// 장소 입력
				OutlinedTextField(
					value = location,
					onValueChange = { location = it },
					label = { Text("장소") },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true,
					leadingIcon = {
						Icon(
							imageVector = Icons.Default.LocationOn,
							contentDescription = null
						)
					}
				)

				Spacer(modifier = Modifier.height(12.dp))

				// 설명 입력
				OutlinedTextField(
					value = description,
					onValueChange = { description = it },
					label = { Text("설명") },
					modifier = Modifier
						.fillMaxWidth()
						.height(80.dp),
					maxLines = 3
				)

				Spacer(modifier = Modifier.height(16.dp))

				// 버튼
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.End
				) {
					TextButton(onClick = onDismiss) {
						Text("취소")
					}
					Spacer(modifier = Modifier.width(8.dp))
					TextButton(
						onClick = {
							if (title.isNotBlank() && selectedCalendarId != 0L) {
								val startDateTime = if (isAllDay) {
									startDate.atStartOfDay()
								} else {
									startDate.atTime(startTime)
								}
								val endDateTime = if (isAllDay) {
									// 종일 이벤트의 경우 종료일 다음날 00:00으로 설정
									endDate.plusDays(1).atStartOfDay()
								} else {
									endDate.atTime(endTime)
								}

								// RRULE 빌드
								val rrule = buildRrule(
									repeatOption = repeatOption,
									repeatEndOption = repeatEndOption,
									repeatCount = repeatCount.toIntOrNull() ?: 10,
									repeatUntilDate = repeatUntilDate
								)

								val newEvent = CalendarEvent(
									id = event?.id ?: 0L,
									calendarId = selectedCalendarId,
									title = title,
									description = description,
									location = location,
									startTime = startDateTime,
									endTime = endDateTime,
									isAllDay = isAllDay,
									color = selectedCalendar?.color ?: 0,
									calendarDisplayName = selectedCalendar?.displayName ?: "",
									rrule = rrule
								)
								onSave(newEvent)
							}
						},
						enabled = title.isNotBlank() && selectedCalendarId != 0L
					) {
						Text(if (event != null) "수정" else "저장")
					}
				}
			}
		}
	}

	// 시작 시간 선택 다이얼로그
	if (showStartTimePicker) {
		TimePickerDialog(
			initialTime = startTime,
			onDismiss = { showStartTimePicker = false },
			onConfirm = { selectedTime ->
				startTime = selectedTime
				// 종료 시간이 시작 시간보다 이전이면 자동 조정
				if (endTime.isBefore(startTime)) {
					endTime = startTime.plusHours(1)
				}
				showStartTimePicker = false
			}
		)
	}

	// 종료 시간 선택 다이얼로그
	if (showEndTimePicker) {
		TimePickerDialog(
			initialTime = endTime,
			onDismiss = { showEndTimePicker = false },
			onConfirm = { selectedTime ->
				endTime = selectedTime
				showEndTimePicker = false
			}
		)
	}

	// 시작 날짜 선택 다이얼로그
	if (showStartDatePicker) {
		val startDatePickerState = rememberDatePickerState(
			initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
		)
		DatePickerDialog(
			onDismissRequest = { showStartDatePicker = false },
			confirmButton = {
				TextButton(
					onClick = {
						startDatePickerState.selectedDateMillis?.let { millis ->
							val newStartDate = Instant.ofEpochMilli(millis)
								.atZone(ZoneId.of("UTC"))
								.toLocalDate()
							startDate = newStartDate
							// 종료일이 시작일보다 이전이면 자동 조정
							if (endDate.isBefore(startDate)) {
								endDate = startDate
							}
						}
						showStartDatePicker = false
					}
				) {
					Text("확인")
				}
			},
			dismissButton = {
				TextButton(onClick = { showStartDatePicker = false }) {
					Text("취소")
				}
			}
		) {
			// 이 부분에 animateContentSize를 추가하는 것이 핵심입니다.

			DatePicker(state = startDatePickerState,showModeToggle = false)
		}
	}

	// 종료 날짜 선택 다이얼로그
	if (showEndDatePicker) {
		val endDatePickerState = rememberDatePickerState(
			initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
		)
		DatePickerDialog(
			onDismissRequest = { showEndDatePicker = false },
			confirmButton = {
				TextButton(
					onClick = {
						endDatePickerState.selectedDateMillis?.let { millis ->
							val newEndDate = Instant.ofEpochMilli(millis)
								.atZone(ZoneId.of("UTC"))
								.toLocalDate()
							// 종료일이 시작일보다 이전이면 시작일로 설정
							endDate = if (newEndDate.isBefore(startDate)) startDate else newEndDate
						}
						showEndDatePicker = false
					}
				) {
					Text("확인")
				}
			},
			dismissButton = {
				TextButton(onClick = { showEndDatePicker = false }) {
					Text("취소")
				}
			}
		) {
			DatePicker(state = endDatePickerState,showModeToggle = false)
		}
	}

	// 반복 종료 날짜 선택 다이얼로그
	if (showRepeatUntilDatePicker) {
		val repeatUntilDatePickerState = rememberDatePickerState(
			initialSelectedDateMillis = repeatUntilDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
		)
		DatePickerDialog(
			onDismissRequest = { showRepeatUntilDatePicker = false },
			confirmButton = {
				TextButton(
					onClick = {
						repeatUntilDatePickerState.selectedDateMillis?.let { millis ->
							val newUntilDate = Instant.ofEpochMilli(millis)
								.atZone(ZoneId.of("UTC"))
								.toLocalDate()
							// 반복 종료일이 시작일보다 이후여야 함
							repeatUntilDate = if (newUntilDate.isAfter(startDate)) newUntilDate else startDate.plusDays(1)
						}
						showRepeatUntilDatePicker = false
					}
				) {
					Text("확인")
				}
			},
			dismissButton = {
				TextButton(onClick = { showRepeatUntilDatePicker = false }) {
					Text("취소")
				}
			}
		) {
			// 이 부분에 animateContentSize를 추가하는 것이 핵심입니다.

			DatePicker(state = repeatUntilDatePickerState,showModeToggle = false)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
	initialTime: LocalTime,
	onDismiss: () -> Unit,
	onConfirm: (LocalTime) -> Unit
) {
	val timePickerState = rememberTimePickerState(
		initialHour = initialTime.hour,
		initialMinute = initialTime.minute,
		is24Hour = true
	)

	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			TextButton(
				onClick = {
					onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
				}
			) {
				Text("확인")
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text("취소")
			}
		},
		text = {
			TimePicker(state = timePickerState)
		}
	)
}

// RRULE 파싱 결과 데이터 클래스
private data class ParsedRrule(
	val repeatOption: RepeatOption,
	val endOption: RepeatEndOption,
	val count: Int,
	val untilDate: LocalDate?
)

// RRULE 문자열 파싱
private fun parseRrule(rrule: String?): ParsedRrule {
	if (rrule == null) {
		return ParsedRrule(RepeatOption.NONE, RepeatEndOption.FOREVER, 10, null)
	}

	// FREQ 파싱
	val freqMatch = Regex("FREQ=(\\w+)").find(rrule)
	val repeatOption = when (freqMatch?.groupValues?.get(1)) {
		"DAILY" -> RepeatOption.DAILY
		"WEEKLY" -> RepeatOption.WEEKLY
		"MONTHLY" -> RepeatOption.MONTHLY
		"YEARLY" -> RepeatOption.YEARLY
		else -> RepeatOption.NONE
	}

	// COUNT 파싱
	val countMatch = Regex("COUNT=(\\d+)").find(rrule)
	val count = countMatch?.groupValues?.get(1)?.toIntOrNull()

	// UNTIL 파싱 (형식: YYYYMMDD 또는 YYYYMMDDTHHMMSSZ)
	val untilMatch = Regex("UNTIL=(\\d{8})").find(rrule)
	val untilDate = untilMatch?.groupValues?.get(1)?.let { dateStr ->
		try {
			LocalDate.of(
				dateStr.take(4).toInt(),
				dateStr.substring(4, 6).toInt(),
				dateStr.substring(6, 8).toInt()
			)
		} catch (e: Exception) {
			null
		}
	}

	val endOption = when {
		count != null -> RepeatEndOption.COUNT
		untilDate != null -> RepeatEndOption.UNTIL
		else -> RepeatEndOption.FOREVER
	}

	return ParsedRrule(
		repeatOption = repeatOption,
		endOption = endOption,
		count = count ?: 10,
		untilDate = untilDate
	)
}

// RRULE 문자열 빌드
@SuppressLint("DefaultLocale")
private fun buildRrule(
	repeatOption: RepeatOption,
	repeatEndOption: RepeatEndOption,
	repeatCount: Int,
	repeatUntilDate: LocalDate
): String? {
	if (repeatOption == RepeatOption.NONE || repeatOption.freq == null) {
		return null
	}

	val sb = StringBuilder("FREQ=${repeatOption.freq}")

	when (repeatEndOption) {
		RepeatEndOption.FOREVER -> {
			// 종료 조건 없음
		}
		RepeatEndOption.COUNT -> {
			sb.append(";COUNT=$repeatCount")
		}
		RepeatEndOption.UNTIL -> {
			// UNTIL 형식: YYYYMMDD
			val untilStr = String.format(
				"%04d%02d%02d",
				repeatUntilDate.year,
				repeatUntilDate.monthValue,
				repeatUntilDate.dayOfMonth
			)
			sb.append(";UNTIL=$untilStr")
		}
	}

	return sb.toString()
}

@Composable
private fun HolidayEditDialog(
	currentName: String?,
	isUserCreated: Boolean,
	onDismiss: () -> Unit,
	onSave: (String) -> Unit,
	onDelete: () -> Unit
) {
	var holidayName by remember { mutableStateOf(currentName ?: "") }
	val isEditMode = currentName != null

	AlertDialog(
		onDismissRequest = onDismiss,
		title = {
			Text(
				text = if (isEditMode) "공휴일 편집" else "공휴일 추가",
				fontWeight = FontWeight.Bold
			)
		},
		text = {
			Column {
				OutlinedTextField(
					value = holidayName,
					onValueChange = { holidayName = it },
					label = { Text("공휴일 이름") },
					placeholder = { Text("예: 설날, 추석") },
					singleLine = true,
					modifier = Modifier.fillMaxWidth()
				)

				// 서버 공휴일인 경우 안내 메시지
				if (isEditMode && !isUserCreated) {
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "※ 서버에서 받아온 공휴일입니다.\n수정하면 사용자 공휴일로 변환됩니다.",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.outline
					)
				}
			}
		},
		confirmButton = {
			TextButton(
				onClick = { onSave(holidayName) },
				enabled = holidayName.isNotBlank()
			) {
				Text(if (isEditMode) "수정" else "추가")
			}
		},
		dismissButton = {
			Row {
				// 삭제 버튼 (편집 모드일 때만 표시)
				if (isEditMode) {
					TextButton(onClick = onDelete) {
						Text("삭제", color = MaterialTheme.colorScheme.error)
					}
				}
				TextButton(onClick = onDismiss) {
					Text("취소")
				}
			}
		}
	)
}

/**
 * 충당 입력 다이얼로그
 * - 충당 종류 선택 (대기충당/초록, 휴무충당/보라, 지근충당/하늘)
 * - 교체할 교번 선택
 * - 충당 일수 선택 (1~2일, 기본값 1일)
 * - 지근충당은 지근이 설정된 날짜에만 선택 가능
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftInputDialog(
    date: LocalDate,
    currentShiftName: String,
    availableShifts: List<String>,
    shiftPattern: List<String>,
    shiftInputTypes: List<ShiftInputType>,
    existingRecord: ShiftInputRecord?,
    hasLateWork: Boolean,
    onConfirm: (ShiftInputType, targetShift: String, days: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    // 지근충당 필터링: 지근이 설정된 날짜에만 사용 가능한 타입 필터
    val availableTypes = shiftInputTypes.filter { type ->
        if (type.requiresLateWork) hasLateWork else true
    }

    var selectedType by remember { mutableStateOf(availableTypes.firstOrNull()) }
    var selectedShift by remember { mutableStateOf(availableShifts.firstOrNull() ?: "") }
    var selectedDays by remember { mutableIntStateOf(1) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var shiftDropdownExpanded by remember { mutableStateOf(false) }
    var daysDropdownExpanded by remember { mutableStateOf(false) }

    // 선택한 교번이 순환 패턴에 포함되어 있는지 여부
    // 패턴에 없는 근무는 순환 계산 불가 → 1일만 입력 가능
    val isInPattern = shiftPattern.contains(selectedShift)
    LaunchedEffect(selectedShift) {
        if (!isInPattern) selectedDays = 1
    }

    val dateFormatter = DateTimeFormatter.ofPattern("M월 d일")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "충당",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 현재 교번 + 시작일 표시
                Text(
                    text = "현재 교번: $currentShiftName | 시작일: ${date.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 기존 충당 정보 표시
                if (existingRecord != null) {
                    val recordColor = try {
                        Color(existingRecord.colorHex.androidToColorInt())
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = recordColor.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "현재 충당: ${existingRecord.shiftInputTypeName} → ${existingRecord.targetShiftName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 충당 종류 선택 드롭다운
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType?.name ?: "선택",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("충당 종류") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        leadingIcon = selectedType?.let {
                            {
                                val typeColor = try {
                                    Color(it.colorHex.androidToColorInt())
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(typeColor)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                    )

                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        availableTypes.forEach { type ->
                            val typeColor = try {
                                Color(type.colorHex.androidToColorInt())
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(typeColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(type.name)
                                        if (type.requiresLateWork) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(지근 필요)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }

                        // 지근충당이 목록에 없는 경우 안내
                        if (!hasLateWork) {
                            val lateWorkType = shiftInputTypes.find { it.requiresLateWork }
                            if (lateWorkType != null) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val disabledColor = MaterialTheme.colorScheme.outline
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(disabledColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${lateWorkType.name} (지근 미설정)",
                                                color = disabledColor
                                            )
                                        }
                                    },
                                    onClick = { /* 클릭 불가 */ },
                                    enabled = false
                                )
                            }
                        }
                    }
                }

                // 교체할 교번 선택 드롭다운
                ExposedDropdownMenuBox(
                    expanded = shiftDropdownExpanded,
                    onExpandedChange = { shiftDropdownExpanded = !shiftDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedShift,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("교체할 교번") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                    )

                    ExposedDropdownMenu(
                        expanded = shiftDropdownExpanded,
                        onDismissRequest = { shiftDropdownExpanded = false }
                    ) {
                        availableShifts.forEach { shift ->
                            DropdownMenuItem(
                                text = { Text(shift) },
                                onClick = {
                                    selectedShift = shift
                                    shiftDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 충당 일수 드롭다운 (1~2일, 기본값 1일)
                // 패턴에 없는 근무 선택 시: 1일로 고정, 드롭다운 비활성화
                ExposedDropdownMenuBox(
                    expanded = daysDropdownExpanded && isInPattern,
                    onExpandedChange = {
                        if (isInPattern) daysDropdownExpanded = !daysDropdownExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = "${selectedDays}일",
                        onValueChange = {},
                        readOnly = true,
                        enabled = isInPattern,
                        label = { Text("야간은 2일") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = daysDropdownExpanded && isInPattern) },
                        supportingText = {
                            when {
                                !isInPattern -> Text("패턴에 없는 근무는 1일만 입력 가능합니다")
                                selectedDays > 1 -> {
                                    val endDate = date.plusDays((selectedDays - 1).toLong())
                                    Text("${date.format(dateFormatter)} ~ ${endDate.format(dateFormatter)}")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = isInPattern
                            )
                    )

                    ExposedDropdownMenu(
                        expanded = daysDropdownExpanded && isInPattern,
                        onDismissRequest = { daysDropdownExpanded = false }
                    ) {
                        // 1~2일만 선택 가능
                        (1..2).forEach { day ->
                            DropdownMenuItem(
                                text = { Text("${day}일") },
                                onClick = {
                                    selectedDays = day
                                    daysDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val type = selectedType ?: return@TextButton
                    onConfirm(type, selectedShift, selectedDays)
                },
                enabled = selectedType != null && selectedShift.isNotBlank()
            ) {
                Text("입력")
            }
        },
        dismissButton = {
            Row {
                if (existingRecord != null) {
                    TextButton(onClick = onDelete) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}

@Composable
private fun ModernShiftButton(
	text: String,
	icon: ImageVector,
	backgroundColor: Color,
	contentColor: Color,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Surface(
		onClick = onClick,
		modifier = modifier.height(44.dp), // 일정한 높이 유지
		shape = RoundedCornerShape(12.dp), // 좀 더 부드러운 곡률
		color = backgroundColor,
		contentColor = contentColor,
		tonalElevation = 2.dp, // 살짝 입체감 부여
		shadowElevation = 1.dp
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Center
		) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				modifier = Modifier.size(18.dp)
			)
			Spacer(modifier = Modifier.width(6.dp))
			Text(
				text = text,
				fontWeight = FontWeight.Bold,
				style = MaterialTheme.typography.labelMedium,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.weight(1f, fill = false)
			)
		}
	}
}

@Composable
private fun FullScreenImageDialog(
	imagePath: String,
	onDismiss: () -> Unit
) {
	var scale by remember { mutableFloatStateOf(1f) }
	var offset by remember { mutableStateOf(Offset.Zero) }
	val context = LocalContext.current

	Dialog(
		onDismissRequest = onDismiss,
		properties = DialogProperties(usePlatformDefaultWidth = false)
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Black)
				.pointerInput(Unit) {
					detectTransformGestures { _, pan, zoom, _ ->
						scale = (scale * zoom).coerceIn(1f, 5f)
						if (scale > 1f) {
							offset = Offset(
								x = offset.x + pan.x,
								y = offset.y + pan.y
							)
						} else {
							offset = Offset.Zero
						}
					}
				}
				.clickable {
					if (scale == 1f) onDismiss()
					else {
						scale = 1f
						offset = Offset.Zero
					}
				},
			contentAlignment = Alignment.Center
		) {
			AsyncImage(
				model = ImageRequest.Builder(context)
					.data(File(imagePath))
					.crossfade(true)
					.build(),
				contentDescription = "전체화면 이미지",
				contentScale = ContentScale.Fit,
				modifier = Modifier
					.fillMaxSize()
					.graphicsLayer {
						scaleX = scale
						scaleY = scale
						translationX = offset.x
						translationY = offset.y
					}
			)

			IconButton(
				onClick = onDismiss,
				modifier = Modifier
					.align(Alignment.TopEnd)
					.padding(16.dp)
					.background(
						color = Color.Black.copy(alpha = 0.5f),
						shape = CircleShape
					)
			) {
				Icon(
					imageVector = Icons.Default.Close,
					contentDescription = "닫기",
					tint = Color.White
				)
			}
		}
	}
}

private fun appendDateParam(url: String, date: java.time.LocalDate): String {
	val dateStr = date.toString() // YYYY-MM-DD
	val separator = if (url.contains('?')) '&' else '?'
	return "$url${separator}date=$dateStr"
}

@Composable
private fun OfficeWebsitePasswordDialog(
	officeName: String,
	onDismiss: () -> Unit,
	onConfirm: (String) -> Boolean,
) {
	var password by remember { mutableStateOf("") }
	var isError by remember { mutableStateOf(false) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("비밀번호 입력") },
		text = {
			Column {
				Text(
					text = "$officeName 사이트에 접근하려면 비밀번호를 입력하세요.",
					style = MaterialTheme.typography.bodyMedium
				)
				Spacer(modifier = Modifier.height(12.dp))
				OutlinedTextField(
					value = password,
					onValueChange = {
						password = it
						isError = false
					},
					label = { Text("비밀번호") },
					singleLine = true,
					isError = isError,
					visualTransformation = PasswordVisualTransformation(),
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
					supportingText = if (isError) {
						{ Text("비밀번호가 일치하지 않습니다.") }
					} else null,
					modifier = Modifier.fillMaxWidth()
				)
			}
		},
		confirmButton = {
			TextButton(
				onClick = {
					val ok = onConfirm(password)
					if (!ok) isError = true
				},
				enabled = password.isNotEmpty()
			) {
				Text("확인")
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text("취소")
			}
		}
	)
}
