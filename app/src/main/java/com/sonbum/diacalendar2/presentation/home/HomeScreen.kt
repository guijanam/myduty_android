package com.sonbum.diacalendar2.presentation.home

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.IconButton
import com.sonbum.diacalendar2.core.util.DeviceIdProvider
import com.sonbum.diacalendar2.domain.repository.SubscriptionRepository
import org.koin.compose.koinInject
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.ContentHeightMode
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import com.kizitonwose.calendar.core.yearMonth
import com.sonbum.diacalendar2.R
import com.sonbum.diacalendar2.SimpleCalendarTitle
import com.sonbum.diacalendar2.StatusBarColorUpdateEffect
import com.sonbum.diacalendar2.applyScaffoldBottomPadding
import com.sonbum.diacalendar2.applyScaffoldHorizontalPaddings
import com.sonbum.diacalendar2.applyScaffoldTopPadding
import com.sonbum.diacalendar2.clickable
import com.sonbum.diacalendar2.domain.model.CalendarEvent
import com.sonbum.diacalendar2.domain.model.Memo
import com.sonbum.diacalendar2.rememberFirstVisibleMonthAfterScroll
import com.sonbum.diacalendar2.shared.displayText
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.core.graphics.toColorInt
import com.sonbum.diacalendar2.rememberFirstCompletelyVisibleMonth
import com.sonbum.diacalendar2.data.local.datastore.CalendarTextSizes
import com.sonbum.diacalendar2.data.local.datastore.ThemeMode
import com.sonbum.diacalendar2.presentation.shared.ShiftBadge
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.HolidayVillage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.temporal.ChronoUnit

