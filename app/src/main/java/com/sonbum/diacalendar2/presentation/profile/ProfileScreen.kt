package com.sonbum.diacalendar2.presentation.profile

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sonbum.diacalendar2.domain.model.ChatNote
import com.sonbum.diacalendar2.domain.model.Memo
import com.sonbum.diacalendar2.domain.model.VacationRecord
import kotlinx.coroutines.launch
import java.io.File

import java.time.format.DateTimeFormatter as JFormatter
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(
	modifier: Modifier = Modifier,
	viewModel: ProfileViewModel = koinViewModel()
) {
	val state by viewModel.state.collectAsStateWithLifecycle()
	val pagerState = rememberPagerState(pageCount = { 3 })
	val coroutineScope = rememberCoroutineScope()
	var chatInputText by remember { mutableStateOf("") }
	var chatImageUri by remember { mutableStateOf<Uri?>(null) }
	val snackbarHostState = remember { SnackbarHostState() }

	LaunchedEffect(Unit) {
		viewModel.events.collect { event ->
			when (event) {
				is ProfileEvent.VipRefreshResult -> {
					val message = if (event.isVip) "VIP가 확인되었습니다." else "VIP 권한이 없습니다."
					snackbarHostState.showSnackbar(message)
				}
			}
		}
	}

	val chatImagePickerLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.GetContent()
	) { uri: Uri? ->
		chatImageUri = uri
	}

	val tabs = listOf("메모내역", "근태내역", "나와의 채팅")
	val scaffoldBottomPadding = com.sonbum.diacalendar2.LocalScaffoldPaddingValues.current.calculateBottomPadding()

	Scaffold(
		modifier = modifier,
		snackbarHost = { SnackbarHost(snackbarHostState) },
		containerColor = MaterialTheme.colorScheme.background
	) { innerPadding ->
	Column(
		modifier = Modifier
			.fillMaxSize()
			.statusBarsPadding()
			.padding(bottom = scaffoldBottomPadding)
			.padding(innerPadding)
	) {
		PrimaryTabRow(
			selectedTabIndex = pagerState.currentPage,
			indicator = {
				TabRowDefaults.PrimaryIndicator(
					modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
					width = 60.dp,
					shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
				)
			}
		) {
			tabs.forEachIndexed { index, title ->
				Tab(
					selected = pagerState.currentPage == index,
					onClick = {
						coroutineScope.launch {
							pagerState.animateScrollToPage(index)
						}
					},
					text = {
						Text(
							text = title,
							style = if (pagerState.currentPage == index)
								MaterialTheme.typography.titleSmall
							else
								MaterialTheme.typography.bodyMedium
						)
					}
				)
			}
		}

		HorizontalPager(
			state = pagerState,
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f),
			userScrollEnabled = true
		) { page ->
			when (page) {
				0 -> MemoHistoryTab(
					state = state,
					onDeleteMemo = viewModel::deleteMemo
				)
				1 -> VacationHistoryTab(
					state = state,
					onDeleteRecord = viewModel::deleteVacationRecord
				)
				2 -> ChatNoteTab(
					state = state,
					onDeleteNote = viewModel::deleteChatNote,
					onUpdateNote = viewModel::updateChatNote
				)
			}
		}

		// 채팅 입력창 - 노트 탭일 때만 표시
		androidx.compose.animation.AnimatedVisibility(
			visible = pagerState.currentPage == 2
		) {
			ChatInputBar(
				text = chatInputText,
				onTextChange = { chatInputText = it },
				imageUri = chatImageUri,
				onPickImage = { chatImagePickerLauncher.launch("image/*") },
				onRemoveImage = { chatImageUri = null },
				onSend = {
					viewModel.sendChatNote(chatInputText, chatImageUri)
					chatInputText = ""
					chatImageUri = null
				}
			)
		}

	}
	}
}

