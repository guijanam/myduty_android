package com.sonbum.diacalendar2.presentation.subway

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubwayPositionScreen(
	myTrainNo: String,
	line: Int,
	officeName: String,
	onBack: () -> Unit,
	modifier: Modifier = Modifier,
	viewModel: SubwayPositionViewModel = koinViewModel(),
) {
	val state by viewModel.state.collectAsStateWithLifecycle()

	LaunchedEffect(myTrainNo, line, officeName) {
		viewModel.initialize(myTrainNo, line, officeName)
	}

	// 화면이 보일 때(ON_RESUME)만 조회/자동 갱신, 가려지면(ON_PAUSE) 중단한다.
	val lifecycleOwner = LocalLifecycleOwner.current
	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			when (event) {
				Lifecycle.Event.ON_RESUME -> viewModel.start()
				Lifecycle.Event.ON_PAUSE -> viewModel.stop()
				else -> Unit
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose {
			lifecycleOwner.lifecycle.removeObserver(observer)
			viewModel.stop()
		}
	}

	val lineColor = lineColor(line)

	Scaffold(
		modifier = modifier,
		topBar = {
			TopAppBar(
				title = { Text("${line}호선 ${myTrainNo}열차") },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
					}
				},
				actions = {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							text = "${state.secondsUntilRefresh}초 후 갱신",
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
						IconButton(onClick = { viewModel.refresh(myTrainNo, line, officeName) }) {
							Icon(Icons.Filled.Refresh, contentDescription = "새로고침")
						}
					}
				}
			)
		},
		bottomBar = {
			if (state.tabs.isNotEmpty()) {
				SubwayBranchTabs(
					tabs = state.tabs,
					selectedKey = state.selectedTabKey,
					accent = lineColor,
					onSelect = viewModel::selectTab
				)
			}
		}
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
		) {
			SubwayHeader(
				baseTime = state.baseTimeText,
				count = state.totalTrainCount,
				accent = lineColor
			)

			when {
				state.isLoading && state.stations.isEmpty() ->
					CenterBox { CircularProgressIndicator() }

				state.errorMessage != null ->
					CenterBox {
						Column(horizontalAlignment = Alignment.CenterHorizontally) {
							Text("불러오기 실패", style = MaterialTheme.typography.titleMedium)
							Text(
								state.errorMessage ?: "",
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
							Button(
								onClick = { viewModel.refresh(myTrainNo, line, officeName) },
								modifier = Modifier.padding(top = 12.dp)
							) { Text("다시 시도") }
						}
					}

				state.stations.isEmpty() ->
					CenterBox {
						Column(horizontalAlignment = Alignment.CenterHorizontally) {
							Text(
								"${line}호선에 지금 운행 중인 열차가 없습니다.",
								style = MaterialTheme.typography.bodyMedium
							)
							Text(
								"차량기지에서 막 출고했거나 본선 진입 전인 열차는 " +
									"실시간 위치에 아직 표시되지 않을 수 있습니다.",
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
								textAlign = TextAlign.Center,
								modifier = Modifier.padding(top = 8.dp)
							)
						}
					}

				state.isLoop ->
					SubwayLoopDiagram(
						stations = state.stations,
						myTrainIndex = state.myTrainIndex,
						selectedTabKey = state.selectedTabKey,
						accent = lineColor,
						modifier = Modifier.fillMaxSize()
					)

				else ->
					SubwayLinearDiagram(
						stations = state.stations,
						myTrainIndex = state.myTrainIndex,
						selectedTabKey = state.selectedTabKey,
						accent = lineColor,
						modifier = Modifier.fillMaxSize()
					)
			}
		}
	}
}