@SuppressLint("UseKtx")
@Composable
fun HomeScreen(
	modifier: Modifier = Modifier,
	memosByDate: Map<LocalDate, List<Memo>> = emptyMap(),
	eventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
	holidayMap: Map<LocalDate, String> = emptyMap(),
	anniversaryMap: Map<LocalDate, String> = emptyMap(),
	shiftScheduleMap: Map<LocalDate, String> = emptyMap(),
	swapDates: Set<LocalDate> = emptySet(),
	shiftInputMap: Map<LocalDate, ShiftInputInfo> = emptyMap(), // date -> ShiftInputInfo
	holidayWorkShifts: List<String> = emptyList(),
	vacationMap: Map<LocalDate, String> = emptyMap(),
	isRefreshingHolidays: Boolean = false,
	shiftPattern: List<String> = emptyList(),
	isCustomShift: Boolean = false,
	officeName: String? = null,
	onAction: (HomeAction) -> Unit,
	onVisibleYearChanged: (Int) -> Unit = {},
	onNavigateToCalendarSelection: () -> Unit = {},
	onNavigateToAnniversary: () -> Unit = {},
	onNavigateToShiftSelection: () -> Unit = {},
	onAddMemo: (LocalDate) -> Unit = {},
	onAddEvent: (LocalDate) -> Unit = {},
	currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
	onThemeModeChange: (ThemeMode) -> Unit = {},
	onRefreshHolidays: () -> Unit = {},
	onNavigateToDiaTable: () -> Unit = {},
	onNavigateToVacationSetting: () -> Unit = {},
	onNavigateToTextSizeSettings: () -> Unit = {},
	textSizes: CalendarTextSizes = CalendarTextSizes.DEFAULT,
	onBackup: () -> Unit = {},
	onRestore: () -> Unit = {},
	horizontal: Boolean? = null,
	showCrewPattern: Boolean = false,
	crewPattern: List<String> = listOf("AD", "BA", "CB", "DC"),
	crewPatternStartDate: LocalDate = LocalDate.of(2026, 2, 1),
	onToggleCrewPattern: (Boolean) -> Unit = {},
) {
	// 앱이 백그라운드에서 돌아올 때 오늘 날짜를 갱신
	var today by remember { mutableStateOf(LocalDate.now()) }
	val lifecycleOwner = LocalLifecycleOwner.current
	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				today = LocalDate.now()
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose {
			lifecycleOwner.lifecycle.removeObserver(observer)
		}
	}
	val currentMonth = remember(today) { today.yearMonth }
	val startMonth = remember { currentMonth.minusMonths(500) }
	val endMonth = remember { currentMonth.plusMonths(500) }
	val selections = remember { mutableStateListOf<CalendarDay>() }
	val daysOfWeek = remember { daysOfWeek() }

	val scope = rememberCoroutineScope()
	val context = LocalContext.current
	val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

	// FAB 상태 관리
	var showDatePicker by remember { mutableStateOf(false) }
	var showFabMenu by remember { mutableStateOf(false) }
	var fabSelectedDate by remember { mutableStateOf<LocalDate?>(null) }

	// 테마 설정 다이얼로그 상태
	var showThemeDialog by remember { mutableStateOf(false) }

	StatusBarColorUpdateEffect(MaterialTheme.colorScheme.background)

	// 테마 설정 다이얼로그
	if (showThemeDialog) {
		ThemeSettingDialog(
			currentThemeMode = currentThemeMode,
			onThemeModeChange = { mode ->
				onThemeModeChange(mode)
				showThemeDialog = false
			},
			onDismiss = { showThemeDialog = false }
		)
	}

	ModalNavigationDrawer(
		drawerState = drawerState,
		drawerContent = {
			HomeDrawerContent(
				isRefreshingHolidays = isRefreshingHolidays,
				showCrewPattern = showCrewPattern,
				onToggleCrewPattern = onToggleCrewPattern,
				officeName = officeName,
				onItemClick = { item ->

					scope.launch {
						drawerState.close()
						when (item) {
							DrawerItem.CALENDAR -> onNavigateToCalendarSelection()
							DrawerItem.ANNIVERSARY -> onNavigateToAnniversary()
							DrawerItem.SHIFT -> onNavigateToShiftSelection()
							DrawerItem.HOLIDAY_REFRESH -> onRefreshHolidays()
							DrawerItem.SETTINGS -> showThemeDialog = true
							DrawerItem.VACATION -> onNavigateToVacationSetting()
							DrawerItem.TEXT_SIZE -> onNavigateToTextSizeSettings()
							DrawerItem.BACKUP -> onBackup()
							DrawerItem.RESTORE -> onRestore()
							DrawerItem.MENU_UPLOAD -> {
								val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cafeteria-nine-psi.vercel.app/analyze"))
								context.startActivity(intent)
							}
						}
					}
				}
			)
		}

	) {
		Box(modifier = Modifier.fillMaxSize()) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.background)
				.applyScaffoldHorizontalPaddings()
				.applyScaffoldTopPadding(),
		) {
			var selectedIndex by remember { mutableIntStateOf(0) }

			val state = rememberCalendarState(
				startMonth = startMonth,
				endMonth = endMonth,
				firstVisibleMonth = currentMonth,
				firstDayOfWeek = daysOfWeek.first(),
				outDateStyle = OutDateStyle.EndOfRow,
			)
			val coroutineScope = rememberCoroutineScope()
			//val visibleMonth = rememberFirstVisibleMonthAfterScroll(state)
			val visibleMonth = rememberFirstCompletelyVisibleMonth(state)

			LaunchedEffect(visibleMonth.yearMonth.year) {
				onVisibleYearChanged(visibleMonth.yearMonth.year)
			}

			val titleYearMonth = visibleMonth.yearMonth
			val titleDaysInMonth = (1..titleYearMonth.lengthOfMonth()).map { titleYearMonth.atDay(it) }
			val titleRestCount = if (shiftPattern.isNotEmpty() && !isCustomShift) {
				titleDaysInMonth.count { date ->
					val shift = shiftScheduleMap[date]
					val hasVacation = vacationMap.containsKey(date)
					val isHoliday = holidayMap.containsKey(date)
					val isRest = !hasVacation && shift?.contains("휴") == true
					val isHolidayWorkRest = !hasVacation && shift != null && holidayWorkShifts.contains(shift) &&
						(isHoliday || date.dayOfWeek.value == 6 || date.dayOfWeek.value == 7)
					isRest || isHolidayWorkRest
				}
			} else null
			val titleCoverCount = if (shiftPattern.isNotEmpty() && !isCustomShift) {
				titleDaysInMonth
					.mapNotNull { date -> shiftInputMap[date] }
					.filter { it.colorHex.uppercase() == "#9C27B0" }
					.map { it.groupId }
					.distinct()
					.size
			} else null

			CompositionLocalProvider(LocalContentColor provides Color.White) {
				SimpleCalendarTitle(
					modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
					currentMonth = visibleMonth.yearMonth,
					isHorizontal = selectedIndex == 0,
					onMenuClick = {
						scope.launch {
							drawerState.open()
						}
					},
					goToday = {
						scope.launch {
							state.scrollToMonth(currentMonth)
							Toast.makeText(context, "오늘로 이동했어요", Toast.LENGTH_SHORT).show()
						}
					},
					onMonthSelected = { selectedYearMonth ->
						coroutineScope.launch {
							state.scrollToMonth(selectedYearMonth)
						}
					},
					restCount = titleRestCount,
					coverCount = titleCoverCount,
				)
			FullScreenCalendar(
				modifier = Modifier
					.weight(1f)
					.fillMaxWidth()
					.background(MaterialTheme.colorScheme.background)
					.testTag("Calendar"),
				state = state,
				horizontal = horizontal ?: (selectedIndex == 0),
				dayContent = { day ->
					val memosForDay = memosByDate[day.date] ?: emptyList()
					val eventsForDay = eventsByDate[day.date] ?: emptyList()
					val holidayName = holidayMap[day.date]
					val anniversaryName = anniversaryMap[day.date]
					val shiftName = shiftScheduleMap[day.date]
					val vacationShortName = vacationMap[day.date]
					val shiftInputInfo = shiftInputMap[day.date]
					// 근무조 패턴 계산
					val crewPatternLabel = if (showCrewPattern && crewPattern.isNotEmpty()) {
						val daysBetween = ChronoUnit.DAYS.between(crewPatternStartDate, day.date)
						if (daysBetween >= 0) {
							crewPattern[(daysBetween % crewPattern.size).toInt()]
						} else {
							val idx = ((daysBetween % crewPattern.size + crewPattern.size) % crewPattern.size).toInt()
							crewPattern[idx]
						}
					} else null
					Day(
						day = day,
						isSelected = selections.contains(day),
						isToday = day.position == DayPosition.MonthDate && day.date == today,
						holidayName = holidayName,
						anniversaryName = anniversaryName,
						shiftName = shiftName,
						swapDates = swapDates,
						shiftInputInfo = shiftInputInfo?.let { it.shortName to it.colorHex },
						holidayWorkShifts = holidayWorkShifts,
						vacationShortName = vacationShortName,
						memos = memosForDay,
						events = eventsForDay,
						textSizes = textSizes,
						crewPatternLabel = crewPatternLabel
					) { clicked ->
						onAction(HomeAction.OnDateClick(clicked.date))
						if (selections.contains(clicked)) {
							selections.remove(clicked)
						} else {
							selections.add(clicked)
						}
					}
				},
				monthBody = { _, content ->
					Box(
						modifier = Modifier
							.weight(1f)
							.testTag("MonthBody"),
					) {
						content()
					}
				},
				monthHeader = {
					MonthHeader(daysOfWeek = daysOfWeek)
				},
				monthFooter = { month ->
					val count = month.weekDays.flatten()
						.count { selections.contains(it) }

					MonthFooter(
						modifier = Modifier.applyScaffoldBottomPadding(),
						selectionCount = count,
						onDiaTableClick = onNavigateToDiaTable,
						shiftPattern = shiftPattern,
						isCustomShift = isCustomShift,
					)
				},
			)
			}
		}

			// FAB 영역
			ExpandableFab(
				showMenu = showFabMenu,
				selectedDate = fabSelectedDate,
				onFabClick = {
					// FAB 클릭 시 DatePicker 표시
					showDatePicker = true
				},
				onDismissMenu = {
					showFabMenu = false
					fabSelectedDate = null
				},
				onAddMemo = {
					showFabMenu = false
					fabSelectedDate?.let { date ->
						onAddMemo(date)
					}
					fabSelectedDate = null
				},
				onAddEvent = {
					showFabMenu = false
					fabSelectedDate?.let { date ->
						onAddEvent(date)
					}
					fabSelectedDate = null
				},
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(16.dp)
					.applyScaffoldBottomPadding()
			)

			// DatePicker 다이얼로그
			if (showDatePicker) {
				FabDatePickerDialog(
					initialDate = today,
					onDateSelected = { date ->
						showDatePicker = false
						fabSelectedDate = date
						showFabMenu = true
					},
					onDismiss = {
						showDatePicker = false
					}
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FabDatePickerDialog(
	initialDate: LocalDate,
	onDateSelected: (LocalDate) -> Unit,
	onDismiss: () -> Unit
) {
	// Material 3 DatePicker는 UTC 기준으로 동작하므로 UTC 정오를 사용하여 날짜 밀림 방지
	val datePickerState = rememberDatePickerState(
		initialSelectedDateMillis = initialDate
			.atStartOfDay(ZoneId.of("UTC"))
			.plusHours(12)
			.toInstant()
			.toEpochMilli()
	)

	DatePickerDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			TextButton(
				onClick = {
					datePickerState.selectedDateMillis?.let { millis ->
						// UTC 기준으로 변환하여 날짜 추출
						val selectedDate = Instant.ofEpochMilli(millis)
							.atZone(ZoneId.of("UTC"))
							.toLocalDate()
						onDateSelected(selectedDate)
					}
				}
			) {
				Text("선택")
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text("취소")
			}
		}
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.heightIn(min = 450.dp, max = 500.dp) // 높이를 어느 정도 고정!
		) {
			DatePicker(
				state = datePickerState,
				// 내부 패딩이나 수정을 최소화하여 기본 성능을 확보합니다.
			)
		}
		//DatePicker(state = datePickerState)
	}
}

