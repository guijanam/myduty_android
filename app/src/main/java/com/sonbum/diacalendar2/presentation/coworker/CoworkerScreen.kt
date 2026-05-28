package com.sonbum.diacalendar2.presentation.coworker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonbum.diacalendar2.domain.model.Coworker
import com.sonbum.diacalendar2.domain.model.CoworkerGroup
import com.sonbum.diacalendar2.presentation.shared.ShiftBadge
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoworkerScreen(
    modifier: Modifier = Modifier,
    onNavigateToCoworkerEdit: (Long?) -> Unit,
    onNavigateToGroupManage: () -> Unit,
    viewModel: CoworkerViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        when (state.selectedTab) {
                            CoworkerTab.CALENDAR -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val prev = YearMonth.of(state.currentYear, state.currentMonth).minusMonths(1)
                                            viewModel.onMonthChanged(prev.year, prev.monthValue)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달", modifier = Modifier.size(20.dp))
                                    }
                                    Text(
                                        text = "${state.currentYear}년 ${state.currentMonth}월",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            val next = YearMonth.of(state.currentYear, state.currentMonth).plusMonths(1)
                                            viewModel.onMonthChanged(next.year, next.monthValue)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "다음 달", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            CoworkerTab.LIST -> {
                                Text(
                                    text = "동료 목록",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    actions = {
                        // 그룹 필터 드롭다운 (공통)
                        ExposedDropdownMenuBox(
                            expanded = groupDropdownExpanded,
                            onExpandedChange = { groupDropdownExpanded = it },
                            modifier = Modifier.width(110.dp)
                        ) {
                            OutlinedTextField(
                                value = state.groups.firstOrNull { it.id == state.selectedGroupId }?.name ?: "전체",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = groupDropdownExpanded,
                                onDismissRequest = { groupDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("전체") },
                                    onClick = {
                                        viewModel.onGroupSelected(null)
                                        groupDropdownExpanded = false
                                    }
                                )
                                state.groups.forEach { group ->
                                    DropdownMenuItem(
                                        text = { Text(group.name) },
                                        onClick = {
                                            viewModel.onGroupSelected(group.id)
                                            groupDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        // 그룹 관리 버튼
                        IconButton(onClick = onNavigateToGroupManage) {
                            Icon(Icons.Default.Groups, contentDescription = "그룹 관리")
                        }
                    }
                )
                // 탭 바
                PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                    Tab(
                        selected = state.selectedTab == CoworkerTab.CALENDAR,
                        onClick = { viewModel.onTabSelected(CoworkerTab.CALENDAR) },
                        text = { Text("달력") },
                        //icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = state.selectedTab == CoworkerTab.LIST,
                        onClick = { viewModel.onTabSelected(CoworkerTab.LIST) },
                        text = { Text("목록") },
                        //icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToCoworkerEdit(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("동료 추가") }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        when (state.selectedTab) {
            CoworkerTab.CALENDAR -> CoworkerCalendarTab(
                innerPadding = innerPadding,
                state = state
            )
            CoworkerTab.LIST -> CoworkerListTab(
                innerPadding = innerPadding,
                coworkers = state.filteredCoworkers,
                groups = state.groups,
                onEditCoworker = { onNavigateToCoworkerEdit(it) },
                onReorder = viewModel::reorderCoworkers
            )
        }
    }
}

// ─── 달력 탭 ───────────────────────────────────────────────────────────────

@Composable
private fun CoworkerCalendarTab(
    innerPadding: PaddingValues,
    state: CoworkerUiState
) {
    if (state.coworkers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "등록된 동료가 없습니다",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "하단 + 버튼으로 동료를 추가하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        CoworkerCalendarGrid(
            modifier = Modifier.padding(innerPadding),
            year = state.currentYear,
            month = state.currentMonth,
            myScheduleMap = state.myScheduleMap,
            coworkers = state.filteredCoworkers,
            coworkerSchedules = state.coworkerSchedules,
            holidayMap = state.holidayMap
        )
    }
}

// ─── 목록 탭 ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoworkerListTab(
    innerPadding: PaddingValues,
    coworkers: List<Coworker>,
    groups: List<CoworkerGroup>,
    onEditCoworker: (Long) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    if (coworkers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "등록된 동료가 없습니다",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "하단 + 버튼으로 동료를 추가하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        val listState = rememberLazyListState()
        val haptic = LocalHapticFeedback.current
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            onReorder(from.index, to.index)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(coworkers, key = { it.id }) { coworker ->
                ReorderableItem(state = reorderState, key = coworker.id) { isDragging ->
                    CoworkerListCard(
                        coworker = coworker,
                        groups = groups,
                        isDragging = isDragging,
                        onEdit = { onEditCoworker(coworker.id) },
                        dragHandle = {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "순서 변경",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .size(24.dp)
                                    .draggableHandle(
                                        onDragStarted = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    )
                            )
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoworkerListCard(
    coworker: Coworker,
    groups: List<CoworkerGroup>,
    isDragging: Boolean,
    onEdit: () -> Unit,
    dragHandle: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.surfaceContainerHigh
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 6.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 드래그 핸들
                dragHandle()
                Spacer(Modifier.width(8.dp))
                // 아바타
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = coworker.name.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                // 이름
                Text(
                    text = coworker.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // 편집 버튼
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "편집",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 소속 그룹 태그
            val coworkerGroups = groups.filter { it.id in coworker.groupIds }
            if (coworkerGroups.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 32.dp)
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        coworkerGroups.forEach { group ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── 달력 그리드 ────────────────────────────────────────────────────────────

// 행 높이: 날짜 헤더(14dp) + 내근무(18dp) + 동료 N명(18dp * N) + 상하패딩(4dp)
private fun rowHeightDp(coworkerCount: Int) = 14 + 18 + 18 * coworkerCount + 4

@Composable
private fun CoworkerCalendarGrid(
    modifier: Modifier = Modifier,
    year: Int,
    month: Int,
    myScheduleMap: Map<LocalDate, String>,
    coworkers: List<Coworker>,
    coworkerSchedules: Map<Long, Map<LocalDate, String>>,
    holidayMap: Map<LocalDate, String> = emptyMap()
) {
    val yearMonth = YearMonth.of(year, month)
    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = (firstDay.dayOfWeek.value % 7) // 일=0
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7
    val rowHeight = rowHeightDp(coworkers.size).dp
    val nameColWidth = 44.dp
    val days = listOf("일", "월", "화", "수", "목", "금", "토")

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // 날짜 클릭 시 팝업
    selectedDate?.let { date ->
        CoworkerDayDetailDialog(
            date = date,
            myShift = myScheduleMap[date],
            coworkers = coworkers,
            coworkerSchedules = coworkerSchedules,
            onDismiss = { selectedDate = null }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ── 헤더: 이름열 + 요일열 ──────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            // 이름 열 헤더 (빈 공간)
            Box(
                modifier = Modifier
                    .width(nameColWidth)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(vertical = 4.dp)
            )
            // 요일 헤더
            days.forEachIndexed { index, day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (index) {
                            0 -> MaterialTheme.colorScheme.error
                            6 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }

        // ── 달력 본문 ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            (0 until rows).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                ) {
                    // ── 왼쪽 이름 열 ──────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .width(nameColWidth)
                            .height(rowHeight)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        // 날짜 자리 높이 맞춤 (14dp)
                        Spacer(Modifier.height(14.dp))
                        // 내 이름 행
                        Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.CenterStart) {
                            Text(
                                text = "나",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // 동료 이름 행
                        coworkers.forEach { coworker ->
                            Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.CenterStart) {
                                Text(
                                    text = coworker.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // ── 날짜 셀 7개 ───────────────────────────────────────
                    (0 until 7).forEach { col ->
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - startOffset + 1
                        Box(modifier = Modifier.weight(1f).height(rowHeight)) {
                            if (dayNumber in 1..daysInMonth) {
                                val date = yearMonth.atDay(dayNumber)
                                CoworkerDayCell(
                                    date = date,
                                    isToday = date == LocalDate.now(),
                                    isSunday = col == 0,
                                    isSaturday = col == 6,
                                    isHoliday = holidayMap.containsKey(date),
                                    myShift = myScheduleMap[date],
                                    coworkers = coworkers,
                                    coworkerSchedules = coworkerSchedules,
                                    rowHeight = rowHeight,
                                    onClick = { selectedDate = date }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(88.dp))
        }
    }
}

@Composable
private fun CoworkerDayCell(
    date: LocalDate,
    isToday: Boolean,
    isSunday: Boolean,
    isSaturday: Boolean,
    isHoliday: Boolean,
    myShift: String?,
    coworkers: List<Coworker>,
    coworkerSchedules: Map<Long, Map<LocalDate, String>>,
    rowHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit = {}
) {
    val borderColor = if (isToday) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val borderWidth = if (isToday) 1.5.dp else 0.5.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .border(borderWidth, borderColor)
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 날짜 (14dp)
        Box(modifier = Modifier.height(14.dp), contentAlignment = Alignment.Center) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                color = when {
                    isToday -> MaterialTheme.colorScheme.primary
                    isSunday || isHoliday -> MaterialTheme.colorScheme.error
                    isSaturday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
        // 내 근무 (18dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (myShift != null) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            if (myShift != null) ShiftBadge(shiftName = myShift, fontSize = 10f)
        }
        // 동료 근무 (18dp × N)
        coworkers.forEach { coworker ->
            val shift = coworkerSchedules[coworker.id]?.get(date)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (shift != null) ShiftBadge(shiftName = shift, fontSize = 9f)
            }
        }
    }
}

// ─── 날짜 상세 팝업 ──────────────────────────────────────────────────────────

@Composable
private fun CoworkerDayDetailDialog(
    date: LocalDate,
    myShift: String?,
    coworkers: List<Coworker>,
    coworkerSchedules: Map<Long, Map<LocalDate, String>>,
    onDismiss: () -> Unit
) {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    val title = "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일 ($dayOfWeek)"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 내 근무
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "나",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "나",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (myShift != null) {
                        ShiftBadge(shiftName = myShift, fontSize = 14f)
                    } else {
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                HorizontalDivider()

                // 동료 근무
                coworkers.forEach { coworker ->
                    val shift = coworkerSchedules[coworker.id]?.get(date)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = coworker.name.take(1),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = coworker.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (shift != null) {
                            ShiftBadge(shiftName = shift, fontSize = 14f)
                        } else {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    )
}
