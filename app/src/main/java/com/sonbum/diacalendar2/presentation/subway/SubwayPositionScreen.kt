package com.sonbum.diacalendar2.presentation.subway

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
 * 2호선 본선: TOPIS 열번위치 화면과 같은 세로 순환 고리(lozenge).
 *
 * 시청을 정상단 가운데 두고 고리를 반으로 갈라 두 열로 내린다.
 *   왼쪽 열(위→아래)  충정로 → 아현 → … → 사당 → 방배
 *   오른쪽 열(위→아래) 을지로입구 → 을지로3가 → … → 삼성 → 선릉
 * 두 열은 상단(시청)과 하단(교대·강남·역삼)에서 이어져 순환을 이룬다.
 * 열차 pill은 각 열의 바깥쪽에 붙어 안쪽 역명과 겹치지 않는다.
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

	val layout = remember(stations) { buildLoopLayout(stations) }
	val scrollState = rememberScrollState()
	var landed by rememberSaveable(selectedTabKey) { mutableStateOf(false) }

	// 내 열차가 있는 행이 화면에 오도록 스크롤. 두 열은 같은 높이를 공유한다.
	LaunchedEffect(selectedTabKey, myTrainIndex, scrollState.maxValue) {
		if (scrollState.maxValue == 0) return@LaunchedEffect
		val row = layout.rowOf(myTrainIndex)
		val rows = maxOf(layout.left.size, layout.right.size).coerceAtLeast(1)
		val target = if (row == null) 0
		else (scrollState.maxValue * row / rows - 200).coerceIn(0, scrollState.maxValue)

		if (landed) {
			scrollState.animateScrollTo(target)
		} else {
			scrollState.scrollTo(target)
			landed = true
		}
	}

	Column(
		modifier = modifier
			.verticalScroll(scrollState)
			.padding(horizontal = 8.dp, vertical = 16.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		// ── 상단 캡: 시청(정상단 가운데) ──
		LoopCapRow(station = layout.top, accent = accent, isTop = true)

		// ── 두 열 본체 ──
		val rows = maxOf(layout.left.size, layout.right.size)
		repeat(rows) { row ->
			Row(
				modifier = Modifier.height(LOOP_ROW_HEIGHT),
				verticalAlignment = Alignment.CenterVertically
			) {
				LoopSideStation(
					station = layout.left.getOrNull(row),
					accent = accent,
					isLeft = true,
					modifier = Modifier.weight(1f)
				)
				Spacer(Modifier.width(LOOP_INNER_GAP))
				LoopSideStation(
					station = layout.right.getOrNull(row),
					accent = accent,
					isLeft = false,
					modifier = Modifier.weight(1f)
				)
			}
		}

		// ── 하단 캡: 두 열을 잇는 역(교대·강남·역삼 등) ──
		layout.bottom.forEach { station ->
			LoopCapRow(station = station, accent = accent, isTop = false)
		}
	}
}

/**
 * 고리를 화면 배치용으로 나눈 결과.
 * top = 시청(정상단), left/right = 양 열(위→아래), bottom = 하단 연결부.
 */
internal data class LoopLayout(
	val top: SubwayStationUi,
	val left: List<SubwayStationUi>,
	val right: List<SubwayStationUi>,
	val bottom: List<SubwayStationUi>,
	/** 원본 인덱스 -> 화면 행 번호. 자동 스크롤에 쓴다. */
	val rowByIndex: Map<Int, Int>
) {
	fun rowOf(index: Int?): Int? = index?.let { rowByIndex[it] }
}

/**
 * 시청(0번)을 정상단에 두고 고리를 반으로 갈라 두 열로 만든다.
 * 오른쪽 열은 진행 방향(1,2,3…), 왼쪽 열은 역방향(마지막,그 앞…)으로 내려간다.
 * 가운데 남는 역들이 하단 연결부가 된다.
 */
internal fun buildLoopLayout(stations: List<SubwayStationUi>): LoopLayout {
	val n = stations.size
	val top = stations.first()
	if (n == 1) {
		return LoopLayout(top, emptyList(), emptyList(), emptyList(), mapOf(0 to 0))
	}

	// 하단 연결부에 둘 역 수. 두 열 길이를 같게 맞추고 남은 만큼을 하단에 준다.
	val bottomCount = if ((n - 1) % 2 == 0) 1 else 2
	val sideCount = ((n - 1 - bottomCount) / 2).coerceAtLeast(0)

	val right = (1..sideCount).map { stations[it] }
	val left = (1..sideCount).map { stations[n - it] }
	// 가운데 남은 구간 = 하단. 화면에는 좌→우로 그린다.
	val bottom = ((sideCount + 1) until (n - sideCount)).map { stations[it] }.reversed()

	val rowByIndex = buildMap {
		put(0, 0)
		for (row in 1..sideCount) {
			put(row, row)          // 오른쪽 열: 원본 index == row
			put(n - row, row)      // 왼쪽 열
		}
		for (i in (sideCount + 1) until (n - sideCount)) put(i, sideCount + 1)
	}

	return LoopLayout(top, left, right, bottom, rowByIndex)
}