@Composable
private fun ExpandableFab(
	showMenu: Boolean,
	selectedDate: LocalDate?,
	onFabClick: () -> Unit,
	onDismissMenu: () -> Unit,
	onAddMemo: () -> Unit,
	onAddEvent: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.End,
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		// 선택된 날짜 표시 (메뉴 표시 시)
		AnimatedVisibility(
			visible = showMenu && selectedDate != null,
			enter = fadeIn() + scaleIn(),
			exit = fadeOut() + scaleOut()
		) {
			selectedDate?.let { date ->
				Box(
					modifier = Modifier
						.clip(RoundedCornerShape(8.dp))
						.background(MaterialTheme.colorScheme.surfaceVariant)
						.padding(horizontal = 12.dp, vertical = 6.dp)
				) {
					Text(
						text = "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일에 추가",
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
		}

		// 캘린더 일정 추가 버튼
		if (showMenu) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Box(
					modifier = Modifier
						.clip(RoundedCornerShape(4.dp))
						.background(MaterialTheme.colorScheme.surface)
						.padding(horizontal = 8.dp, vertical = 4.dp)
				) {
					Text(
						text = "캘린더 일정",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onSurface
					)
				}
				SmallFloatingActionButton(
					onClick = onAddEvent,
					containerColor = MaterialTheme.colorScheme.secondaryContainer
				) {
					Icon(
						imageVector = Icons.Default.Event,
						contentDescription = "캘린더 일정 추가"
					)
				}
			}
		}

		// 메모 추가 버튼
		if (showMenu) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Box(
					modifier = Modifier
						.clip(RoundedCornerShape(4.dp))
						.background(MaterialTheme.colorScheme.surface)
						.padding(horizontal = 8.dp, vertical = 4.dp)
				) {
					Text(
						text = "메모",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onSurface
					)
				}
				SmallFloatingActionButton(
					onClick = onAddMemo,
					containerColor = MaterialTheme.colorScheme.tertiaryContainer
				) {
					Icon(
						imageVector = Icons.AutoMirrored.Filled.NoteAdd,
						contentDescription = "메모 추가"
					)
				}
			}
		}

		// 메인 FAB
		FloatingActionButton(
			onClick = {
				if (showMenu) {
					onDismissMenu()
				} else {
					onFabClick()
				}
			},
			containerColor = MaterialTheme.colorScheme.primary
		) {
			Icon(
				imageVector = if (showMenu) Icons.Default.Close else Icons.Default.Add,
				contentDescription = if (showMenu) "닫기" else "추가"
			)
		}
	}
}