@Composable
private fun YearSelector(
	years: List<Int>,
	selectedYear: Int?,
	onYearSelected: (Int?) -> Unit
) {
	var expanded by remember { mutableStateOf(false) }

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		FilterChip(
			selected = selectedYear == null,
			onClick = { onYearSelected(null) },
			label = { Text("전체") },
			colors = FilterChipDefaults.filterChipColors(
				selectedContainerColor = MaterialTheme.colorScheme.primary,
				selectedLabelColor = MaterialTheme.colorScheme.onPrimary
			)
		)

		Box {
			OutlinedButton(
				onClick = { expanded = true },
				shape = RoundedCornerShape(8.dp),
				contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
			) {
				Text(
					text = selectedYear?.let { "${it}년" } ?: "년도 선택",
					style = MaterialTheme.typography.bodyMedium
				)
				Icon(
					imageVector = Icons.Default.ArrowDropDown,
					contentDescription = "년도 선택"
				)
			}
			DropdownMenu(
				expanded = expanded,
				onDismissRequest = { expanded = false }
			) {
				years.forEach { year ->
					DropdownMenuItem(
						text = { Text("${year}년") },
						onClick = {
							onYearSelected(year)
							expanded = false
						}
					)
				}
			}
		}
	}
}

@Composable
private fun MemoHistoryTab(
	state: ProfileState,
	onDeleteMemo: (Memo) -> Unit
) {
	var memoToDelete by remember { mutableStateOf<Memo?>(null) }
	var selectedYear by remember { mutableStateOf<Int?>(LocalDate.now().year) }
	var searchQuery by remember { mutableStateOf("") }

	// 삭제 확인 다이얼로그
	memoToDelete?.let { memo ->
		AlertDialog(
			onDismissRequest = { memoToDelete = null },
			title = { Text("메모 삭제") },
			text = {
				Text("\"${memo.title.ifEmpty { "(제목 없음)" }}\" 메모를 삭제하시겠습니까?")
			},
			confirmButton = {
				TextButton(
					onClick = {
						onDeleteMemo(memo)
						memoToDelete = null
					}
				) {
					Text("삭제", color = MaterialTheme.colorScheme.error)
				}
			},
			dismissButton = {
				TextButton(onClick = { memoToDelete = null }) {
					Text("취소")
				}
			}
		)
	}

	when {
		state.isLoading -> {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				CircularProgressIndicator()
			}
		}
		state.memosByDate.isEmpty() -> {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				Column(
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					Text(
						text = "저장된 메모가 없습니다",
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "날짜를 눌러 메모를 추가해보세요",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.outline
					)
				}
			}
		}
		else -> {
			val availableYears = remember(state.memosByDate) {
				state.memosByDate.keys.map { it.year }.distinct().sortedDescending()
			}

			val filteredMemos = remember(state.memosByDate, selectedYear, searchQuery) {
				val yearFiltered = if (selectedYear == null) {
					state.memosByDate
				} else {
					state.memosByDate.filterKeys { it.year == selectedYear }
				}
				val searchFiltered = if (searchQuery.isBlank()) {
					yearFiltered
				} else {
					yearFiltered.mapValues { (_, memos) ->
						memos.filter { memo ->
							memo.title.contains(searchQuery, ignoreCase = true) ||
								memo.content.contains(searchQuery, ignoreCase = true)
						}
					}.filterValues { it.isNotEmpty() }
				}
				searchFiltered.flatMap { (date, memos) ->
					listOf(MemoListItemType.Header(date, memos.size)) +
						memos.map { MemoListItemType.MemoItem(it) }
				}
			}

			Column(modifier = Modifier.fillMaxSize()) {
				YearSelector(
					years = availableYears,
					selectedYear = selectedYear,
					onYearSelected = { selectedYear = it }
				)

				// 검색바
				OutlinedTextField(
					value = searchQuery,
					onValueChange = { searchQuery = it },
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp, vertical = 4.dp),
					placeholder = {
						Text(
							"제목 또는 내용 검색",
							style = MaterialTheme.typography.bodyMedium
						)
					},
					leadingIcon = {
						Icon(
							imageVector = Icons.Default.Search,
							contentDescription = "검색",
							tint = MaterialTheme.colorScheme.onSurfaceVariant
						)
					},
					trailingIcon = {
						if (searchQuery.isNotEmpty()) {
							IconButton(onClick = { searchQuery = "" }) {
								Icon(
									imageVector = Icons.Default.Clear,
									contentDescription = "지우기",
									tint = MaterialTheme.colorScheme.onSurfaceVariant
								)
							}
						}
					},
					shape = RoundedCornerShape(12.dp),
					singleLine = true,
					textStyle = MaterialTheme.typography.bodyMedium,
					colors = OutlinedTextFieldDefaults.colors(
						focusedBorderColor = MaterialTheme.colorScheme.primary,
						unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
					)
				)

				if (filteredMemos.isEmpty()) {
					Box(
						modifier = Modifier.fillMaxSize(),
						contentAlignment = Alignment.Center
					) {
						Text(
							text = if (searchQuery.isNotBlank()) {
								"\"${searchQuery}\" 검색 결과가 없습니다"
							} else {
								"${selectedYear}년 메모가 없습니다"
							},
							style = MaterialTheme.typography.bodyLarge,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				} else {
					LazyColumn(
						modifier = Modifier
							.fillMaxSize()
							.padding(bottom = 10.dp),
						contentPadding = PaddingValues(
							start = 16.dp,
							end = 16.dp,
							top = 0.dp,
							bottom = 100.dp
						),
						verticalArrangement = Arrangement.spacedBy(4.dp)
					) {
						items(
							items = filteredMemos,
							key = { item ->
								when (item) {
									is MemoListItemType.Header -> "header_${item.date}"
									is MemoListItemType.MemoItem -> item.memo.objectId
								}
							}
						) { item ->
							when (item) {
								is MemoListItemType.Header -> {
									DateHeader(date = item.date, memoCount = item.count)
								}
								is MemoListItemType.MemoItem -> {
									MemoListItem(
										memo = item.memo,
										onDelete = { memoToDelete = item.memo }
									)
								}
							}
						}
					}
				}
			}
		}
	}
}

