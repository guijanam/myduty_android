package com.sonbum.diacalendar2.presentation.shift

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonbum.diacalendar2.domain.model.CustomShift
import com.sonbum.diacalendar2.domain.model.Office
import com.sonbum.diacalendar2.domain.model.UserShiftConfig
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShiftSelectionScreen(
    onBack: () -> Unit,
    onNavigateToLocalOfficeList: () -> Unit = {},
    onNavigateToCustomShiftList: () -> Unit = {},
    isSubShift: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: ShiftSelectionViewModel = koinViewModel()
) {
    // sub 근무 모드 설정 (저장소 분리). 화면별로 별도 ViewModel 인스턴스가 생성됨.
    LaunchedEffect(isSubShift) {
        viewModel.setSubShiftMode(isSubShift)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showReferenceDatePicker by remember { mutableStateOf(false) }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ShiftSelectionEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is ShiftSelectionEvent.SaveSuccess -> {
                    onBack()
                }
                is ShiftSelectionEvent.DeleteSuccess -> {
                    onBack()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isSubShift) "sub 근무 설정" else "내근무 설정",
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
                    if (state.officeSource == OfficeSource.SERVER) {
                        IconButton(
                            onClick = { viewModel.refreshOfficesFromServer() },
                            enabled = !state.isLoading
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "새로고침"
                                )
                            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 기존 설정이 있는 경우 경고 메시지
            if (state.hasExistingConfig && state.existingOfficeName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "현재 '${state.existingOfficeName}'적용 중입니다.\n다시 설정하면 기존 근무 데이터가 삭제되고 새로 생성됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::showDeleteConfirmDialog,
                    enabled = !state.isDeleting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("삭제 중...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("생성근무 지우기")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 안내 텍스트
            Text(
                text = "근무 일정을 설정하려면 아래 항목을 순서대로 선택하세요\n" +
		                "근무 일정이 필요없다면 설정없이 쓰시면 됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 승무소 소스 선택 (서버 / 내부 / 교대근무자)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = state.officeSource == OfficeSource.SERVER,
                    onClick = { viewModel.onOfficeSourceChange(OfficeSource.SERVER) },
                    label = { Text("교번근무(서버)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = state.officeSource == OfficeSource.CUSTOM_SHIFT,
                    onClick = { viewModel.onOfficeSourceChange(OfficeSource.CUSTOM_SHIFT) },
                    label = { Text("교대근무") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )

	            FilterChip(
		            selected = state.officeSource == OfficeSource.LOCAL,
		            onClick = { viewModel.onOfficeSourceChange(OfficeSource.LOCAL) },
		            label = { Text("교번근무(로컬)") },
		            colors = FilterChipDefaults.filterChipColors(
			            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
			            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
		            )
	            )
                if (state.officeSource == OfficeSource.LOCAL) {
                    TextButton(onClick = onNavigateToLocalOfficeList) {
                        Text("근무표편집(Dia)")
                    }
                }
                if (state.officeSource == OfficeSource.CUSTOM_SHIFT) {
                    TextButton(onClick = onNavigateToCustomShiftList) {
                        Text("교대근무편집(예:4조2교대-주,야,비,휴)")
                    }
                }

	            if (state.officeSource == OfficeSource.SERVER) {
		            // 2. UriHandler 인스턴스 가져오기
		            val uriHandler = LocalUriHandler.current
		            val targetUrl = "https://diacalendar.co.kr" // 여기에 이동하고 싶은 웹사이트 주소를 넣으세요.

		            Button(
			            onClick = { uriHandler.openUri(targetUrl) },
			            modifier = Modifier // 필요한 경우 여기에 패딩이나 가로 너비를 설정하세요.
		            ) {
			            // 아이콘과 텍스트를 가로로 배치
			            Row(verticalAlignment = Alignment.CenterVertically) {
				            Icon(
					            imageVector = Icons.AutoMirrored.Filled.OpenInNew, // 외부 연결을 의미하는 아이콘
					            contentDescription = "웹사이트 열기",
					            modifier = Modifier.size(15.dp)
				            )
				            Spacer(modifier = Modifier.width(4.dp)) // 아이콘과 글자 사이 간격
				            Text("Dia관리")
			            }
		            }
	            }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isCustomShift = state.officeSource == OfficeSource.CUSTOM_SHIFT

            if (isCustomShift) {
                // === 교대근무자 전용 흐름 ===

                // 1. 교대근무 선택
                SectionTitle(number = 1, title = "교대근무 선택")
                Spacer(modifier = Modifier.height(8.dp))
                CustomShiftSelector(
                    customShifts = state.customShifts,
                    selectedCustomShift = state.selectedCustomShift,
                    onCustomShiftSelected = viewModel::onCustomShiftSelected
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. 시작 날짜 선택
                SectionTitle(number = 2, title = "시작 날짜")
                Spacer(modifier = Modifier.height(8.dp))
                DateSelector(
                    selectedDate = state.startDate,
                    onDateClick = { showDatePicker = true },
                    enabled = state.selectedCustomShift != null
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "시작 날짜부터 3년간의 근무순서가 달력에 표시됩니다.\n선택하지 않으면 오늘부터 시작합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. 기준교번 선택
                SectionTitle(number = 3, title = "기준근무 선택")
                Spacer(modifier = Modifier.height(8.dp))
                if (state.selectedCustomShift != null && state.availableShifts.isNotEmpty()) {
                    BaseShiftSelector(
                        availableShifts = state.availableShifts,
                        selectedShift = state.selectedTodayShift,
                        selectedShiftIndex = state.selectedTodayShiftAvailableIndex ?: state.selectedTodayShiftIndex,
                        onShiftSelected = viewModel::onTodayShiftSelected,
                        referenceDate = state.referenceDate,
                        onReferenceDateClick = { showReferenceDatePicker = true }
                    )
//                    if (state.shiftPattern.isNotEmpty() && state.selectedTodayShift != null) {
//                        Spacer(modifier = Modifier.height(12.dp))
//                        ShiftPatternPreview(
//                            pattern = state.shiftPattern,
//                            selectedShift = state.selectedTodayShift!!,
//                            selectedShiftIndex = state.selectedTodayShiftIndex,
//                            referenceDate = state.referenceDate,
//                            startDate = state.startDate
//                        )
//                    }
                } else {
                    DisabledPlaceholder(text = "먼저 교대근무를 선택하세요")
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 4. 근무생성 버튼
                SectionTitle(number = 4, title = "내근무 생성")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = viewModel::saveSelectedOfficeData,
                    enabled = !state.isSaving &&
                            state.selectedCustomShift != null &&
                            state.selectedTodayShift != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("생성 중...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("내근무 생성하기")
                    }
                }
            } else {
                // === 기존 승무소 흐름 (서버/내부) ===

                // 1. 승무소 선택
                SectionTitle(number = 1, title = "승무소 선택")
                Spacer(modifier = Modifier.height(8.dp))
                OfficeSearchDropdown(
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    isExpanded = state.isDropdownExpanded,
                    onExpandedChange = viewModel::onDropdownExpandedChange,
                    offices = state.filteredOffices,
                    selectedOffice = state.selectedOffice,
                    onOfficeSelected = viewModel::onOfficeSelected,
                    onClear = viewModel::clearSelection,
                    isLoading = state.isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. 포지션 선택
                SectionTitle(number = 2, title = "포지션 선택")
                Spacer(modifier = Modifier.height(8.dp))
                if (state.selectedOffice != null) {
                    PositionSelector(
                        selectedPosition = state.selectedPosition,
                        onPositionSelected = viewModel::onPositionSelected,
                        office = state.selectedOffice!!
                    )
                } else {
                    DisabledPlaceholder(text = "먼저 승무소를 선택하세요")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. 시작 날짜 선택
                SectionTitle(number = 3, title = "시작 날짜")
                Spacer(modifier = Modifier.height(8.dp))
                DateSelector(
                    selectedDate = state.startDate,
                    onDateClick = { showDatePicker = true },
                    enabled = state.selectedOffice != null
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "시작 날짜부터 3년간의 교번 순서가 달력에 표시됩니다.\n선택하지 않으면 오늘부터 시작합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. 기준교번 선택
                SectionTitle(number = 4, title = "기준교번 선택")
                Spacer(modifier = Modifier.height(8.dp))
                if (state.selectedPosition != null && state.availableShifts.isNotEmpty()) {
                    BaseShiftSelector(
                        availableShifts = state.availableShifts,
                        selectedShift = state.selectedTodayShift,
                        selectedShiftIndex = state.selectedTodayShiftAvailableIndex ?: state.selectedTodayShiftIndex,
                        onShiftSelected = viewModel::onTodayShiftSelected,
                        referenceDate = state.referenceDate,
                        onReferenceDateClick = { showReferenceDatePicker = true }
                    )
//                    if (state.shiftPattern.isNotEmpty() && state.selectedTodayShift != null) {
//                        Spacer(modifier = Modifier.height(12.dp))
//                        ShiftPatternPreview(
//                            pattern = state.shiftPattern,
//                            selectedShift = state.selectedTodayShift!!,
//                            selectedShiftIndex = state.selectedTodayShiftIndex,
//                            referenceDate = state.referenceDate,
//                            startDate = state.startDate
//                        )
//                    }
                } else {
                    DisabledPlaceholder(text = "먼저 포지션을 선택하세요")
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 5. 근무생성 버튼
                SectionTitle(number = 5, title = "내근무 생성")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = viewModel::saveSelectedOfficeData,
                    enabled = !state.isSaving &&
                            state.selectedOffice != null &&
                            state.selectedPosition != null &&
                            state.selectedTodayShift != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("생성 중...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("내근무 생성하기")
                    }
                }
            }

            // 에러 메시지
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // 빈 상태 (승무소 목록이 없을 때) - 교대근무자 모드에서는 표시하지 않음
            if (!isCustomShift && !state.isLoading && state.filteredOffices.isEmpty() && state.searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                if (state.officeSource == OfficeSource.SERVER) {
                    EmptyOfficeContent(
                        onRefresh = viewModel::refreshOfficesFromServer
                    )
                } else {
                    EmptyLocalOfficeContent(
                        onManageOffices = onNavigateToLocalOfficeList
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // DatePicker 다이얼로그
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.startDate
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            viewModel.onStartDateSelected(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소")
                }
            }
        ) {
	        // 이 부분에 animateContentSize를 추가하는 것이 핵심입니다.

		        DatePicker(state = datePickerState,showModeToggle = false)

        }
    }

    // 기준교번 DatePicker 다이얼로그
    if (showReferenceDatePicker) {
        val refDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.referenceDate
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showReferenceDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        refDatePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            viewModel.onReferenceDateSelected(selectedDate)
                        }
                        showReferenceDatePicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReferenceDatePicker = false }) {
                    Text("취소")
                }
            }
        ) {
	        // 이 부분에 animateContentSize를 추가하는 것이 핵심입니다.

            DatePicker(state = refDatePickerState,showModeToggle = false)
        }
    }

    // 삭제 확인 다이얼로그
    if (state.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirmDialog,
            title = { Text("생성근무 지우기") },
            text = { Text("생성된 근무 스케줄이 모두 삭제됩니다.\n삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteAllShiftData,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirmDialog) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun CustomShiftSelector(
    customShifts: List<CustomShift>,
    selectedCustomShift: CustomShift?,
    onCustomShiftSelected: (CustomShift) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
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
                            "등록된 교대근무가 없습니다.\n교대근무편집에서 추가하세요.",
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
                                Text(
                                    text = shift.shiftName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${shift.shiftPattern.joinToString(",")} (${shift.shiftPattern.size}일 주기)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onCustomShiftSelected(shift)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    number: Int,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DisabledPlaceholder(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun OfficeSearchDropdown(
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
        // 검색 필드
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("승무소 이름으로 검색") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "검색"
                )
            },
            trailingIcon = {
                Row {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "지우기"
                            )
                        }
                    }
                    IconButton(onClick = { onExpandedChange(!isExpanded) }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = if (isExpanded) "접기" else "펼치기"
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

        // 드롭다운 목록
        if (isExpanded && !isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
	                .heightIn(max = 450.dp)
	                .padding(top = 4.dp), // 검색창과 약간의 간격을 둡니다.
	            shape = RoundedCornerShape(8.dp), // 하단만 둥글게 하기보다 전체를 둥글게 하는 것이 깔끔할 수 있습니다.
	            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                if (offices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "검색 결과가 없습니다" else "최신승무소목록은 우측상단 버튼",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
	                // offices 리스트를 이름 순으로 정렬합니다.
	                val sortedOffices = offices.sortedBy { it.officeName }
                    LazyColumn(
	                    modifier = Modifier.fillMaxWidth()
					) {
                        items(sortedOffices, key = { it.officeCode }) { office ->
                            OfficeDropdownItem(
                                office = office,
                                isSelected = selectedOffice?.officeCode == office.officeCode,
                                onClick = {
									onOfficeSelected(office)
	                                onExpandedChange(false) // 선택 시 자동으로 닫히도록 추가
								}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficeDropdownItem(
    office: Office,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Subway,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = office.officeName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
//            Text(
//                text = if (office.officeCode < 0) "내부 승무소" else "코드: ${office.officeCode}",
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.outline
//            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PositionSelector(
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
                    {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun DateSelector(
    selectedDate: LocalDate,
    onDateClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }
    val isToday = selectedDate == LocalDate.now()

    OutlinedCard(
        onClick = onDateClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isToday) "시작 날짜 (기본값:오늘)" else "시작 날짜",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = selectedDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// 1~20: ①②③…⑳ (U+2460~U+2473), 21 이상은 (N) 형태로 fallback
private fun occurrenceMark(occurrence: Int): String {
    if (occurrence <= 0) return ""
    return if (occurrence in 1..20) (0x2460 + (occurrence - 1)).toChar().toString()
    else "($occurrence)"
}

@Composable
private fun BaseShiftSelector(
    availableShifts: List<String>,
    selectedShift: String?,
    selectedShiftIndex: Int? = null,
    onShiftSelected: (String, Int) -> Unit,
    referenceDate: LocalDate,
    onReferenceDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("MM월 dd일") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }
    var expanded by remember { mutableStateOf(false) }
    val isToday = referenceDate == LocalDate.now()

    // 선택된 근무의 표시 텍스트 (중복이면 번호 포함)
    val selectedDisplayText = remember(selectedShift, selectedShiftIndex, availableShifts) {
        if (selectedShift == null || selectedShiftIndex == null) {
            selectedShift
        } else {
            val count = availableShifts.count { it == selectedShift }
            if (count > 1) {
                val occurrence = availableShifts.take(selectedShiftIndex + 1).count { it == selectedShift }
                "$selectedShift ${occurrenceMark(occurrence)}"
            } else {
                selectedShift
            }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "기준 날짜에 해당하는 근무를 선택하세요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 기준 날짜 선택
        OutlinedCard(
            onClick = onReferenceDateClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
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
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 기준교번 드롭다운 메뉴
        Box {
            OutlinedCard(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = if (selectedShift != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = selectedDisplayText ?: "기준교번 선택",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedShift != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedShift != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
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
                // 중복 근무명에 번호 부여를 위한 카운터
                val shiftCounts = availableShifts.groupingBy { it }.eachCount()
                val shiftOccurrence = mutableMapOf<String, Int>()

                availableShifts.forEachIndexed { index, shift ->
                    val count = shiftCounts[shift] ?: 1
                    val occurrence = (shiftOccurrence[shift] ?: 0) + 1
                    shiftOccurrence[shift] = occurrence

                    val displayText = if (count > 1) {
                        "$shift ${occurrenceMark(occurrence)}"
                    } else {
                        shift
                    }
                    val isSelected = selectedShiftIndex == index

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = displayText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onShiftSelected(shift, index)
                            expanded = false
                        },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        }

        // 선택 확인 메시지
        if (selectedShift != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isToday) {
                        "오늘(${referenceDate.format(dateFormatter)}) '${selectedDisplayText}' 근무를 기준으로 순환 근무가 생성됩니다"
                    } else {
                        "${referenceDate.format(dateFormatter)} '${selectedDisplayText}' 근무를 기준으로 순환 근무가 생성됩니다"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

//@Composable
//private fun ShiftPatternPreview(
//    pattern: List<String>,
//    selectedShift: String,
//    selectedShiftIndex: Int? = null,
//    referenceDate: LocalDate,
//    startDate: LocalDate,
//    modifier: Modifier = Modifier
//) {
//    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd") }
//    val selectedIndex = selectedShiftIndex ?: pattern.indexOf(selectedShift)
//    val patternSize = pattern.size
//
//    // referenceDate에 selectedShift가 오도록, startDate부터의 오프셋 계산
//    val daysFromRefToStart = java.time.temporal.ChronoUnit.DAYS.between(referenceDate, startDate).toInt()
//
//    Card(
//        modifier = modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(12.dp)
//        ) {
//            Text(
//                text = "교번 미리보기 (${pattern.size}일 주기)",
//                style = MaterialTheme.typography.labelMedium,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.onSecondaryContainer
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // 선택한 근무 기준으로 앞뒤 표시
//            Text(
//                text = "패턴: ${pattern.joinToString(" → ")}",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // 시작일 기준 7일간 미리보기
//            Text(
//                text = "시작일 기준 7일간 근무:",
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.outline
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                repeat(7) { dayOffset ->
//                    val date = startDate.plusDays(dayOffset.toLong())
//                    val totalOffset = daysFromRefToStart + dayOffset
//                    val shiftIndex = ((selectedIndex + totalOffset) % patternSize + patternSize) % patternSize
//                    val shift = pattern[shiftIndex]
//
//                    Column(
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Text(
//                            text = date.format(dateFormatter),
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.outline
//                        )
//                        Spacer(modifier = Modifier.height(2.dp))
//                        Box(
//                            modifier = Modifier
//                                .clip(RoundedCornerShape(4.dp))
//                                .background(
//                                    when {
//                                        shift.contains("비") || shift == "휴" ->
//                                            MaterialTheme.colorScheme.tertiaryContainer
//                                        shift.contains("주") ->
//                                            MaterialTheme.colorScheme.secondaryContainer
//                                        else ->
//                                            MaterialTheme.colorScheme.primaryContainer
//                                    }
//                                )
//                                .padding(horizontal = 6.dp, vertical = 2.dp),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = shift,
//                                style = MaterialTheme.typography.labelMedium,
//                                fontWeight = FontWeight.Bold,
//                                color = when {
//                                    shift.contains("비") || shift == "휴" ->
//                                        MaterialTheme.colorScheme.onTertiaryContainer
//                                    shift.contains("주") ->
//                                        MaterialTheme.colorScheme.onSecondaryContainer
//                                    else ->
//                                        MaterialTheme.colorScheme.onPrimaryContainer
//                                }
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

@Composable
private fun EmptyOfficeContent(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Work,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "승무소 목록이 없습니다",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "서버에서 승무소 목록을 불러와주세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("승무소 목록 불러오기")
        }
    }
}

@Composable
private fun EmptyLocalOfficeContent(
    onManageOffices: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Work,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "내부에 저장된 승무소가 없습니다",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "승무소 관리에서 직접 추가하세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onManageOffices) {
            Icon(
                imageVector = Icons.Default.Work,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("승무소 관리")
        }
    }
}