private enum class DrawerItem {
	CALENDAR, ANNIVERSARY, SHIFT, HOLIDAY_REFRESH, SETTINGS, VACATION, TEXT_SIZE, BACKUP, RESTORE, MENU_UPLOAD
}

@Composable
private fun HomeDrawerContent(
	isRefreshingHolidays: Boolean = false,
	showCrewPattern: Boolean = false,
	onToggleCrewPattern: (Boolean) -> Unit = {},
	officeName: String? = null,
	onItemClick: (DrawerItem) -> Unit
) {
	ModalDrawerSheet(
		modifier = Modifier.width(270.dp)
	) {
		Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState())
		) {
		    // 헤더
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(120.dp)
					.background(MaterialTheme.colorScheme.primaryContainer),
				contentAlignment = Alignment.BottomStart
			) {
				Column(
					modifier = Modifier.padding(16.dp)
				) {
					val drawerContext = LocalContext.current
					val ssaid = remember { DeviceIdProvider.getSsaid(drawerContext) }
					val subscriptionRepository: SubscriptionRepository = koinInject()
					val drawerScope = rememberCoroutineScope()
					val snackbarHostState = remember { SnackbarHostState() }
					var isVipRefreshing by remember { mutableStateOf(false) }

					SnackbarHost(hostState = snackbarHostState)

					OutlinedButton(
						onClick = {
							drawerScope.launch {
								isVipRefreshing = true
								val isVip = subscriptionRepository.refreshVipStatus(ssaid)
								isVipRefreshing = false
								val message = if (isVip) "VIP가 확인되었습니다." else "VIP 권한이 없습니다."
								snackbarHostState.showSnackbar(message)
							}
						},
						enabled = !isVipRefreshing,
						modifier = Modifier.fillMaxWidth()
					) {
						if (isVipRefreshing) {
							CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
						} else {
							Text("VIP 상태 확인", style = MaterialTheme.typography.labelMedium)
						}
					}

					Spacer(modifier = Modifier.height(8.dp))

					Row(
						modifier = Modifier.fillMaxWidth(),
						verticalAlignment = Alignment.CenterVertically
					) {
						Text(
							text = "내근무",
							style = MaterialTheme.typography.headlineSmall,
							fontWeight = FontWeight.Bold,
							color = MaterialTheme.colorScheme.onPrimaryContainer
						)
						Spacer(modifier = Modifier.width(12.dp))
						Text(
							text = ssaid.ifBlank { "-" },
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
							modifier = Modifier.weight(1f)
						)
						IconButton(
							onClick = {
								val clipboard = drawerContext.getSystemService(ClipboardManager::class.java)
								clipboard?.setPrimaryClip(ClipData.newPlainText("device_id", ssaid))
								Toast.makeText(drawerContext, "기기 ID가 복사되었습니다", Toast.LENGTH_SHORT).show()
							},
							enabled = ssaid.isNotBlank(),
							modifier = Modifier.size(28.dp)
						) {
							Icon(
								imageVector = Icons.Default.ContentCopy,
								contentDescription = "기기 ID 복사",
								tint = MaterialTheme.colorScheme.onPrimaryContainer,
								modifier = Modifier.size(18.dp)
							)
						}
					}
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = "교번,교대,통상 모든 근무자의 달력",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
					)
				}
			}

			Spacer(modifier = Modifier.height(8.dp))

			// 메뉴 아이템들
			NavigationDrawerItem(
				icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
				label = { Text("캘린더 연동") },
				selected = false,
				onClick = { onItemClick(DrawerItem.CALENDAR) },
				modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
			)

			NavigationDrawerItem(
				icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
				label = { Text("기념일 관리") },
				selected = false,
				onClick = { onItemClick(DrawerItem.ANNIVERSARY) },
				modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
			)

			NavigationDrawerItem(
				icon = {
					if (isRefreshingHolidays) {
						CircularProgressIndicator(
							modifier = Modifier.size(24.dp),
							strokeWidth = 2.dp
						)
					} else {
						Icon(Icons.Default.Refresh, contentDescription = null)
					}
				},
				label = { Text(if (isRefreshingHolidays) "공휴일 갱신 중..." else "공휴일 갱신") },
				selected = false,
				onClick = { if (!isRefreshingHolidays) onItemClick(DrawerItem.HOLIDAY_REFRESH) },
				modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
			)

			NavigationDrawerItem(
				icon = { Icon(Icons.Default.Work, contentDescription = null) },
				label = { Text("내근무 생성") },
				selected = false,
				onClick = { onItemClick(DrawerItem.SHIFT) },
				modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
			)


			HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

			NavigationDrawerItem(
				icon = { Icon(Icons.Default.DeviceThermostat, contentDescription = null) },
				label = { Text("테마 설정") },
				selected = false,
				onClick = { onItemClick(DrawerItem.SETTINGS) },
				modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
			)

			NavigationDrawerItem(
				icon = { Icon(Icons.Default.HolidayVillage, contentDescription = null) },
				label = { Text("근태종류 설정") },
				selected = false,
				onClick = { onItemClick(DrawerItem.VACATION) },
				modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
			)

			NavigationDrawerItem(
				icon = { Icon(Icons.Default.TextFields, contentDescription = null) },
				label = { Text("텍스트 크기 설정") },
				selected = false,
				onClick = { onItemClick(DrawerItem.TEXT_SIZE) },
				modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
			)

			NavigationDrawerItem(
				icon = { Icon(Icons.Default.Groups, contentDescription = null) },
				label = {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically
					) {
						Text("근무조 표시")
						Switch(
							checked = showCrewPattern,
							onCheckedChange = onToggleCrewPattern,
							modifier = Modifier.height(24.dp)
						)
					}
				},
				selected = false,
				onClick = { onToggleCrewPattern(!showCrewPattern) },
				modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
			)

			NavigationDrawerItem(
				icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
				label = { Text("식당메뉴사진업로드") },
				selected = false,
				onClick = { onItemClick(DrawerItem.MENU_UPLOAD) },
				modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
			)

			HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

			// 백업/복원 섹션
			NavigationDrawerItem(
				icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
				label = { Text("데이터 백업") },
				selected = false,
				onClick = { onItemClick(DrawerItem.BACKUP) },
				modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
			)

			NavigationDrawerItem(
				icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
				label = { Text("데이터 복원") },
				selected = false,
				onClick = { onItemClick(DrawerItem.RESTORE) },
				modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
			)

			Spacer(modifier = Modifier.height(80.dp))
		}
	}
}