private sealed class MemoListItemType {
	data class Header(val date: LocalDate, val count: Int) : MemoListItemType()
	data class MemoItem(val memo: Memo) : MemoListItemType()
}

@Composable
private fun VacationHistoryTab(
	state: ProfileState,
	onDeleteRecord: (VacationRecord) -> Unit
) {
	var recordToDelete by remember { mutableStateOf<VacationRecord?>(null) }
	var selectedYear by remember { mutableStateOf<Int?>(LocalDate.now().year) }

	recordToDelete?.let { record ->
		AlertDialog(
			onDismissRequest = { recordToDelete = null },
			title = { Text("근태 삭제") },
			text = {
				val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
				Text("${record.date.format(dateFormatter)}의 \"${record.vacationName}\"을(를) 삭제하시겠습니까?")
			},
			confirmButton = {
				TextButton(
					onClick = {
						onDeleteRecord(record)
						recordToDelete = null
					}
				) {
					Text("삭제", color = MaterialTheme.colorScheme.error)
				}
			},
			dismissButton = {
				TextButton(onClick = { recordToDelete = null }) {
					Text("취소")
				}
			}
		)
	}

	when {
		state.isVacationLoading -> {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				CircularProgressIndicator()
			}
		}
		state.vacationsByType.isEmpty() -> {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				Column(
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					Text(
						text = "사용한 내역이 없습니다",
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "날짜를 눌러 근태를 등록해보세요",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.outline
					)
				}
			}
		}
		else -> {
			val availableYears = remember(state.vacationsByType) {
				state.vacationsByType.values.flatten().map { it.date.year }.distinct().sortedDescending()
			}

			val filteredVacationList = remember(state.vacationsByType, selectedYear) {
				val filtered = if (selectedYear == null) {
					state.vacationsByType
				} else {
					state.vacationsByType.mapValues { (_, records) ->
						records.filter { it.date.year == selectedYear }
					}.filterValues { it.isNotEmpty() }
				}
				filtered.flatMap { (typeName, records) ->
					val type = state.vacationTypesByName[typeName]
					listOf(
						VacationListItemType.Header(
							typeName = typeName,
							count = records.size,
							quota = type?.annualQuota ?: 0,
							resetMonthDay = type?.resetMonthDay ?: "01-01"
						)
					) + records.map { VacationListItemType.RecordItem(it) }
				}
			}

			Column(modifier = Modifier.fillMaxSize()) {
				YearSelector(
					years = availableYears,
					selectedYear = selectedYear,
					onYearSelected = { selectedYear = it }
				)

				if (filteredVacationList.isEmpty()) {
					Box(
						modifier = Modifier.fillMaxSize(),
						contentAlignment = Alignment.Center
					) {
						Text(
							text = "${selectedYear}년 내역이 없습니다",
							style = MaterialTheme.typography.bodyLarge,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				} else {
					LazyColumn(
						modifier = Modifier
							.fillMaxSize()
							.padding(bottom = 10.dp),
						contentPadding = PaddingValues(
							start = 16.dp,
							end = 16.dp,
							top = 0.dp,
							bottom = 100.dp
						),
						verticalArrangement = Arrangement.spacedBy(4.dp)
					) {
						items(
							items = filteredVacationList,
							key = { item ->
								when (item) {
									is VacationListItemType.Header -> "vheader_${selectedYear}_${item.typeName}"
									is VacationListItemType.RecordItem -> "vrecord_${item.record.id}"
								}
							}
						) { item ->
							when (item) {
								is VacationListItemType.Header -> {
									VacationTypeHeader(
										typeName = item.typeName,
										count = item.count,
										quota = item.quota,
										resetMonthDay = item.resetMonthDay
									)
								}
								is VacationListItemType.RecordItem -> {
									VacationRecordItem(
										record = item.record,
										onDelete = { recordToDelete = item.record }
									)
								}
							}
						}
					}
				}
			}
		}
	}
}

