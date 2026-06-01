package com.sonbum.diacalendar2.presentation.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeRoot(
	onNavigateToDetail: (String) -> Unit,
	onNavigateToCalendarSelection: () -> Unit = {},
	onNavigateToAnniversary: () -> Unit = {},
	onNavigateToShiftSelection: () -> Unit = {},
	onNavigateToAddMemo: (String) -> Unit = {},
	onNavigateToAddEvent: (String) -> Unit = {},
	onNavigateToDiaTable: () -> Unit = {},
	onNavigateToVacationSetting: () -> Unit = {},
	onNavigateToTextSizeSettings: () -> Unit = {},
	onNavigateToWorkAlarmSettings: () -> Unit = {},
	modifier: Modifier,
	viewModel: HomeViewModel = koinViewModel()
) {
	val state by viewModel.state.collectAsStateWithLifecycle()
	val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
	val textSizes by viewModel.textSizes.collectAsStateWithLifecycle()
	val showCrewPattern by viewModel.showCrewPattern.collectAsStateWithLifecycle()
	val crewPattern by viewModel.crewPattern.collectAsStateWithLifecycle()
	val crewPatternStartDate by viewModel.crewPatternStartDate.collectAsStateWithLifecycle()
	val lifecycleOwner = LocalLifecycleOwner.current
	val context = LocalContext.current

	// 백업 파일 저장 launcher
	val backupLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.CreateDocument("application/json")
	) { uri ->
		uri?.let { viewModel.exportBackup(it) }
	}

	// 복원 파일 선택 launcher
	val restoreLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenDocument()
	) { uri ->
		uri?.let { viewModel.importBackup(it) }
	}

	// 앱이 포그라운드로 돌아올 때 캘린더 이벤트 새로고침
	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				viewModel.refreshCalendarEvents()
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose {
			lifecycleOwner.lifecycle.removeObserver(observer)
		}
	}

	// 이벤트 수집 및 네비게이션 처리
	LaunchedEffect(viewModel.event) {
		viewModel.event.collect { event ->
			when (event) {
				is HomeEvent.NavigateToDateDetail -> {
					onNavigateToDetail(event.date)
				}
				is HomeEvent.ShowMessage -> {
					Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
				}
				is HomeEvent.BackupRestored -> {
					// 복원 후 앱 데이터 새로고침
					viewModel.refreshCalendarEvents()
				}
			}
		}
	}

	HomeScreen(
		modifier = modifier,
		memosByDate = state.memosByDate,
		eventsByDate = state.eventsByDate,
		holidayMap = state.holidayMap,
		anniversaryMap = state.anniversaryMap,
		shiftScheduleMap = state.shiftScheduleMap,
		swapDates = state.swapDates,
		shiftInputMap = state.shiftInputMap,
		holidayWorkShifts = state.holidayWorkShifts,
		vacationMap = state.vacationMap,
		isRefreshingHolidays = state.isRefreshingHolidays,
		shiftPattern = state.shiftPattern,
		isCustomShift = state.isCustomShift,
		officeName = state.officeName,
		onAction = viewModel::onAction,
		onVisibleYearChanged = viewModel::onVisibleYearChanged,
		onNavigateToCalendarSelection = onNavigateToCalendarSelection,
		onNavigateToAnniversary = onNavigateToAnniversary,
		onNavigateToShiftSelection = onNavigateToShiftSelection,
		onAddMemo = { date ->
			onNavigateToAddMemo(date.toString())
		},
		onAddEvent = { date ->
			onNavigateToAddEvent(date.toString())
		},
		currentThemeMode = themeMode,
		onThemeModeChange = viewModel::setThemeMode,
		onRefreshHolidays = viewModel::refreshHolidays,
		onNavigateToDiaTable = onNavigateToDiaTable,
		onNavigateToVacationSetting = onNavigateToVacationSetting,
		onNavigateToTextSizeSettings = onNavigateToTextSizeSettings,
		onNavigateToWorkAlarmSettings = onNavigateToWorkAlarmSettings,
		textSizes = textSizes,
		onBackup = {
			// 파일 이름에 현재 날짜/시간 포함
			val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
			backupLauncher.launch("diacalendar_backup_$timestamp.json")
		},
		onRestore = {
			restoreLauncher.launch(arrayOf("application/json"))
		},
		showCrewPattern = showCrewPattern,
		crewPattern = crewPattern,
		crewPatternStartDate = crewPatternStartDate,
		onToggleCrewPattern = viewModel::toggleShowCrewPattern
	)
}