@Composable
private fun FullScreenCalendar(
	modifier: Modifier,
	state: CalendarState,
	horizontal: Boolean,
	dayContent: @Composable BoxScope.(CalendarDay) -> Unit,
	monthHeader: @Composable ColumnScope.(CalendarMonth) -> Unit,
	monthBody: @Composable ColumnScope.(CalendarMonth, content: @Composable () -> Unit) -> Unit,
	monthFooter: @Composable ColumnScope.(CalendarMonth) -> Unit,
) {
	if (horizontal) {
		HorizontalCalendar(
			modifier = modifier,
			state = state,
			calendarScrollPaged = true,
			contentHeightMode = ContentHeightMode.Fill,
			dayContent = dayContent,
			monthBody = monthBody,
			monthHeader = monthHeader,
			monthFooter = monthFooter,
		)
	}
}


@Composable
private fun MonthHeader(daysOfWeek: List<DayOfWeek>) {
	Row(
		Modifier
			.fillMaxWidth()
			.testTag("MonthHeader")
			.background(MaterialTheme.colorScheme.background)
			.padding(vertical = 1.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		for (dayOfWeek in daysOfWeek) {
			Text(
				modifier = Modifier.weight(1f),
				textAlign = TextAlign.Center,
				fontSize = 12.sp,
				color = MaterialTheme.colorScheme.onBackground,
				text = dayOfWeek.displayText(),
				style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
			)
		}
	}
}

@Composable
private fun MonthFooter(
	selectionCount: Int,
	modifier: Modifier,
	onDiaTableClick: () -> Unit = {},
	shiftPattern: List<String> = emptyList(),
	isCustomShift: Boolean = false,
) {
	var showShiftOrderDialog by remember { mutableStateOf(false) }

	Box(
		Modifier
			.fillMaxWidth()
			.testTag("MonthFooter")
			.background(MaterialTheme.colorScheme.surface) // 배경색을 조금 더 차분하게 변경
			.padding(vertical = 2.dp, horizontal = 5.dp)
			.then(modifier),
		contentAlignment = Alignment.Center,
	) {
		if (shiftPattern.isNotEmpty() && !isCustomShift) Row(
			Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.Center,
			verticalAlignment = Alignment.CenterVertically,
		) {
			Spacer(modifier = Modifier.weight(1f))
			Text(
				text = "📋근무표",
				modifier = Modifier
					.clickable(
						showRipple = true,
						onClick = onDiaTableClick
					)
					.padding(horizontal = 4.dp, vertical = 1.dp),
				fontSize = 15.sp,
				style = TextStyle(
					platformStyle = PlatformTextStyle(includeFontPadding = false),
					fontWeight = FontWeight.Bold
				),
				color = MaterialTheme.colorScheme.onBackground
			)
			Spacer(modifier = Modifier.width(20.dp))
			Text(
				text = "🔁교번순서",
				modifier = Modifier
					.clickable(
						showRipple = true,
						onClick = { showShiftOrderDialog = true }
					)
					.padding(horizontal = 4.dp, vertical = 1.dp),
				fontSize = 15.sp,
				style = TextStyle(
					platformStyle = PlatformTextStyle(includeFontPadding = false),
					fontWeight = FontWeight.Bold
				),
				color = MaterialTheme.colorScheme.onBackground
			)


			Spacer(modifier = Modifier.weight(1f))
		}
	}

	if (showShiftOrderDialog) {
		ShiftOrderDialog(
			shiftPattern = shiftPattern,
			onDismiss = { showShiftOrderDialog = false }
		)
	}
}

@Composable
private fun ShiftOrderDialog(
	shiftPattern: List<String>,
	onDismiss: () -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = {
			Text(
				text = "교번순서",
				fontWeight = FontWeight.Bold
			)
		},
		text = {
			if (shiftPattern.isEmpty()) {
				Text(
					text = "교번 설정이 없습니다.\n교번 설정에서 승무소를 먼저 선택해주세요.",
					textAlign = TextAlign.Center,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			} else {
				LazyVerticalGrid(
					columns = GridCells.Fixed(7),
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(max = 400.dp),
					horizontalArrangement = Arrangement.spacedBy(4.dp),
					verticalArrangement = Arrangement.spacedBy(4.dp)
				) {
					itemsIndexed(shiftPattern) { index, shiftName ->
						val isDark = isSystemInDarkTheme()
						val (bgColor, txtColor) = when {
							shiftName.contains("휴") || shiftName.contains("비") ->
								Color(0xFFFFCDD2) to Color(0xFFC62828)
							shiftName.contains("대") ->
								if (isDark) Color(0xFF1B5E20) to Color(0xFFA5D6A7)
								else Color(0xFFC8E6C9) to Color(0xFF2E7D32)
							else ->
								MaterialTheme.colorScheme.primaryContainer to
									MaterialTheme.colorScheme.onPrimaryContainer
						}
						val displayName = if (shiftName.contains("~")) "~" else shiftName
						Box(
							modifier = Modifier
								.clip(RoundedCornerShape(4.dp))
								.background(bgColor)
								.padding(vertical = 6.dp),
							contentAlignment = Alignment.Center
						) {
							Text(
								text = displayName,
								fontSize = 13.sp,
								color = txtColor,
								textAlign = TextAlign.Center,
								style = TextStyle(
									platformStyle = PlatformTextStyle(includeFontPadding = false),
									fontWeight = FontWeight.Bold
								),
								maxLines = 1
							)
						}
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) {
				Text("닫기")
			}
		}
	)
}

@Composable
private fun Day(
	day: CalendarDay,
	isSelected: Boolean,
	isToday: Boolean,
	holidayName: String? = null,
	anniversaryName: String? = null,
	shiftName: String? = null,
	swapDates: Set<LocalDate> = emptySet(),
	shiftInputInfo: Pair<String, String>? = null, // (shortName, colorHex) for 충당
	holidayWorkShifts: List<String> = emptyList(),
	vacationShortName: String? = null,
	memos: List<Memo> = emptyList(),
	events: List<CalendarEvent> = emptyList(),
	textSizes: CalendarTextSizes = CalendarTextSizes.DEFAULT,
	crewPatternLabel: String? = null,
	onClick: (CalendarDay) -> Unit,
) {
	val isHoliday = holidayName != null

	// 강조하고 싶은 오늘 날짜 테두리 색상과 두께를 정의합니다.
	val todayBorderWidth = 1.dp
	val todayBorderColor = colorResource(id = R.color.example_2_red) // 혹은 원하는 테두리 색상 (예: R.color.today_border_color)

	Column(
		Modifier
			.fillMaxSize()
			.clip(RectangleShape)
			.border(
				width = 0.1.dp,
				color = colorResource(id = R.color.calendarBorder_color),
				shape = RectangleShape
			)
			// 2. 오늘 날짜일 경우 강조 테두리 추가 적용
			.then(
				if (isToday) {
					Modifier.border(
						width = todayBorderWidth,
						color = todayBorderColor,
						shape = RectangleShape
					)
				} else {
					Modifier
				}
			)
			.background(
				color = when {
					isSelected -> colorResource(R.color.example_1_selection_color)
					isToday -> colorResource(id = R.color.today_backgroundColor)
					else -> Color.Transparent
				},
			)
			.clickable(
				enabled = day.position == DayPosition.MonthDate,
				showRipple = !isSelected,
				onClick = { onClick(day) },
			),
		verticalArrangement = Arrangement.Top,
	) {
		// 근무조 패턴 표시 (셀 상단)
		if (day.position == DayPosition.MonthDate && crewPatternLabel != null) {
			CrewPatternBadge(label = crewPatternLabel, fontSize = textSizes.crewPatternFontSize)
		}

		// 날짜
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(all = 0.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Center
		) {
			val textColor = when (day.position) {
				DayPosition.MonthDate -> {
					when {
						// 일요일 또는 공휴일은 빨간색
						day.date.dayOfWeek.value == 7 || isHoliday -> MaterialTheme.colorScheme.error
						day.date.dayOfWeek.value == 6 -> MaterialTheme.colorScheme.primary
						isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
						else -> MaterialTheme.colorScheme.onBackground
					}
				}
				else -> MaterialTheme.colorScheme.outlineVariant
			}
			Text(
				text = "${day.date.dayOfMonth}일",
				color = textColor,
				style = TextStyle(
					fontSize = textSizes.dateFontSize.sp,
					fontFamily = FontFamily.Monospace,
					fontWeight = FontWeight.SemiBold,
					platformStyle = PlatformTextStyle(includeFontPadding = false),
				)
			)

		}
		// 공휴일 이름 표시 (공휴일 유무와 상관없이 일정 공간 확보)
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(10.dp)
			    .offset(y = (-1).dp),
			contentAlignment = Alignment.TopCenter
		) {
			if (day.position == DayPosition.MonthDate && holidayName != null) {
				Text(
					text = holidayName,
					color = MaterialTheme.colorScheme.error,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					textAlign = TextAlign.Center,
					style = TextStyle(
						fontSize = 8.sp,
						fontWeight = FontWeight.Medium,
						platformStyle = PlatformTextStyle(includeFontPadding = false),
						lineHeight = 8.sp
					),
					modifier = Modifier.fillMaxWidth()
				)
			}
		}

		if (day.position == DayPosition.MonthDate && anniversaryName != null) {
			Text(
				text = anniversaryName,
				color = Color(0xFF6366F1),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				textAlign = TextAlign.Center,
				style = TextStyle(
					fontSize = 10.sp,
					fontWeight = FontWeight.Bold,
					platformStyle = PlatformTextStyle(includeFontPadding = false),
					lineHeight = 10.sp
				),
				modifier = Modifier.fillMaxWidth()
			)
		}

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(all = 0.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Center
		) {
			// 교번 
			if (day.position == DayPosition.MonthDate) {
				if (vacationShortName != null) {
					VacationBadge(shortName = vacationShortName, fontSize = textSizes.shiftFontSize)
				} else if (shiftName != null) {
					// 충당인 경우 충당 색상 적용
					val isShiftInput = shiftInputInfo != null
					val shiftInputColorHex = shiftInputInfo?.second
					// ShiftBadge determines if it is swap by checking swapDates
					val isSwap = !isShiftInput && swapDates.contains(day.date)
					val isHolidayWork = holidayWorkShifts.contains(shiftName) &&
						(day.date.dayOfWeek.value == 6 || day.date.dayOfWeek.value == 7 || holidayName != null)
					ShiftBadge(
						shiftName = shiftName,
						isSwap = isSwap,
						isShiftInput = isShiftInput,
						shiftInputColorHex = shiftInputColorHex,
						isHolidayWork = isHolidayWork,
						fontSize = textSizes.shiftFontSize
					)
				}
			}
		}



		// 메모 + 캘린더 이벤트 표시 (합쳐서 최대 3개)
		val totalItems = memos.size + events.size
		if (day.position == DayPosition.MonthDate && totalItems > 0) {
			Spacer(modifier = Modifier.height(2.dp))
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 0.dp),
			) {
				Column(
					verticalArrangement = Arrangement.spacedBy(1.dp)
				) {
					// 먼저 캘린더 이벤트 표시 (최대 3개 중)
					var displayedCount = 0
					events.take(3).forEach { event ->
						EventIndicator(event = event, fontSize = textSizes.eventFontSize)
						displayedCount++
					}
					// 남은 슬롯에 메모 표시
					val remainingSlots = 3 - displayedCount
					memos.take(remainingSlots).forEach { memo ->
						MemoIndicator(memo = memo, fontSize = textSizes.memoFontSize)
						displayedCount++
					}
				}
				// 3개 이상일 때 "+N" 표시
				if (totalItems > 3) {
					Text(
						text = "+${totalItems - 3}",
						fontSize = 8.sp,
						lineHeight = 8.sp,
						color = MaterialTheme.colorScheme.outline,
						style = TextStyle(
							platformStyle = PlatformTextStyle(includeFontPadding = false),
							fontWeight = FontWeight.Bold
						),
						modifier = Modifier
							.padding(start = 2.dp)
					)
				}
			}
		}

	}
}