private sealed class VacationListItemType {
	data class Header(
		val typeName: String,
		val count: Int,
		val quota: Int,
		val resetMonthDay: String
	) : VacationListItemType()
	data class RecordItem(val record: VacationRecord) : VacationListItemType()
}

@Composable
private fun VacationTypeHeader(
	typeName: String,
	count: Int,
	quota: Int,
	resetMonthDay: String
) {
	Column {
		Spacer(modifier = Modifier.height(8.dp))
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 8.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Column {
				Text(
					text = typeName,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold,
					color = MaterialTheme.colorScheme.primary
				)
				if (quota > 0) {
					Text(
						text = "${resetMonthDay} 초기화",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.outline
					)
				}
			}
			Text(
				text = if (quota > 0) "${count}일 / ${quota}일" else "${count}일",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.outline
			)
		}
		HorizontalDivider(
			color = MaterialTheme.colorScheme.outlineVariant,
			thickness = 1.dp
		)
	}
}

@Composable
private fun VacationRecordItem(
	record: VacationRecord,
	onDelete: () -> Unit
) {
	val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)")
	val dayOfWeek = record.date.dayOfWeek.value // 1=Mon, 7=Sun

	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 2.dp),
		shape = RoundedCornerShape(8.dp),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface
		),
		elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// 휴가 약어 뱃지
			Box(
				modifier = Modifier
					.clip(RoundedCornerShape(4.dp))
					.background(MaterialTheme.colorScheme.errorContainer)
					.padding(horizontal = 6.dp, vertical = 2.dp),
				contentAlignment = Alignment.Center
			) {
				Text(
					text = record.shortName,
					style = MaterialTheme.typography.labelSmall,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onErrorContainer
				)
			}

			Spacer(modifier = Modifier.width(12.dp))

			// 날짜
			Text(
				text = record.date.format(dateFormatter),
				style = MaterialTheme.typography.bodyMedium,
				color = when (dayOfWeek) {
					7 -> MaterialTheme.colorScheme.error
					6 -> MaterialTheme.colorScheme.primary
					else -> MaterialTheme.colorScheme.onSurface
				},
				modifier = Modifier.weight(1f)
			)

			// 삭제 버튼
			IconButton(onClick = onDelete) {
				Icon(
					imageVector = Icons.Default.Delete,
					contentDescription = "삭제",
					tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
				)
			}
		}
	}
}