/** "22:07 기준 16대" — 기기 시계가 아니라 데이터 수신시각. */
@Composable
private fun SubwayHeader(baseTime: String, count: Int, accent: Color) {
	Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
		Row(
			modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Surface(color = accent, shape = RoundedCornerShape(14.dp)) {
				Text(
					text = if (baseTime.isBlank()) "${count}대" else "$baseTime 기준 ${count}대",
					style = MaterialTheme.typography.labelLarge,
					fontWeight = FontWeight.SemiBold,
					color = Color.White,
					modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
				)
			}
			Spacer(Modifier.width(10.dp))
			Text(
				text = "30초마다 자동 갱신",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

/** 하단 지선/방향 탭. */
@Composable
private fun SubwayBranchTabs(
	tabs: List<SubwayTabUi>,
	selectedKey: String?,
	accent: Color,
	onSelect: (String) -> Unit
) {
	val selectedIndex = tabs.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
	ScrollableTabRow(
		selectedTabIndex = selectedIndex,
		edgePadding = 0.dp,
		containerColor = MaterialTheme.colorScheme.surface,
		contentColor = accent
	) {
		tabs.forEachIndexed { index, tab ->
			Tab(
				selected = index == selectedIndex,
				onClick = { onSelect(tab.key) },
				text = {
					Text(
						text = if (tab.trainCount > 0) "${tab.label} ${tab.trainCount}" else tab.label,
						style = MaterialTheme.typography.labelLarge,
						fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
					)
				}
			)
		}
	}
}

/** 순환선이 아닌 노선: 세로 한 줄. */
@Composable
private fun SubwayLinearDiagram(
	stations: List<SubwayStationUi>,
	myTrainIndex: Int?,
	selectedTabKey: String?,
	accent: Color,
	modifier: Modifier = Modifier
) {
	val listState = rememberLazyListState()

	// 탭 전환 또는 내 열차 위치 변동 시에만 스크롤. stations에 key를 걸면 30초마다 화면이 튄다.
	LaunchedEffect(selectedTabKey, myTrainIndex) {
		myTrainIndex?.let {
			listState.animateScrollToItem(index = it.coerceAtLeast(0), scrollOffset = -240)
		}
	}

	LazyColumn(state = listState, modifier = modifier) {
		itemsIndexed(stations, key = { _, s -> s.statnId }) { index, station ->
			StationRailRow(
				station = station,
				isFirst = index == 0,
				isLast = index == stations.lastIndex,
				accent = accent
			)
		}
	}
}

/**
 * 2호선 본선: 좌우 두 열의 순환 고리(TOPIS 노선도와 동일한 형태).
 * 좌열은 위→아래, 우열은 아래→위로 읽어 전체가 하나의 고리가 된다.
 * 역 수가 43개로 고정이라 LazyColumn 없이 스크롤 컬럼으로 충분하다.
 */
@Composable
private fun SubwayLoopDiagram(
	stations: List<SubwayStationUi>,
	myTrainIndex: Int?,
	selectedTabKey: String?,
	accent: Color,
	modifier: Modifier = Modifier
) {
	val scrollState = rememberScrollState()
	val rowOffsets = remember(selectedTabKey) { mutableStateMapOf<Int, Int>() }

	val half = (stations.size + 1) / 2
	val leftColumn = stations.take(half)                       // 위 → 아래
	val rightColumn = stations.drop(half).reversed()           // 아래 → 위

	LaunchedEffect(selectedTabKey, myTrainIndex, rowOffsets.size) {
		val target = myTrainIndex ?: return@LaunchedEffect
		// 고리에서의 인덱스를 화면상의 행 번호로 환산한다.
		val row = if (target < half) target else stations.lastIndex - target
		rowOffsets[row]?.let { y ->
			scrollState.animateScrollTo((y - 240).coerceAtLeast(0))
		}
	}

	Column(
		modifier = modifier
			.verticalScroll(scrollState)
			.padding(vertical = 12.dp)
	) {
		LoopCap(text = "순환 계속", accent = accent)
		val rows = maxOf(leftColumn.size, rightColumn.size)
		repeat(rows) { row ->
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.onGloballyPositioned { rowOffsets[row] = it.positionInParent().y.toInt() }
			) {
				LoopHalfRow(
					station = leftColumn.getOrNull(row),
					accent = accent,
					alignEnd = true,
					modifier = Modifier.weight(1f)
				)
				LoopHalfRow(
					station = rightColumn.getOrNull(row),
					accent = accent,
					alignEnd = false,
					modifier = Modifier.weight(1f)
				)
			}
		}
		LoopCap(text = "순환 계속", accent = accent)
	}
}

@Composable
private fun LoopCap(text: String, accent: Color) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 6.dp),
		horizontalArrangement = Arrangement.Center,
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.height(3.dp)
				.width(90.dp)
				.background(accent, RoundedCornerShape(2.dp))
		)
		Text(
			text = text,
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.padding(horizontal = 8.dp)
		)
		Box(
			modifier = Modifier
				.height(3.dp)
				.width(90.dp)
				.background(accent, RoundedCornerShape(2.dp))
		)
	}
}