@Composable
private fun CrewPatternBadge(label: String, fontSize: Float = 9f) {
	val isDark = isSystemInDarkTheme()
	val firstChar = label.firstOrNull()?.uppercaseChar()

	// 색상 정의: (배경색, 텍스트색)
	val (bgColor, textColor) = when (firstChar) {
	'A' -> if (isDark) Color(0xFF2A0E0E) to Color(0xFFEF9A9A) else Color(0xFFFFCDD2) to Color(0xFFC62828)
	'B' -> if (isDark) Color(0xFF0E1A2A) to Color(0xFF90CAF9) else Color(0xFFBBDEFB) to Color(0xFF1565C0)
	'C' -> if (isDark) Color(0xFF0E2A0E) to Color(0xFFA5D6A7) else Color(0xFFC8E6C9) to Color(0xFF2E7D32)
	'D' -> if (isDark) Color(0xFF1A0E2A) to Color(0xFFCE93D8) else Color(0xFFE1BEE7) to Color(0xFF7B1FA2)
	else -> if (isDark) Color(0xFF1A1A1A) to Color(0xFFBDBDBD) else Color(0xFFE0E0E0) to Color(0xFF616161)

	}

	// 다크 모드일 때 배경색은 더 어둡게(Deep), 텍스트는 더 밝게(Light) 조정


	Box(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(2.dp))
			.background(MaterialTheme.colorScheme.background)
			.padding(horizontal = 4.dp, vertical = 1.dp), // 가독성을 위해 패딩을 미세하게 조정했습니다.
		contentAlignment = Alignment.Center
	) {
		Text(
			text = label,
			fontSize = fontSize.sp,
			lineHeight = (fontSize + 1).sp,
			fontWeight = FontWeight.Bold,
			color = textColor,
			maxLines = 1,
			style = TextStyle(
				platformStyle = PlatformTextStyle(includeFontPadding = false),
				lineHeightStyle = LineHeightStyle(
					alignment = LineHeightStyle.Alignment.Center,
					trim = LineHeightStyle.Trim.Both
				)
			)
		)
	}
}