@Composable
private fun ChatNoteTab(
	state: ProfileState,
	onDeleteNote: (ChatNote) -> Unit,
	onUpdateNote: (ChatNote, String) -> Unit
) {
	var noteToDelete by remember { mutableStateOf<ChatNote?>(null) }
	var noteToEdit by remember { mutableStateOf<ChatNote?>(null) }
	var editText by remember { mutableStateOf("") }
	val listState = rememberLazyListState()
	val notes = state.chatNotes

	// 새 메시지가 추가되면 맨 아래로 스크롤
	androidx.compose.runtime.LaunchedEffect(notes.size) {
		if (notes.isNotEmpty()) {
			listState.animateScrollToItem(notes.size - 1)
		}
	}

	if (notes.isEmpty() && !state.isChatNotesLoading) {
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			Text(
				text = "나만의 메모를 채팅처럼 기록해보세요",
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	} else {
		LazyColumn(
			state = listState,
			modifier = Modifier
				.fillMaxSize(),
			contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
			verticalArrangement = Arrangement.spacedBy(4.dp)
		) {
			var lastDate: LocalDate? = null
			items(
				count = notes.size,
				key = { notes[it].id }
			) { index ->
				val note = notes[index]
				val noteDate = note.createdAt.toLocalDate()

				// 날짜 구분선
				if (lastDate != noteDate) {
					lastDate = noteDate
					ChatDateHeader(date = noteDate)
				}

				ChatBubble(
					note = note,
					onClick = {
						noteToEdit = note
						editText = note.content
					},
					onLongClick = { noteToDelete = note }
				)
			}
		}
	}

	// 삭제 확인 다이얼로그
	noteToDelete?.let { note ->
		AlertDialog(
			onDismissRequest = { noteToDelete = null },
			title = { Text("메모 삭제") },
			text = { Text("이 메모를 삭제하시겠습니까?") },
			confirmButton = {
				TextButton(onClick = {
					onDeleteNote(note)
					noteToDelete = null
				}) {
					Text("삭제", color = MaterialTheme.colorScheme.error)
				}
			},
			dismissButton = {
				TextButton(onClick = { noteToDelete = null }) {
					Text("취소")
				}
			}
		)
	}

	// 수정 다이얼로그
	noteToEdit?.let { note ->
		AlertDialog(
			onDismissRequest = { noteToEdit = null },
			title = { Text("메모 수정") },
			text = {
				OutlinedTextField(
					value = editText,
					onValueChange = { editText = it },
					modifier = Modifier.fillMaxWidth(),
					maxLines = 6,
					shape = RoundedCornerShape(12.dp),
					textStyle = MaterialTheme.typography.bodyMedium
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						onUpdateNote(note, editText)
						noteToEdit = null
					},
					enabled = editText.isNotBlank()
				) {
					Text("저장")
				}
			},
			dismissButton = {
				TextButton(onClick = { noteToEdit = null }) {
					Text("취소")
				}
			}
		)
	}
}