/** 고리 한쪽 열의 한 행. alignEnd=true면 레일이 오른쪽(좌열), false면 왼쪽(우열). */
@Composable
private fun LoopHalfRow(
	station: SubwayStationUi?,
	accent: Color,
	alignEnd: Boolean,
	modifier: Modifier = Modifier
) {
	if (station == null) {
		Box(modifier = modifier.height(52.dp))
		return
	}
	Row(
		modifier = modifier,
		verticalAlignment = Alignment.CenterVertically
	) {
		if (alignEnd) {
			TrainPillStack(station, alignEnd = true, modifier = Modifier.weight(1f))
			StationLabel(station, accent, alignEnd = true)
			Rail(station, accent)
		} else {
			Rail(station, accent)
			StationLabel(station, accent, alignEnd = false)
			TrainPillStack(station, alignEnd = false, modifier = Modifier.weight(1f))
		}
	}
}

/** 세로 한 줄 노선도의 한 행: [좌 pill] [레일] [역명 + 우 pill]. */
@Composable
private fun StationRailRow(
	station: SubwayStationUi,
	isFirst: Boolean,
	isLast: Boolean,
	accent: Color
) {
	val leftTrains = station.trains.filter { it.dto.updnLine == "1" }
	val rightTrains = station.trains.filter { it.dto.updnLine != "1" }

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(56.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Row(
			modifier = Modifier.weight(1f),
			horizontalArrangement = Arrangement.End,
			verticalAlignment = Alignment.CenterVertically
		) {
			leftTrains.forEach { TrainPill(it, alignEnd = true) }
		}

		// 레일: 위/아래 연결선 + 역 원.
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier
				.width(36.dp)
				.fillMaxSize()
		) {
			Box(
				modifier = Modifier
					.width(3.dp)
					.weight(1f)
					.background(if (isFirst) Color.Transparent else accent)
			)
			StationDot(
				color = accent,
				pulsing = station.trains.any { it.isMine },
				filled = station.trains.isNotEmpty()
			)
			Box(
				modifier = Modifier
					.width(3.dp)
					.weight(1f)
					.background(if (isLast) Color.Transparent else accent)
			)
		}

		Row(
			modifier = Modifier.weight(1f),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = station.name,
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = if (station.trains.isNotEmpty()) FontWeight.SemiBold
				else FontWeight.Normal,
				color = if (station.trains.isNotEmpty()) MaterialTheme.colorScheme.onSurface
				else MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(start = 6.dp, end = 4.dp)
			)
			rightTrains.forEach { TrainPill(it, alignEnd = false) }
		}
	}
}

@Composable
private fun Rail(station: SubwayStationUi, accent: Color) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.width(28.dp)
			.height(52.dp)
	) {
		Box(
			modifier = Modifier
				.width(3.dp)
				.weight(1f)
				.background(accent)
		)
		StationDot(
			color = accent,
			pulsing = station.trains.any { it.isMine },
			filled = station.trains.isNotEmpty()
		)
		Box(
			modifier = Modifier
				.width(3.dp)
				.weight(1f)
				.background(accent)
		)
	}
}

