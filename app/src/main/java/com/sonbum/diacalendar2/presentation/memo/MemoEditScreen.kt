package com.sonbum.diacalendar2.presentation.memo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.graphics.toColorInt

private val colorOptions = listOf(

	"#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#795548",

	"#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4"

)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoEditScreen(
	dateString: String,
	memoId: String?,
	onBack: () -> Unit,
	viewModel: MemoEditViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isNewMemo = memoId == null
    var showDeleteDialog by remember { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }

    // 이미지 선택 launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onAction(MemoEditAction.OnImageSelected(it)) }
    }

    LaunchedEffect(Unit) {
        viewModel.initialize(dateString, memoId)
    }

    // 화면이 열리면 제목 필드에 포커스
    LaunchedEffect(Unit) {
        titleFocusRequester.requestFocus()
    }

    LaunchedEffect(viewModel.event) {
        viewModel.event.collect { event ->
            when (event) {
	            MemoEditEvent.SaveSuccess,
	            MemoEditEvent.DeleteSuccess,
		            -> onBack()
                is MemoEditEvent.Error -> {
                    // TODO: Show snackbar
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        DeleteConfirmDialog(
            title = state.title.ifBlank { "이 메모" },
            onConfirm = {
                showDeleteDialog = false
                viewModel.onAction(MemoEditAction.OnDelete)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isNewMemo) "메모 추가" else "메모 편집")
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
                    if (!isNewMemo) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "삭제"
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.onAction(MemoEditAction.OnSave) }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "저장"
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
	    // --- 하단 버튼 바 추가 ---
	    bottomBar = {
		    Surface(
			    modifier = Modifier.fillMaxWidth(),
			    tonalElevation = 3.dp, // 살짝 입체감 부여
			    shadowElevation = 8.dp
		    ) {
			    Row(
				    modifier = Modifier
					    .navigationBarsPadding() // 시스템 네비게이션 바 영역 확보
					    .padding(horizontal = 16.dp, vertical = 12.dp)
					    .fillMaxWidth(),
				    horizontalArrangement = Arrangement.spacedBy(12.dp)
			    ) {
				    // 닫기 버튼
				    OutlinedButton(
					    onClick = onBack,
					    modifier = Modifier
						    .weight(1f)
						    .height(52.dp),
					    shape = RoundedCornerShape(12.dp)
				    ) {
					    Text("닫기", fontWeight = FontWeight.SemiBold)
				    }

				    // 저장 버튼
				    Button(
					    onClick = { viewModel.onAction(MemoEditAction.OnSave) },
					    modifier = Modifier
						    .weight(1f)
						    .height(52.dp),
					    shape = RoundedCornerShape(12.dp),
					    colors = ButtonDefaults.buttonColors(
						    containerColor = MaterialTheme.colorScheme.primary
					    )
				    ) {
					    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
					    Spacer(Modifier.width(8.dp))
					    Text("저장하기", fontWeight = FontWeight.Bold)
				    }
			    }
		    }
	    }

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 제목
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onAction(MemoEditAction.OnTitleChange(it)) },
                label = { Text("제목") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester),
                singleLine = true
            )

            // 내용
            OutlinedTextField(
                value = state.content,
                onValueChange = { viewModel.onAction(MemoEditAction.OnContentChange(it)) },
                label = { Text("내용") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // 날짜 선택 섹션
            DateSelectionSection(
                startDate = state.startDate,
                endDate = state.endDate,
                isMultipleDays = state.isMultipleDays,
                dayCount = state.dayCount,
                isNewMemo = isNewMemo,
                onMultipleDaysToggle = { viewModel.onAction(MemoEditAction.OnMultipleDaysToggle(it)) },
                onEndDateChange = { viewModel.onAction(MemoEditAction.OnEndDateChange(it)) }
            )

            // 종일 스위치
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "종일",
                    style = MaterialTheme.typography.labelLarge
                )
                Switch(
                    checked = state.isAllDay,
                    onCheckedChange = { viewModel.onAction(MemoEditAction.OnAllDayToggle(it)) }
                )
            }

            // 시간 선택 (종일이 아닐 때만)
            AnimatedVisibility(visible = !state.isAllDay) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "시간",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TimeSelector(
                            label = "시작",
                            time = state.startTime,
                            onTimeChange = { viewModel.onAction(MemoEditAction.OnStartTimeChange(it)) },
                            modifier = Modifier.weight(1f)
                        )
                        TimeSelector(
                            label = "종료",
                            time = state.endTime,
                            onTimeChange = { viewModel.onAction(MemoEditAction.OnEndTimeChange(it)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 알림 설정
            ReminderSection(
                reminderEnabled = state.reminderEnabled,
                reminderType = state.reminderType,
                customReminderDateTime = state.customReminderDateTime,
                startDate = state.startDate,
                startTime = state.startTime,
                onReminderToggle = { viewModel.onAction(MemoEditAction.OnReminderToggle(it)) },
                onReminderTypeChange = { viewModel.onAction(MemoEditAction.OnReminderTypeChange(it)) },
                onCustomDateTimeChange = { viewModel.onAction(MemoEditAction.OnCustomReminderDateTimeChange(it)) }
            )

            // 이미지 첨부 섹션
            ImageAttachmentSection(
                imagePath = state.imagePath,
                selectedImageUri = state.selectedImageUri,
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onRemoveImage = { viewModel.onAction(MemoEditAction.OnImageRemove) }
            )

            // 색상 선택
            Text(
                text = "색상",
                style = MaterialTheme.typography.labelLarge,
	            modifier = Modifier.padding(top = 8.dp)
            )
	        // 8열(Fixed(8))로 설정하면 16개일 때 자동으로 2줄이 됩니다.
	        LazyVerticalGrid(
		        columns = GridCells.Fixed(8),
		        modifier = Modifier
			        .fillMaxWidth()
			        .height(110.dp), // 2줄이 들어갈 적절한 높이
		        horizontalArrangement = Arrangement.spacedBy(8.dp),
		        verticalArrangement = Arrangement.spacedBy(12.dp),
		        userScrollEnabled = false // 2줄이 딱 맞게 들어가므로 스크롤은 끕니다.
	        ) {
		        items(colorOptions) { colorHex ->
			        ColorOption(
				        colorHex = colorHex,
				        isSelected = state.hexColorString == colorHex,
				        onClick = { viewModel.onAction(MemoEditAction.OnColorChange(colorHex)) }
			        )
		        }
	        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectionSection(
	startDate: LocalDate,
	endDate: LocalDate,
	isMultipleDays: Boolean,
	dayCount: Int,
	isNewMemo: Boolean,
	onMultipleDaysToggle: (Boolean) -> Unit,
	onEndDateChange: (LocalDate) -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)") }
    var showDatePicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 시작일 표시
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "시작일",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = startDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // 연속 일정 토글 (새 메모일 때만)
            if (isNewMemo) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "연속 일정",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = isMultipleDays,
                        onCheckedChange = onMultipleDaysToggle
                    )
                }

                // 종료일 선택 (연속 일정일 때)
                AnimatedVisibility(
                    visible = isMultipleDays,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { showDatePicker = true }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "종료일",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = endDate.format(dateFormatter),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Text(
                                text = "변경",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 선택된 일수 표시
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "총 ${dayCount}일간의 일정이 생성됩니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 날짜 선택 다이얼로그
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli()
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
                            onEndDateChange(selectedDate)
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
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
}

@Composable
private fun ColorOption(
	colorHex: String,
	isSelected: Boolean,
	onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(colorHex.toColorInt()))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TimeSelector(
	label: String,
	time: LocalTime,
	onTimeChange: (LocalTime) -> Unit,
	modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small
                )
                .clickable { showDialog = true }
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = time.format(formatter),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    if (showDialog) {
        TimePickerDialog(
            initialTime = time,
            onTimeSelected = {
                onTimeChange(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun TimePickerDialog(
	initialTime: LocalTime,
	onTimeSelected: (LocalTime) -> Unit,
	onDismiss: () -> Unit,
) {
	var hourInput by remember { mutableStateOf("%02d".format(initialTime.hour)) }
	var minuteInput by remember { mutableStateOf("%02d".format(initialTime.minute)) }

	// 포커스 제어를 위한 객체
	val hourFocusRequester = remember { FocusRequester() }
	val minuteFocusRequester = remember { FocusRequester() }

	androidx.compose.material3.AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("시간 입력", style = MaterialTheme.typography.titleMedium) },
		text = {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.Center,
				verticalAlignment = Alignment.CenterVertically
			) {
				// [시] 입력 필드
				EnhancedTimeField(
					value = hourInput,
					onValueChange = { newValue ->
						if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
							val h = newValue.toIntOrNull() ?: 0
							if (h in 0..23) {
								hourInput = newValue
								// 두 자리가 채워지면 자동으로 '분'으로 이동
								if (newValue.length == 2) minuteFocusRequester.requestFocus()
							}
						}
					},
					focusRequester = hourFocusRequester,
					onFocusLost = {
						// 포커스를 잃을 때 1자리라면 0을 붙여줌 (예: "5" -> "05")
						if (hourInput.isNotEmpty()) {
							hourInput = hourInput.padStart(2, '0')
						}
					}
				)

				Text(
					text = ":",
					style = MaterialTheme.typography.headlineMedium,
					modifier = Modifier.padding(horizontal = 12.dp)
				)

				// [분] 입력 필드
				EnhancedTimeField(
					value = minuteInput,
					onValueChange = { newValue ->
						if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
							val m = newValue.toIntOrNull() ?: 0
							if (m in 0..59) {
								minuteInput = newValue
							}
						}
					},
					focusRequester = minuteFocusRequester,
					onFocusLost = {
						if (minuteInput.isNotEmpty()) {
							minuteInput = minuteInput.padStart(2, '0')
						}
					}
				)
			}
		},
		confirmButton = {
			TextButton(
				onClick = {
					val h = hourInput.toIntOrNull() ?: 0
					val m = minuteInput.toIntOrNull() ?: 0
					onTimeSelected(LocalTime.of(h, m))
				}
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

@Composable
fun EnhancedTimeField(
	value: String,
	onValueChange: (String) -> Unit,
	focusRequester: FocusRequester,
	onFocusLost: () -> Unit
) {
	OutlinedTextField(
		value = value,
		onValueChange = onValueChange,
		modifier = Modifier
			.width(80.dp)
			.focusRequester(focusRequester) // 포커스 요청자 연결
			.onFocusChanged { focusState ->
				if (!focusState.isFocused) {
					onFocusLost() // 포커스를 잃었을 때 포맷팅 실행
				}
			},
		textStyle = MaterialTheme.typography.headlineSmall.copy(
			textAlign = TextAlign.Center,
			fontWeight = FontWeight.Bold
		),
		keyboardOptions = KeyboardOptions(
			keyboardType = KeyboardType.Number,
			imeAction = ImeAction.Done
		),
		singleLine = true,
		shape = RoundedCornerShape(12.dp)
	)
}

//@Composable
//private fun TimePickerDialog(
//	initialTime: LocalTime,
//	onTimeSelected: (LocalTime) -> Unit,
//	onDismiss: () -> Unit,
//) {
//    var hour by remember { mutableIntStateOf(initialTime.hour) }
//    var minute by remember { mutableIntStateOf(initialTime.minute) }
//
//    androidx.compose.material3.AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("시간 선택") },
//        text = {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.Center,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    IconButton(onClick = { hour = (hour + 1) % 24 }) {
//                        Text("▲")
//                    }
//                    Text(
//                        text = "%02d".format(hour),
//                        style = MaterialTheme.typography.headlineMedium
//                    )
//                    IconButton(onClick = { hour = if (hour == 0) 23 else hour - 1 }) {
//                        Text("▼")
//                    }
//                }
//
//                Text(
//                    text = ":",
//                    style = MaterialTheme.typography.headlineMedium,
//                    modifier = Modifier.padding(horizontal = 8.dp)
//                )
//
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    IconButton(onClick = { minute = (minute + 5) % 60 }) {
//                        Text("▲")
//                    }
//                    Text(
//                        text = "%02d".format(minute),
//                        style = MaterialTheme.typography.headlineMedium
//                    )
//                    IconButton(onClick = { minute = if (minute < 5) 55 else minute - 5 }) {
//                        Text("▼")
//                    }
//                }
//            }
//        },
//        confirmButton = {
//            TextButton(
//                onClick = { onTimeSelected(LocalTime.of(hour, minute)) }
//            ) {
//                Text("확인")
//            }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text("취소")
//            }
//        }
//    )
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSection(
	reminderEnabled: Boolean,
	reminderType: ReminderType,
	customReminderDateTime: LocalDateTime?,
	startDate: LocalDate,
	startTime: LocalTime,
	onReminderToggle: (Boolean) -> Unit,
	onReminderTypeChange: (ReminderType) -> Unit,
	onCustomDateTimeChange: (LocalDateTime) -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M월 d일 HH:mm") }
    val context = LocalContext.current

    fun handleReminderToggle(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                // 앱 시작 시 이미 권한 요청을 했으므로, 여기서는 설정 화면으로 안내
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                return
            }
        }
        onReminderToggle(enabled)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 알림 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (reminderEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "알림",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { handleReminderToggle(it) }
                )
            }

            // 알림 옵션
            AnimatedVisibility(
                visible = reminderEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ReminderType.entries.forEach { type ->
                            FilterChip(
                                selected = reminderType == type,
                                onClick = { onReminderTypeChange(type) },
                                label = { Text(type.label, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    // 직접 설정 시 날짜/시간 선택
                    if (reminderType == ReminderType.CUSTOM) {
                        CustomReminderPicker(
                            currentDateTime = customReminderDateTime
                                ?: startDate.atTime(startTime).minusMinutes(30),
                            onDateTimeChange = onCustomDateTimeChange
                        )
                    }

                    // 알림 시간 미리보기
                    val previewText = when (reminderType) {
                        ReminderType.CUSTOM -> {
                            val dt = customReminderDateTime
                                ?: startDate.atTime(startTime).minusMinutes(30)
                            "알림: ${dt.format(dateFormatter)}"
                        }
                        else -> {
                            val dt = startDate.atTime(startTime)
                                .minusMinutes(reminderType.minutesBefore)
                            "알림: ${dt.format(dateFormatter)}"
                        }
                    }
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomReminderPicker(
	currentDateTime: LocalDateTime,
	onDateTimeChange: (LocalDateTime) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 M월 d일") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 날짜 선택 버튼
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.weight(1f)
        ) {
            Text(currentDateTime.toLocalDate().format(dateFormatter))
        }
        // 시간 선택 버튼
        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.weight(1f)
        ) {
            Text(currentDateTime.toLocalTime().format(timeFormatter))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDateTime.toLocalDate()
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC")).toLocalDate()
                            onDateTimeChange(
                                LocalDateTime.of(selectedDate, currentDateTime.toLocalTime())
                            )
                        }
                        showDatePicker = false
                    }
                ) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialTime = currentDateTime.toLocalTime(),
            onTimeSelected = { time ->
                onDateTimeChange(
                    LocalDateTime.of(currentDateTime.toLocalDate(), time)
                )
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun ImageAttachmentSection(
	imagePath: String?,
	selectedImageUri: Uri?,
	onPickImage: () -> Unit,
	onRemoveImage: () -> Unit,
) {
    val context = LocalContext.current
    // 표시할 이미지 모델: 새로 선택한 URI 우선, 없으면 기존 저장된 파일 경로
    val imageModel: Any? = selectedImageUri ?: imagePath?.let { File(it) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "이미지",
            style = MaterialTheme.typography.labelLarge
        )

        if (imageModel != null) {
            // 이미지 미리보기
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = "첨부 이미지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // 삭제 버튼
                IconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "이미지 삭제",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // 이미지 추가 버튼
            OutlinedButton(
                onClick = onPickImage,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("이미지 추가")
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
	title: String,
	onConfirm: () -> Unit,
	onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("메모 삭제") },
        text = { Text("\"$title\"을(를) 삭제하시겠습니까?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("삭제", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
