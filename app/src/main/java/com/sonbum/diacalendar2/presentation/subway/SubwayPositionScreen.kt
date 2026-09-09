package com.sonbum.diacalendar2.presentation.subway

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubwayPositionScreen(
	myTrainNo: String,
	line: Int,
	officeName: String,
	onBack: () -> Unit,
	allTrainNos: String = "",
	modifier: Modifier = Modifier,
	viewModel: SubwayPositionViewModel = koinViewModel(),
) {
	val state by viewModel.state.collectAsStateWithLifecycle()

	LaunchedEffect(myTrainNo, line, officeName, allTrainNos) {
		viewModel.initialize(myTrainNo, line, officeName, allTrainNos)
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
				title = {
					val shown = state.runningTrainNos.ifEmpty { state.myTrainNos }
					Text(
						text = if (shown.size > 1) "${line}호선 ${shown.joinToString(" · ")}"
						else "${line}호선 ${shown.firstOrNull() ?: myTrainNo}열차",
						maxLines = 1
					)
				},
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

			// 내 열번이 지금 어디에 있는지(또는 왜 없는지)를 목록보다 먼저 알린다.
			if (state.myTrainNos.isNotEmpty() && !state.isLoading) {
				MyTrainSummary(
					myTrainNos = state.myTrainNos,
					runningTrainNos = state.runningTrainNos,
					stations = state.stations,
					accent = lineColor
				)
			}

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

/**
 * 내 근무 열번의 현재 위치 요약.
 * 운행 중이면 어느 역에 있는지 바로 보여주고, 아니면 그 사실을 명확히 알린다
 * (그렇지 않으면 "열차 제일 많은 탭"이 열려 무관한 화면으로 보인다).
 */
@Composable
private fun MyTrainSummary(
	myTrainNos: List<String>,
	runningTrainNos: List<String>,
	stations: List<SubwayStationUi>,
	accent: Color
) {
	// 열번 -> 현재 역명
	val whereByTrainNo = remember(stations) {
		buildMap {
			stations.forEach { st ->
				st.trains.filter { it.isMine }.forEach { t ->
					t.dto.trainNo?.let { no -> put(no, st.name) }
				}
			}
		}
	}

	Surface(
		color = if (runningTrainNos.isEmpty()) MaterialTheme.colorScheme.surfaceVariant
		else accent.copy(alpha = 0.12f),
		modifier = Modifier.fillMaxWidth()
	) {
		Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
			if (runningTrainNos.isEmpty()) {
				Text(
					text = "내 열번 ${myTrainNos.joinToString(" · ")} 은(는) 지금 운행 중이 아닙니다.",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Text(
					text = "아래는 현재 운행 중인 다른 열차입니다.",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			} else {
				runningTrainNos.forEach { no ->
					// matchKey(뒤 3자리)로 매칭되므로 표시용 실제 열번을 찾아 쓴다.
					val entry = whereByTrainNo.entries.firstOrNull { (apiNo, _) ->
						apiNo.takeLast(3) == no.takeLast(3)
					}
					Row(verticalAlignment = Alignment.CenterVertically) {
						Surface(color = accent, shape = RoundedCornerShape(5.dp)) {
							Text(
								text = entry?.key ?: no,
								style = MaterialTheme.typography.labelMedium,
								fontWeight = FontWeight.Bold,
								fontFamily = FontFamily.Monospace,
								color = Color.White,
								modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
							)
						}
						Text(
							text = entry?.value?.let { "  $it 부근" } ?: "  다른 방향/지선에서 운행 중",
							style = MaterialTheme.typography.bodySmall,
							fontWeight = FontWeight.SemiBold
						)
					}
				}
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
	// 첫 진입은 애니메이션 없이 즉시 내 열차 위치로 보낸다(긴 목록을 훑는 연출 방지).
	var landed by rememberSaveable { mutableStateOf(false) }

	// 탭 전환 또는 내 열차 위치 변동 시에만 스크롤. stations에 key를 걸면 30초마다 화면이 튄다.
	LaunchedEffect(selectedTabKey, myTrainIndex) {
		val target = myTrainIndex ?: return@LaunchedEffect
		val index = target.coerceAtLeast(0)
		if (landed) {
			listState.animateScrollToItem(index = index, scrollOffset = -240)
		} else {
			listState.scrollToItem(index = index, scrollOffset = -240)
			landed = true
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
 * 2호선 본선: 실제 노선도처럼 원형 고리로 그린다.
 *
 * 시청(index 0)을 정상단(12시)에 두고 시계 방향으로
 * 을지로입구 → 을지로3가 → … → 충정로 순서로 배치한다.
 * 43개 역을 각도로 나눠 원주에 올리므로 순환선의 "끝이 없는" 성질이 그대로 드러난다.
 */
@Composable
private fun SubwayLoopDiagram(
	stations: List<SubwayStationUi>,
	myTrainIndex: Int?,
	selectedTabKey: String?,
	accent: Color,
	modifier: Modifier = Modifier
) {
	if (stations.isEmpty()) return

	// 원주에 43개 라벨이 겹치지 않으려면 역당 최소 호(弧) 길이가 필요하다.
	// 지름 = (역 수 x 역당 호 길이) / PI 로 잡아 라벨 간격을 일정하게 유지한다.
	val diameter = ((stations.size * LOOP_ARC_PER_STATION.value / Math.PI).toFloat())
		.dp.coerceAtLeast(320.dp)
	val scrollState = rememberScrollState()
	val hScrollState = rememberScrollState()

	// 내 열차가 있으면 그 역이 화면에 오도록, 없으면 원 중앙이 보이도록 스크롤한다.
	var landed by rememberSaveable(selectedTabKey) { mutableStateOf(false) }
	LaunchedEffect(selectedTabKey, myTrainIndex, diameter, scrollState.maxValue) {
		if (scrollState.maxValue == 0) return@LaunchedEffect
		val (targetX, targetY) = if (myTrainIndex != null && stations.isNotEmpty()) {
			// 역의 원주 좌표를 스크롤 좌표로 환산(원 중심 = 스크롤 중앙).
			val rad = Math.toRadians((-90f + (360f / stations.size) * myTrainIndex).toDouble())
			(hScrollState.maxValue / 2 + (hScrollState.maxValue / 2 * cos(rad)).toInt()) to
				(scrollState.maxValue / 2 + (scrollState.maxValue / 2 * sin(rad)).toInt())
		} else {
			hScrollState.maxValue / 2 to scrollState.maxValue / 2
		}
		// 첫 진입은 즉시, 이후 갱신만 애니메이션.
		if (landed) {
			scrollState.animateScrollTo(targetY)
			hScrollState.animateScrollTo(targetX)
		} else {
			scrollState.scrollTo(targetY)
			hScrollState.scrollTo(targetX)
			landed = true
		}
	}

	Box(
		modifier = modifier
			.verticalScroll(scrollState)
			.horizontalScroll(hScrollState)
	) {
		Box(
			modifier = Modifier
				.size(diameter + LOOP_LABEL_MARGIN * 2)
				.padding(LOOP_LABEL_MARGIN)
		) {
			// 노선 원(레일).
			Canvas(modifier = Modifier.fillMaxSize()) {
				drawCircle(
					color = accent,
					radius = size.minDimension / 2f,
					style = Stroke(width = LOOP_RAIL_STROKE.toPx())
				)
			}

			// 각 역을 원주 위 각도로 배치. 시청(0)이 12시, 시계 방향으로 증가.
			stations.forEachIndexed { index, station ->
				val angleDeg = -90f + (360f / stations.size) * index
				val angleRad = Math.toRadians(angleDeg.toDouble())
				val radius = diameter / 2
				val dx = radius * cos(angleRad).toFloat()
				val dy = radius * sin(angleRad).toFloat()

				LoopStationNode(
					station = station,
					accent = accent,
					// 오른쪽 절반이면 라벨을 바깥(오른쪽), 왼쪽 절반이면 바깥(왼쪽)으로.
					labelOnRight = cos(angleRad) >= 0,
					modifier = Modifier
						.align(Alignment.Center)
						.offset(x = dx, y = dy)
				)
			}

			// 원 안쪽 가운데에 방향 안내.
			Column(
				modifier = Modifier.align(Alignment.Center),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = "2호선 순환",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Bold,
					color = accent
				)
				Text(
					text = "시청 기준 시계방향",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

/** 원주 위 역 하나: 역 점 + 역명 + (있으면) 열차 pill. */
@Composable
private fun LoopStationNode(
	station: SubwayStationUi,
	accent: Color,
	labelOnRight: Boolean,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier.width(LOOP_NODE_WIDTH),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = if (labelOnRight) Arrangement.Start else Arrangement.End
	) {
		if (labelOnRight) {
			StationDot(
				color = accent,
				pulsing = station.trains.any { it.isMine },
				filled = station.trains.isNotEmpty()
			)
			Spacer(Modifier.width(4.dp))
		}
		Column(
			horizontalAlignment = if (labelOnRight) Alignment.Start else Alignment.End
		) {
			Text(
				text = station.name,
				style = MaterialTheme.typography.labelMedium,
				fontWeight = if (station.trains.isNotEmpty()) FontWeight.Bold
				else FontWeight.Normal,
				color = if (station.trains.isNotEmpty()) MaterialTheme.colorScheme.onSurface
				else MaterialTheme.colorScheme.onSurfaceVariant,
				maxLines = 1
			)
			station.trains.forEach { ui ->
				TrainPill(ui = ui, alignEnd = !labelOnRight)
			}
		}
		if (!labelOnRight) {
			Spacer(Modifier.width(4.dp))
			StationDot(
				color = accent,
				pulsing = station.trains.any { it.isMine },
				filled = station.trains.isNotEmpty()
			)
		}
	}
}

private val LOOP_RAIL_STROKE = 4.dp
private val LOOP_LABEL_MARGIN = 96.dp
private val LOOP_NODE_WIDTH = 180.dp

/** 원주에서 역 하나가 차지할 호 길이. 라벨 겹침을 막는 기준. */
private val LOOP_ARC_PER_STATION = 58.dp


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