@Composable
private fun VacationBadge(shortName: String, fontSize: Float = 14f) {
	Box(
		modifier = Modifier
			.clip(RoundedCornerShape(3.dp))
			.background(MaterialTheme.colorScheme.errorContainer)
			.padding(horizontal = 4.dp, vertical = 1.dp),
		contentAlignment = Alignment.Center
	) {
		Text(
			text = shortName,
			fontSize = fontSize.sp,
			lineHeight = (fontSize - 4).coerceAtLeast(8f).sp,
			fontWeight = FontWeight.Bold,
			color = MaterialTheme.colorScheme.onErrorContainer,
			maxLines = 1,
			style = TextStyle(
				platformStyle = PlatformTextStyle(includeFontPadding = false),
				lineHeightStyle = LineHeightStyle(
					alignment = LineHeightStyle.Alignment.Center,
					trim = LineHeightStyle.Trim.Both
				)
			)
		)
	}
}

// ShiftBadge는 presentation/shared/ShiftBadge.kt 로 이동되었습니다.

@Composable
private fun MemoIndicator(memo: Memo, fontSize: Float = 8f) {
	val memoColor = Color(memo.hexColorString.toColorInt())

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(2.dp))
			.background(
				if (memo.isCompleted)
					MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
				else
					Color.Transparent
					//memoColor.copy(alpha = 0.3f)
			)
			.padding(horizontal = 0.dp, vertical = 1.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.Start
	) {
		Box(
			modifier = Modifier
				.width(3.dp)
				.height(13.dp)
				.clip(RectangleShape)
				.background(
					if (memo.isCompleted)
						MaterialTheme.colorScheme.outline
					else
						memoColor
				)
		)
		Text(
			text = memo.title,
			fontSize = fontSize.sp,
			lineHeight = (fontSize + 2).sp,
			maxLines = 1,
			//overflow = TextOverflow.Ellipsis,
			color = if (memo.isCompleted)
				MaterialTheme.colorScheme.outline
			else
				MaterialTheme.colorScheme.onSurface,
			textDecoration = if (memo.isCompleted) TextDecoration.LineThrough else null,
			style = TextStyle(
				// 폰트 자체의 불필요한 상하 패딩 제거
				platformStyle = PlatformTextStyle(includeFontPadding = false),
				lineHeightStyle = LineHeightStyle(
					alignment = LineHeightStyle.Alignment.Center,
					trim = LineHeightStyle.Trim.Both
				)
			),
			modifier = Modifier.padding(start = 1.dp)
		)
	}
}

