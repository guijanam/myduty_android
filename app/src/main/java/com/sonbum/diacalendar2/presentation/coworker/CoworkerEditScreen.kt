package com.sonbum.diacalendar2.presentation.coworker

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonbum.diacalendar2.domain.model.CustomShift
import com.sonbum.diacalendar2.domain.model.Office
import com.sonbum.diacalendar2.domain.model.UserShiftConfig
import com.sonbum.diacalendar2.presentation.shared.ShiftBadge
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoworkerEditScreen(
    coworkerId: Long?,
    onBack: () -> Unit,
    viewModel: CoworkerEditViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showReferenceDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(coworkerId) {
        viewModel.initialize(coworkerId)
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                CoworkerEditEvent.SaveSuccess -> onBack()
                CoworkerEditEvent.DeleteSuccess -> onBack()
                is CoworkerEditEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (coworkerId == null) "동료 추가" else "동료 편집") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (coworkerId != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "삭제",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Step 1: 이름 ──────────────────────────────────────
            SectionTitle(number = 1, title = "이름")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("동료 이름") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // ── Step 2: 소속 그룹 ─────────────────────────────────
            SectionTitle(number = 2, title = "소속 그룹")
            Spacer(Modifier.height(8.dp))
            if (state.allGroups.isEmpty()) {
                DisabledPlaceholder(text = "그룹이 없습니다. 상단 그룹 관리에서 먼저 그룹을 추가하세요.")
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    state.allGroups.forEach { group ->
                        val selected = group.id in state.selectedGroupIds
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.onGroupToggle(group.id) },
                            label = { Text(group.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Step 3: 근무 패턴 소스 선택 ───────────────────────
            SectionTitle(number = 3, title = "근무 패턴")
            Spacer(Modifier.height(8.dp))

            // 패턴 소스 FilterChip
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = state.patternSource == CoworkerPatternSource.MANUAL,
                    onClick = { viewModel.onPatternSourceChange(CoworkerPatternSource.MANUAL) },
                    label = { Text("직접입력") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = state.patternSource == CoworkerPatternSource.OFFICE,
                    onClick = { viewModel.onPatternSourceChange(CoworkerPatternSource.OFFICE) },
                    label = { Text("교번근무(서버)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = state.patternSource == CoworkerPatternSource.LOCAL,
                    onClick = { viewModel.onPatternSourceChange(CoworkerPatternSource.LOCAL) },
                    label = { Text("교번근무(로컬)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
                FilterChip(
                    selected = state.patternSource == CoworkerPatternSource.CUSTOM_SHIFT,
                    onClick = { viewModel.onPatternSourceChange(CoworkerPatternSource.CUSTOM_SHIFT) },
                    label = { Text("교대근무") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            when (state.patternSource) {
                CoworkerPatternSource.MANUAL -> {
                    // 직접 입력 텍스트 필드
                    OutlinedTextField(
                        value = state.shiftPatternInput,
                        onValueChange = viewModel::onPatternChange,
                        label = { Text("근무 패턴 (쉼표로 구분)") },
                        placeholder = { Text("예: 주,야,비,휴") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                CoworkerPatternSource.OFFICE, CoworkerPatternSource.LOCAL -> {
                    // 승무소 선택 드롭다운
                    CoworkerOfficeSearchDropdown(
                        searchQuery = state.searchQuery,
                        onSearchQueryChange = viewModel::onSearchQueryChange,
                        isExpanded = state.isDropdownExpanded,
                        onExpandedChange = viewModel::onDropdownExpandedChange,
                        offices = if (state.filteredOffices.isNotEmpty() || state.searchQuery.isNotEmpty())
                            state.filteredOffices
                        else if (state.patternSource == CoworkerPatternSource.OFFICE) state.offices
                        else state.localOffices,
                        selectedOffice = state.selectedOffice,
                        onOfficeSelected = viewModel::onOfficeSelected,
                        onClear = viewModel::clearOfficeSelection,
                        isLoading = state.isOfficesLoading
                    )

                    // 포지션 선택
                    if (state.selectedOffice != null) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "포지션 선택",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        CoworkerPositionSelector(
                            selectedPosition = state.selectedPosition,
                            onPositionSelected = viewModel::onPositionSelected,
                            office = state.selectedOffice!!
                        )
                    }

                }

                CoworkerPatternSource.CUSTOM_SHIFT -> {
                    // 교대근무 선택
                    CoworkerCustomShiftSelector(
                        customShifts = state.customShifts,
                        selectedCustomShift = state.selectedCustomShift,
                        onCustomShiftSelected = viewModel::onCustomShiftSelected
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Step 4: 승무소 모드일 때 시작 날짜 ──────────────────
            if (state.patternSource != CoworkerPatternSource.MANUAL) {
                SectionTitle(number = 4, title = "시작 날짜")
                Spacer(Modifier.height(8.dp))
                val startEnabled = state.parsedPattern.isNotEmpty()
                OutlinedCard(
                    onClick = { if (startEnabled) showStartDatePicker = true },
                    enabled = startEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (startEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val isToday = state.startDate == LocalDate.now()
                            Text(
                                text = if (isToday) "시작 날짜 (기본값:오늘)" else "시작 날짜",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = state.startDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (startEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "달력에서 동료 근무가 표시되기 시작하는 날짜입니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Step 4 or 5: 기준교번 선택 ───────────────────────────
            val baseShiftStepNumber = if (state.patternSource != CoworkerPatternSource.MANUAL) 5 else 4
            SectionTitle(number = baseShiftStepNumber, title = "기준교번 선택")
            Spacer(Modifier.height(8.dp))
            if (state.parsedPattern.isEmpty()) {
                DisabledPlaceholder(
                    text = when (state.patternSource) {
                        CoworkerPatternSource.MANUAL -> "먼저 근무 패턴을 입력하세요"
                        CoworkerPatternSource.OFFICE, CoworkerPatternSource.LOCAL -> "먼저 승무소와 포지션을 선택하세요"
                        CoworkerPatternSource.CUSTOM_SHIFT -> "먼저 교대근무를 선택하세요"
                    }
                )
            } else {
                CoworkerBaseShiftSelector(
                    availableShifts = state.availableShifts,
                    selectedShift = state.referenceShift.ifBlank { null },
                    selectedShiftIndex = state.referenceShiftAvailableIndex ?: state.referenceShiftIndex,
                    onShiftSelected = viewModel::onReferenceShiftChange,
                    referenceDate = state.referenceDate ?: LocalDate.now(),
                    onReferenceDateClick = { showReferenceDatePicker = true }
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── 마지막 Step: 저장 ─────────────────────────────────────
            val saveStepNumber = if (state.patternSource != CoworkerPatternSource.MANUAL) 6 else 5
            SectionTitle(number = saveStepNumber, title = "저장")
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::save,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("저장 중...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("동료 저장하기")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // 시작 날짜 DatePicker
    if (showStartDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = state.startDate
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { millis ->
                        viewModel.onStartDateChange(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                    }
                    showStartDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("취소") } }
        ) { DatePicker(state = dpState, showModeToggle = false) }
    }

    // 기준 날짜 DatePicker
    if (showReferenceDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = (state.referenceDate ?: LocalDate.now())
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showReferenceDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { millis ->
                        viewModel.onReferenceDateChange(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                    }
                    showReferenceDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showReferenceDatePicker = false }) { Text("취소") } }
        ) { DatePicker(state = dpState, showModeToggle = false) }
    }

    // 삭제 확인
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("동료 삭제") },
            text = { Text("\"${state.name}\"을(를) 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(); showDeleteConfirm = false }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") } }
        )
    }
}

// ─── SectionTitle ────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(number: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

// ─── 비활성 플레이스홀더 ────────────────────────────────────────────────────────

@Composable
private fun DisabledPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

// ─── 승무소 검색 드롭다운 ────────────────────────────────────────────────────────

@Composable
private fun CoworkerOfficeSearchDropdown(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    offices: List<Office>,
    selectedOffice: Office?,
    onOfficeSelected: (Office) -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("승무소 이름으로 검색") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색") },
            trailingIcon = {
                Row {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Clear, contentDescription = "지우기")
                        }
                    }
                    IconButton(onClick = { onExpandedChange(!isExpanded) }) {
                        Icon(
                            if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        if (isExpanded && !isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                if (offices.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "검색 결과가 없습니다" else "승무소 목록이 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    val sortedOffices = offices.sortedBy { it.officeName }
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(sortedOffices, key = { it.officeCode }) { office ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOfficeSelected(office); onExpandedChange(false) }
                                    .background(
                                        if (selectedOffice?.officeCode == office.officeCode)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isSelected = selectedOffice?.officeCode == office.officeCode
                                Icon(
                                    Icons.Default.Subway,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = office.officeName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── 포지션 선택 ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoworkerPositionSelector(
    selectedPosition: UserShiftConfig.Position?,
    onPositionSelected: (UserShiftConfig.Position) -> Unit,
    office: Office,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UserShiftConfig.Position.entries.forEach { position ->
            val isAvailable = when (position) {
                UserShiftConfig.Position.ENGINEER -> !office.diaTurns1.isNullOrBlank()
                UserShiftConfig.Position.CONDUCTOR -> !office.diaTurns2.isNullOrBlank()
                UserShiftConfig.Position.FOUR_SHIFT -> !office.subTurns.isNullOrBlank()
            }
            FilterChip(
                selected = selectedPosition == position,
                onClick = { if (isAvailable) onPositionSelected(position) },
                label = { Text(position.displayName) },
                enabled = isAvailable,
                leadingIcon = if (selectedPosition == position) {
                    { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

// ─── 교대근무 선택 ────────────────────────────────────────────────────────────

@Composable
private fun CoworkerCustomShiftSelector(
    customShifts: List<CustomShift>,
    selectedCustomShift: CustomShift?,
    onCustomShiftSelected: (CustomShift) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedCustomShift != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedCustomShift.shiftName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = selectedCustomShift.shiftPattern.joinToString(" → "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "교대근무를 선택하세요",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            if (customShifts.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "등록된 교대근무가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { expanded = false }
                )
            } else {
                customShifts.forEach { shift ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(shift.shiftName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    "${shift.shiftPattern.joinToString(",")} (${shift.shiftPattern.size}일 주기)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = { onCustomShiftSelected(shift); expanded = false }
                    )
                }
            }
        }
    }
}

// ─── 기준교번 선택기 ──────────────────────────────────────────────────────────

// 1~20: ①②③…⑳ (U+2460~U+2473), 21 이상은 (N) 형태로 fallback
private fun coworkerOccurrenceMark(occurrence: Int): String {
    if (occurrence <= 0) return ""
    return if (occurrence in 1..20) (0x2460 + (occurrence - 1)).toChar().toString()
    else "($occurrence)"
}

@Composable
private fun CoworkerBaseShiftSelector(
    availableShifts: List<String>,
    selectedShift: String?,
    selectedShiftIndex: Int?,
    onShiftSelected: (String, Int) -> Unit,
    referenceDate: LocalDate,
    onReferenceDateClick: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }
    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("MM월 dd일") }
    var expanded by remember { mutableStateOf(false) }
    val isToday = referenceDate == LocalDate.now()

    val selectedDisplayText = remember(selectedShift, selectedShiftIndex, availableShifts) {
        if (selectedShift == null || selectedShiftIndex == null) selectedShift
        else {
            val count = availableShifts.count { it == selectedShift }
            if (count > 1) {
                val occ = availableShifts.take(selectedShiftIndex + 1).count { it == selectedShift }
                "$selectedShift ${coworkerOccurrenceMark(occ)}"
            } else selectedShift
        }
    }

    Column {
        Text(
            text = "기준 날짜에 해당하는 근무를 선택하세요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(8.dp))

        OutlinedCard(onClick = onReferenceDateClick, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isToday) "기준 날짜 (오늘)" else "기준 날짜",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = referenceDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(Modifier.height(12.dp))

        Box {
            OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Work,
                            contentDescription = null,
                            tint = if (selectedShift != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = selectedDisplayText ?: "기준교번 선택",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedShift != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedShift != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(
                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                val shiftCounts = availableShifts.groupingBy { it }.eachCount()
                val shiftOccurrence = mutableMapOf<String, Int>()
                availableShifts.forEachIndexed { index, shift ->
                    val count = shiftCounts[shift] ?: 1
                    val occ = (shiftOccurrence[shift] ?: 0) + 1
                    shiftOccurrence[shift] = occ
                    val displayText = if (count > 1) "$shift ${coworkerOccurrenceMark(occ)}" else shift
                    val isSelected = selectedShiftIndex == index
                    DropdownMenuItem(
                        text = { Text(displayText, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onShiftSelected(shift, index); expanded = false },
                        leadingIcon = {
                            if (isSelected) Icon(
                                Icons.Default.Work, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }

        if (selectedShift != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isToday) {
                        "오늘(${referenceDate.format(shortDateFormatter)}) '${selectedDisplayText}' 근무를 기준으로 순환이 계산됩니다"
                    } else {
                        "${referenceDate.format(shortDateFormatter)} '${selectedDisplayText}' 근무를 기준으로 순환이 계산됩니다"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─── 패턴 미리보기 ─────────────────────────────────────────────────────────────

//@Composable
//private fun CoworkerShiftPatternPreview(
//    pattern: List<String>,
//    selectedShift: String,
//    selectedShiftIndex: Int?,
//    referenceDate: LocalDate
//) {
//    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd") }
//    val selectedIndex = selectedShiftIndex ?: pattern.indexOf(selectedShift)
//    val patternSize = pattern.size
//    val today = LocalDate.now()
//    val daysFromRefToToday = ChronoUnit.DAYS.between(referenceDate, today).toInt()
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
//        )
//    ) {
//        Column(modifier = Modifier.padding(12.dp)) {
//            Text(
//                text = "교번 미리보기 (${pattern.size}일 주기)",
//                style = MaterialTheme.typography.labelMedium,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.onSecondaryContainer
//            )
//            Spacer(Modifier.height(8.dp))
//            Text(
//                text = "패턴: ${pattern.joinToString(" → ")}",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Spacer(Modifier.height(8.dp))
//            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
//            Spacer(Modifier.height(8.dp))
//            Text(text = "오늘 기준 7일간 근무:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
//            Spacer(Modifier.height(4.dp))
//            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
//                repeat(7) { dayOffset ->
//                    val date = today.plusDays(dayOffset.toLong())
//                    val totalOffset = daysFromRefToToday + dayOffset
//                    val shiftIndex = ((selectedIndex + totalOffset) % patternSize + patternSize) % patternSize
//                    val shift = pattern[shiftIndex]
//                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                        Text(
//                            text = date.format(dateFormatter),
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.outline,
//                            fontSize = 9.sp
//                        )
//                        Spacer(Modifier.height(2.dp))
//                        ShiftBadge(shiftName = shift, fontSize = 11f)
//                    }
//                }
//            }
//        }
//    }
//}