/** 고리 상단/하단의 가운데 정렬 역(시청, 교대·강남·역삼 등). */
@Composable
private fun LoopCapRow(station: SubwayStationUi, accent: Color, isTop: Boolean) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier.height(LOOP_ROW_HEIGHT)
	) {
		if (!isTop) {
			Box(
				modifier = Modifier
					.width(LOOP_RAIL_STROKE)
					.weight(1f)
					.background(accent)
			)
		}
		Row(verticalAlignment = Alignment.CenterVertically) {
			station.trains.forEach { TrainPill(ui = it, alignEnd = true) }
			Spacer(Modifier.width(4.dp))
			StationDot(
				color = accent,
				pulsing = station.trains.any { it.isMine },
				filled = station.trains.isNotEmpty()
			)
			Spacer(Modifier.width(4.dp))
			Text(
				text = station.name,
				style = MaterialTheme.typography.labelMedium,
				fontWeight = if (station.trains.isNotEmpty()) FontWeight.Bold
				else FontWeight.SemiBold,
				color = MaterialTheme.colorScheme.onSurface,
				maxLines = 1
			)
		}
		if (isTop) {
			Box(
				modifier = Modifier
					.width(LOOP_RAIL_STROKE)
					.weight(1f)
					.background(accent)
			)
		}
	}
}

/**
 * 한 열의 역 한 칸.
 * 레일은 고리 안쪽, 역명은 레일 옆, 열차 pill은 바깥쪽에 둔다
 * (스크린샷에서 "8452 성수행"이 노선 바깥에 붙는 배치와 같다).
 */
@Composable
private fun LoopSideStation(
	station: SubwayStationUi?,
	accent: Color,
	isLeft: Boolean,
	modifier: Modifier = Modifier
) {
	if (station == null) {
		Box(modifier = modifier.height(LOOP_ROW_HEIGHT))
		return
	}
	Row(
		modifier = modifier,
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = if (isLeft) Arrangement.End else Arrangement.Start
	) {
		if (isLeft) {
			LoopTrainPillGroup(station, alignEnd = true)
			Spacer(Modifier.width(6.dp))
			LoopStationName(station, alignEnd = true)
			Spacer(Modifier.width(6.dp))
			LoopRail(station, accent)
		} else {
			LoopRail(station, accent)
			Spacer(Modifier.width(6.dp))
			LoopStationName(station, alignEnd = false)
			Spacer(Modifier.width(6.dp))
			LoopTrainPillGroup(station, alignEnd = false)
		}
	}
}

/** 세로 레일 + 역 점. */
@Composable
private fun LoopRail(station: SubwayStationUi, accent: Color) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier.height(LOOP_ROW_HEIGHT)
	) {
		Box(
			modifier = Modifier
				.width(LOOP_RAIL_STROKE)
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
				.width(LOOP_RAIL_STROKE)
				.weight(1f)
				.background(accent)
		)
	}
}

/** 레일 옆 역명. 열차가 있으면 굵게(스크린샷의 진한 역명과 동일). */
@Composable
private fun LoopStationName(station: SubwayStationUi, alignEnd: Boolean) {
	Text(
		text = station.name,
		style = MaterialTheme.typography.labelMedium,
		fontWeight = if (station.trains.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
		color = if (station.trains.isNotEmpty()) MaterialTheme.colorScheme.onSurface
		else MaterialTheme.colorScheme.onSurfaceVariant,
		textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
		maxLines = 1,
		modifier = Modifier.width(LOOP_NAME_WIDTH)
	)
}

/** 노선 바깥쪽 열차 pill 묶음. */
@Composable
private fun LoopTrainPillGroup(station: SubwayStationUi, alignEnd: Boolean) {
	Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
		station.trains.forEach { ui -> TrainPill(ui = ui, alignEnd = alignEnd) }
	}
}

private val LOOP_RAIL_STROKE = 4.dp
private val LOOP_ROW_HEIGHT = 46.dp
private val LOOP_NAME_WIDTH = 84.dp
private val LOOP_INNER_GAP = 8.dp

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