@Composable
private fun EventIndicator(event: CalendarEvent, fontSize: Float = 8f) {
	val eventColor = Color(event.color)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(2.dp))
			.background(eventColor.copy(alpha = 0.3f))
			.padding(horizontal = 1.dp, vertical = 1.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.width(3.dp)
				.height(13.dp)
				.clip(RectangleShape)
				.background(eventColor)
		)
		Text(
			text = event.title,
			fontSize = fontSize.sp,
			lineHeight = (fontSize + 2).sp,
			maxLines = 1,
			//overflow = TextOverflow.Ellipsis,
			color = MaterialTheme.colorScheme.onSurface,
			style = TextStyle(
				platformStyle = PlatformTextStyle(includeFontPadding = false),
				lineHeightStyle = LineHeightStyle(
					alignment = LineHeightStyle.Alignment.Center,
					trim = LineHeightStyle.Trim.Both
				)
			),
			modifier = Modifier.padding(start = 1.dp)
		)
	}
}

@Composable
private fun ThemeSettingDialog(
	currentThemeMode: ThemeMode,
	onThemeModeChange: (ThemeMode) -> Unit,
	onDismiss: () -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = {
			Text(
				text = "테마 설정",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold
			)
		},
		text = {
			Column {
				ThemeMode.entries.forEach { mode ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable(
								showRipple = true,
								onClick = { onThemeModeChange(mode) }
							)
							.padding(vertical = 8.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						RadioButton(
							selected = currentThemeMode == mode,
							onClick = { onThemeModeChange(mode) }
						)
						Spacer(modifier = Modifier.width(8.dp))
						Text(
							text = mode.displayName,
							style = MaterialTheme.typography.bodyLarge
						)
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) {
				Text("닫기")
			}
		}
	)
}

@Composable
private fun FooterActionButton(
	text: String,
	icon: ImageVector,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Surface(
		onClick = onClick,
		shape = RoundedCornerShape(8.dp),
		color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // 은은한 배경색
		contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
		modifier = modifier
	) {
		Row(
			modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Center
		) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				modifier = Modifier.size(16.dp)
			)
			Spacer(modifier = Modifier.width(4.dp))
			Text(
				text = text,
				fontSize = 13.sp,
				fontWeight = FontWeight.Bold,
				style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
			)
		}
	}
}