@Composable
private fun ChatDateHeader(date: LocalDate) {
	val formatter = JFormatter.ofPattern("yyyy년 M월 d일 (E)")
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 12.dp),
		contentAlignment = Alignment.Center
	) {
		Surface(
			shape = RoundedCornerShape(12.dp),
			color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
		) {
			Text(
				text = date.format(formatter),
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
			)
		}
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
	note: ChatNote,
	onClick: () -> Unit,
	onLongClick: () -> Unit
) {
	val timeFormatter = JFormatter.ofPattern("a h:mm")
	val context = LocalContext.current
	var showFullImage by remember { mutableStateOf(false) }

	// 전체화면 이미지 다이얼로그
	if (showFullImage && note.imagePath != null) {
		FullScreenImageDialog(
			imagePath = note.imagePath,
			onDismiss = { showFullImage = false }
		)
	}

	Column(
		modifier = Modifier.fillMaxWidth(),
		horizontalAlignment = Alignment.End
	) {
		Surface(
			shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
			color = MaterialTheme.colorScheme.primaryContainer,
			modifier = Modifier
				.widthIn(max = 280.dp)
				.combinedClickable(
					onClick = onClick,
					onLongClick = onLongClick
				)
		) {
			Column {
				// 이미지 표시
				if (note.imagePath != null) {
					AsyncImage(
						model = ImageRequest.Builder(context)
							.data(File(note.imagePath))
							.crossfade(true)
							.build(),
						contentDescription = "첨부 이미지",
						contentScale = ContentScale.Crop,
						modifier = Modifier
							.fillMaxWidth()
							.height(160.dp)
							.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp))
							.clickable { showFullImage = true }
					)
				}
				if (note.content.isNotBlank()) {
					Text(
						text = note.content,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onPrimaryContainer,
						modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
					)
				}
			}
		}

		Text(
			text = note.createdAt.format(timeFormatter),
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
			modifier = Modifier.padding(top = 2.dp, end = 4.dp)
		)
	}
}

@Composable
private fun ChatInputBar(
	text: String,
	onTextChange: (String) -> Unit,
	imageUri: Uri?,
	onPickImage: () -> Unit,
	onRemoveImage: () -> Unit,
	onSend: () -> Unit,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	val canSend = text.isNotBlank() || imageUri != null

	Surface(
		tonalElevation = 3.dp,
		modifier = modifier.fillMaxWidth()
	) {
		Column {
			// 이미지 미리보기
			if (imageUri != null) {
				Box(
					modifier = Modifier
						.padding(horizontal = 12.dp, vertical = 4.dp)
						.height(100.dp)
						.width(100.dp)
				) {
					AsyncImage(
						model = ImageRequest.Builder(context)
							.data(imageUri)
							.crossfade(true)
							.build(),
						contentDescription = "첨부 이미지",
						contentScale = ContentScale.Crop,
						modifier = Modifier
							.fillMaxSize()
							.clip(RoundedCornerShape(8.dp))
					)
					IconButton(
						onClick = onRemoveImage,
						modifier = Modifier
							.align(Alignment.TopEnd)
							.size(24.dp)
							.background(
								color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
								shape = CircleShape
							)
					) {
						Icon(
							imageVector = Icons.Default.Close,
							contentDescription = "이미지 삭제",
							modifier = Modifier.size(14.dp)
						)
					}
				}
			}

			Row(
				modifier = Modifier
					.padding(horizontal = 12.dp, vertical = 8.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				// 이미지 첨부 버튼
				IconButton(
					onClick = onPickImage,
					modifier = Modifier.size(40.dp)
				) {
					Icon(
						imageVector = Icons.Default.AddPhotoAlternate,
						contentDescription = "이미지 첨부",
						tint = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				OutlinedTextField(
					value = text,
					onValueChange = onTextChange,
					modifier = Modifier.weight(1f),
					placeholder = {
						Text(
							"내용을 입력하세요",
							style = MaterialTheme.typography.bodyMedium
						)
					},
					shape = RoundedCornerShape(24.dp),
					colors = OutlinedTextFieldDefaults.colors(
						focusedBorderColor = MaterialTheme.colorScheme.primary,
						unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
					),
					maxLines = 4,
					textStyle = MaterialTheme.typography.bodyMedium
				)
				Spacer(modifier = Modifier.width(8.dp))
				IconButton(
					onClick = onSend,
					enabled = canSend
				) {
					Icon(
						imageVector = Icons.AutoMirrored.Filled.Send,
						contentDescription = "전송",
						tint = if (canSend)
							MaterialTheme.colorScheme.primary
						else
							MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
					)
				}
			}
		}
	}
}

@Composable
private fun DateHeader(
	date: LocalDate,
	memoCount: Int
) {
	val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)")
	val dateText = date.format(dateFormatter)

	Column {
		Spacer(modifier = Modifier.height(8.dp))
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 8.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = dateText,
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
				color = MaterialTheme.colorScheme.primary
			)
			Text(
				text = "${memoCount}개",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.outline
			)
		}
		HorizontalDivider(
			color = MaterialTheme.colorScheme.outlineVariant,
			thickness = 1.dp
		)
	}
}