@Composable
private fun StationLabel(station: SubwayStationUi, accent: Color, alignEnd: Boolean) {
	Text(
		text = station.name,
		style = MaterialTheme.typography.labelMedium,
		fontWeight = if (station.trains.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
		color = if (station.trains.isNotEmpty()) MaterialTheme.colorScheme.onSurface
		else MaterialTheme.colorScheme.onSurfaceVariant,
		textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
		modifier = Modifier
			.width(78.dp)
			.padding(horizontal = 4.dp)
	)
}

@Composable
private fun TrainPillStack(
	station: SubwayStationUi,
	alignEnd: Boolean,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier,
		horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
		verticalAlignment = Alignment.CenterVertically
	) {
		station.trains.forEach { TrainPill(it, alignEnd = alignEnd) }
	}
}

/** 역 원. 내 열차가 있으면 은은한 펄스로 강조. */
@Composable
private fun StationDot(color: Color, pulsing: Boolean, filled: Boolean) {
	val scale = if (pulsing) {
		val transition = rememberInfiniteTransition(label = "dotPulse")
		transition.animateFloat(
			initialValue = 1f,
			targetValue = 1.35f,
			animationSpec = infiniteRepeatable(
				animation = tween(700),
				repeatMode = RepeatMode.Reverse
			),
			label = "dotScale"
		).value
	} else 1f

	Box(
		modifier = Modifier
			.size(if (filled) 14.dp else 10.dp)
			.scale(scale)
			.background(
				if (filled) color else MaterialTheme.colorScheme.surface,
				CircleShape
			)
			.border(3.dp, color, CircleShape)
	)
}

/** "2450 / 성수행" 열차 마커 + 진행 방향 삼각형. */
@Composable
private fun TrainPill(ui: SubwayTrainUi, alignEnd: Boolean) {
	val bg = when {
		ui.isMine -> MaterialTheme.colorScheme.primary
		ui.isPrevious -> Color(0xFFFF9800)
		else -> MaterialTheme.colorScheme.surfaceVariant
	}
	val fg = when {
		ui.isMine -> MaterialTheme.colorScheme.onPrimary
		ui.isPrevious -> Color.White
		else -> MaterialTheme.colorScheme.onSurfaceVariant
	}

	Row(verticalAlignment = Alignment.CenterVertically) {
		if (alignEnd) PillPointer(pointsRight = false, color = bg)
		Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
			Column(
				modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = ui.dto.trainNo ?: "-",
					style = MaterialTheme.typography.labelMedium,
					fontWeight = FontWeight.Bold,
					fontFamily = FontFamily.Monospace,
					color = fg
				)
				Text(
					text = "${ui.dto.statnTnm ?: ""}행",
					style = MaterialTheme.typography.labelSmall,
					color = fg
				)
			}
		}
		if (!alignEnd) PillPointer(pointsRight = true, color = bg)
	}
}

/** pill이 레일을 가리키는 작은 삼각형. */
@Composable
private fun PillPointer(pointsRight: Boolean, color: Color) {
	Canvas(modifier = Modifier.size(width = 7.dp, height = 12.dp)) {
		val path = Path().apply {
			if (pointsRight) {
				moveTo(0f, 0f)
				lineTo(size.width, size.height / 2f)
				lineTo(0f, size.height)
			} else {
				moveTo(size.width, 0f)
				lineTo(0f, size.height / 2f)
				lineTo(size.width, size.height)
			}
			close()
		}
		drawPath(path, color)
	}
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		contentAlignment = Alignment.Center
	) { content() }
}

/** 호선 표준 색상. */
private fun lineColor(line: Int): Color = when (line) {
	1 -> Color(0xFF0052A4)
	2 -> Color(0xFF00A84D)
	3 -> Color(0xFFEF7C1C)
	4 -> Color(0xFF00A5DE)
	5 -> Color(0xFF996CAC)
	6 -> Color(0xFFCD7C2F)
	7 -> Color(0xFF747F00)
	8 -> Color(0xFFE6186C)
	9 -> Color(0xFFBDB092)
	else -> Color(0xFF616161)
}