@Composable
private fun MemoListItem(
	memo: Memo,
	onDelete: () -> Unit
) {
	val memoColor = Color(memo.hexColorString.toColorInt())
	val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
	var showFullImage by remember { mutableStateOf(false) }

	// 전체화면 이미지 다이얼로그
	if (showFullImage && memo.imagePath != null) {
		FullScreenImageDialog(
			imagePath = memo.imagePath,
			onDismiss = { showFullImage = false }
		)
	}

	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 4.dp),
		shape = RoundedCornerShape(12.dp),
		colors = CardDefaults.cardColors(
			containerColor = if (memo.isCompleted) {
				MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
			} else {
				MaterialTheme.colorScheme.surface
			}
		),
		elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// 색상 인디케이터
			Box(
				modifier = Modifier
					.size(8.dp)
					.clip(CircleShape)
					.background(
						if (memo.isCompleted) {
							MaterialTheme.colorScheme.outline
						} else {
							memoColor
						}
					)
			)

			Spacer(modifier = Modifier.width(12.dp))

			// 메모 내용
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = memo.title.ifEmpty { "(제목 없음)" },
					style = MaterialTheme.typography.bodyLarge,
					fontWeight = FontWeight.Medium,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					textDecoration = if (memo.isCompleted) TextDecoration.LineThrough else null,
					color = if (memo.isCompleted) {
						MaterialTheme.colorScheme.outline
					} else {
						MaterialTheme.colorScheme.onSurface
					}
				)

				if (memo.content.isNotEmpty()) {
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = memo.content,
						style = MaterialTheme.typography.bodySmall,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
						color = if (memo.isCompleted) {
							MaterialTheme.colorScheme.outline
						} else {
							MaterialTheme.colorScheme.onSurfaceVariant
						}
					)
				}

				if (memo.imagePath != null) {
					Spacer(modifier = Modifier.height(6.dp))
					AsyncImage(
						model = ImageRequest.Builder(LocalContext.current)
							.data(File(memo.imagePath))
							.crossfade(true)
							.build(),
						contentDescription = "첨부 이미지",
						contentScale = ContentScale.Crop,
						modifier = Modifier
							.fillMaxWidth()
							.height(80.dp)
							.clip(RoundedCornerShape(6.dp))
							.clickable { showFullImage = true }
					)
				}
			}

			Spacer(modifier = Modifier.width(8.dp))

			// 시간 표시
			Column(
				horizontalAlignment = Alignment.End
			) {
				Text(
					text = memo.startTime.format(timeFormatter),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.outline
				)
				Text(
					text = memo.endTime.format(timeFormatter),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.outline
				)
			}

			// 삭제 버튼
			IconButton(
				onClick = onDelete
			) {
				Icon(
					imageVector = Icons.Default.Delete,
					contentDescription = "삭제",
					tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
				)
			}
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
						offset = if (scale > 1f) {
							Offset(
								x = offset.x + pan.x,
								y = offset.y + pan.y
							)
						} else {
							Offset.Zero
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
