package com.sonbum.diacalendar2.presentation.subway

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.first
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.min
import kotlin.math.roundToInt

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

	val activeLine = state.line.takeIf { it in 1..9 }
		?: line.takeIf { it in 1..9 }
		?: DEFAULT_SUBWAY_LINE
	val diagramLine = state.tabs
		.firstOrNull { it.key == state.selectedTabKey }
		?.sourceLine
		?: activeLine
	val selectedTab = state.tabs.firstOrNull { it.key == state.selectedTabKey }
	val isSeongsuBranch = selectedTab?.sourceLine == 2 && selectedTab.segmentId == "seongsu"
	val emphasizedStationNames = when {
		activeLine == 1 && selectedTab?.sourceLine == 1 && selectedTab.segmentId == "main" -> setOf("신설동")
		isSeongsuBranch -> setOf("신답")
		activeLine == 2 && selectedTab?.sourceLine == 2 && selectedTab.segmentId == "main" ->
			setOf("동대문역사문화공원", "신도림", "대림")
		activeLine == 2 && selectedTab?.sourceLine == 2 && selectedTab.segmentId == "sinjeong" ->
			setOf("양천구청")
		activeLine == 4 && selectedTab?.sourceLine == 4 && selectedTab.segmentId == "main" ->
			setOf("상계", "동작")
		activeLine == 8 && selectedTab?.sourceLine == 8 && selectedTab.segmentId == "main" ->
			setOf("잠실")
		else -> emptySet()
	}
	val useZigzagDiagram = shouldUseZigzagDiagram(
		line = selectedTab?.sourceLine ?: activeLine,
		segmentId = selectedTab?.segmentId,
		isLoop = state.isLoop
	)
	val zigzagStationsPerRow = if ("신설동" in emphasizedStationNames) 7
	else ZIGZAG_STATIONS_PER_ROW
	val lineColor = lineColor(diagramLine)

	Scaffold(
		modifier = modifier,
		topBar = {
			TopAppBar(
				title = {
					val shown = state.runningTrainNos.firstOrNull()
						?: state.previousTrainNo
						?: state.myTrainNo.ifBlank { myTrainNo }.takeIf { it.isNotBlank() }
					Text(
						text = shown?.let { "${it}열차" } ?: "열차 위치",
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
						SubwayLineSelector(
							selectedLine = activeLine,
							enabled = state.line in 1..9,
							onLineSelected = viewModel::selectLine
						)
						Text(
							text = "${state.secondsUntilRefresh}초 후 갱신",
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
						IconButton(onClick = viewModel::refresh) {
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
				accent = lineColor,
				myTrainNos = state.myTrainNos,
				runningTrainNos = state.runningTrainNos,
				previousTrainNo = state.previousTrainNo,
				stations = state.stations,
				limitedToCrewChange = activeLine == 1 && selectedTab?.sourceLine == 1,
				isLoading = state.isLoading
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
									onClick = viewModel::refresh,
								modifier = Modifier.padding(top = 12.dp)
							) { Text("다시 시도") }
						}
					}

				state.stations.isEmpty() ->
					CenterBox {
						Column(horizontalAlignment = Alignment.CenterHorizontally) {
							Text(
								"${activeLine}호선에 지금 운행 중인 열차가 없습니다.",
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

				else ->
					ZoomableSubwayDiagram(
						diagramKey = "$activeLine:${state.selectedTabKey}",
						initialFocusFraction = if (activeLine == 1 || isSeongsuBranch) null
						else state.myTrainIndex?.let { trainIndex ->
							when {
								state.isLoop -> loopStationFocusFraction(
									stationCount = state.stations.size,
									stationIndex = trainIndex
								)
								useZigzagDiagram -> zigzagStationFocusFraction(
									stationCount = state.stations.size,
									stationIndex = trainIndex
								)
								else -> Offset(0.5f, 0.5f)
							}
						},
						modifier = Modifier
							.weight(1f)
							.fillMaxWidth()
					) { isZoomed ->
						if (state.isLoop) {
							SubwayLoopDiagram(
								stations = state.stations,
								emphasizedStationNames = emphasizedStationNames,
								accent = lineColor,
								modifier = Modifier.fillMaxSize()
							)
						} else if (useZigzagDiagram) {
							SubwayZigzagDiagram(
								stations = state.stations,
								stationsPerRow = zigzagStationsPerRow,
								emphasizedStationNames = emphasizedStationNames,
								accent = lineColor,
								modifier = Modifier.fillMaxSize()
							)
						} else {
							SubwayLinearDiagram(
								stations = state.stations,
								myTrainIndex = state.myTrainIndex,
								selectedTabKey = state.selectedTabKey,
								segmentId = selectedTab?.segmentId,
								emphasizedStationNames = emphasizedStationNames,
								accent = lineColor,
								userScrollEnabled = !isZoomed,
								modifier = Modifier.fillMaxSize()
							)
						}
					}
			}
		}
	}
}

@Composable
private fun SubwayLineSelector(
	selectedLine: Int,
	enabled: Boolean,
	onLineSelected: (Int) -> Unit
) {
	var expanded by rememberSaveable { mutableStateOf(false) }

	Box {
		TextButton(
			onClick = { expanded = true },
			enabled = enabled
		) {
			Text(
				text = "${selectedLine}호선",
				fontWeight = FontWeight.SemiBold
			)
			Icon(
				imageVector = Icons.Filled.ArrowDropDown,
				contentDescription = "호선 선택"
			)
		}
		DropdownMenu(
			expanded = expanded,
			onDismissRequest = { expanded = false }
		) {
			(1..9).forEach { line ->
				DropdownMenuItem(
					text = {
						Text(
							text = "${line}호선",
							fontWeight = if (line == selectedLine) {
								FontWeight.Bold
							} else {
								FontWeight.Normal
							}
						)
					},
					onClick = {
						expanded = false
						onLineSelected(line)
					}
				)
			}
		}
	}
}

/**
 * 두 번 탭하면 탭한 지점을 기준으로 2배 확대하고, 확대 상태에서는 한 손가락으로 이동한다.
 * 다시 두 번 탭하면 배율과 이동 위치를 함께 초기화한다.
 */
@Composable
private fun ZoomableSubwayDiagram(
	modifier: Modifier = Modifier,
	diagramKey: String? = null,
	initialFocusFraction: Offset? = null,
	content: @Composable (isZoomed: Boolean) -> Unit
) {
	val scale = remember(diagramKey) {
		mutableFloatStateOf(
			if (initialFocusFraction == null) SUBWAY_DIAGRAM_OVERVIEW_SCALE
			else SUBWAY_DIAGRAM_ZOOM
		)
	}
	val offset = remember(diagramKey) { mutableStateOf(Offset.Zero) }
	val viewportSize = remember { mutableStateOf(IntSize.Zero) }
	var initialFocusApplied by remember(diagramKey) { mutableStateOf(false) }
	val isZoomed = scale.floatValue > SUBWAY_DIAGRAM_OVERVIEW_SCALE

	LaunchedEffect(diagramKey, initialFocusFraction, viewportSize.value) {
		val focus = initialFocusFraction ?: return@LaunchedEffect
		val size = viewportSize.value
		if (initialFocusApplied || size.width <= 0 || size.height <= 0) return@LaunchedEffect
		val focusPoint = Offset(
			x = size.width * focus.x.coerceIn(0f, 1f),
			y = size.height * focus.y.coerceIn(0f, 1f)
		)
		scale.floatValue = SUBWAY_DIAGRAM_ZOOM
		offset.value = zoomOffsetForPoint(
			point = focusPoint,
			viewportSize = size,
			effectiveScale = diagramEffectiveScale(SUBWAY_DIAGRAM_ZOOM)
		)
		initialFocusApplied = true
	}

	BoxWithConstraints(
		modifier = modifier
			.clipToBounds()
			.onSizeChanged { viewportSize.value = it }
			.pointerInput(Unit) {
				detectTapGestures(
					onDoubleTap = { tapPosition ->
						if (scale.floatValue > SUBWAY_DIAGRAM_OVERVIEW_SCALE) {
							scale.floatValue = SUBWAY_DIAGRAM_OVERVIEW_SCALE
							offset.value = Offset.Zero
						} else {
							scale.floatValue = SUBWAY_DIAGRAM_ZOOM
							val effectiveZoom = diagramEffectiveScale(SUBWAY_DIAGRAM_ZOOM)
							offset.value = zoomOffsetForPoint(
								point = tapPosition,
								viewportSize = viewportSize.value,
								effectiveScale = effectiveZoom
							)
						}
					}
				)
			}
			.pointerInput(Unit) {
				// 확대 전부터 감지기를 유지해 확대 직후 첫 드래그가 누락되지 않게 한다.
				// 기본 touch slop보다 작은 임계값만 두어 두 번 탭의 미세한 손떨림은 구분한다.
				val dragThreshold = 2.dp.toPx()
				awaitEachGesture {
					val down = awaitFirstDown(requireUnconsumed = false)
					val zoomedAtStart = scale.floatValue > SUBWAY_DIAGRAM_OVERVIEW_SCALE
					var accumulatedDrag = Offset.Zero
					var dragging = false

					while (true) {
						val event = awaitPointerEvent()
						val change = event.changes.firstOrNull { it.id == down.id } ?: break
						if (!change.pressed) break

						val movement = change.positionChange()
						if (!zoomedAtStart || movement == Offset.Zero) continue
						accumulatedDrag += movement
						if (!dragging && accumulatedDrag.getDistance() >= dragThreshold) {
							dragging = true
						}
						if (dragging) {
							change.consume()
							offset.value = constrainZoomOffset(
								offset = offset.value + movement,
								viewportSize = viewportSize.value,
								scale = diagramEffectiveScale(scale.floatValue)
							)
						}
					}
				}
			},
		contentAlignment = Alignment.Center
	) {
		// 축소 배율의 역수만큼 실제 배치 영역을 넓혀 LazyColumn도 더 많은 역을 구성한다.
		// 그래픽만 작게 그리면 목록에 보이는 역 수는 그대로이므로 두 값을 함께 조정해야 한다.
		Box(
			modifier = Modifier
				.requiredSize(
					width = maxWidth / SUBWAY_DIAGRAM_OVERVIEW_SCALE,
					height = maxHeight / SUBWAY_DIAGRAM_OVERVIEW_SCALE
				)
				.graphicsLayer {
					scaleX = scale.floatValue
					scaleY = scale.floatValue
					translationX = offset.value.x
					translationY = offset.value.y
				}
		) {
			content(isZoomed)
		}
	}
}

internal const val SUBWAY_DIAGRAM_OVERVIEW_SCALE = 0.8f
internal const val SUBWAY_DIAGRAM_ZOOM = 1.6f

internal fun diagramEffectiveScale(renderScale: Float): Float =
	renderScale / SUBWAY_DIAGRAM_OVERVIEW_SCALE

internal fun zoomOffsetForPoint(
	point: Offset,
	viewportSize: IntSize,
	effectiveScale: Float
): Offset {
	val center = Offset(
		x = viewportSize.width / 2f,
		y = viewportSize.height / 2f
	)
	return constrainZoomOffset(
		offset = (center - point) * (effectiveScale - 1f),
		viewportSize = viewportSize,
		scale = effectiveScale
	)
}

internal fun constrainZoomOffset(
	offset: Offset,
	viewportSize: IntSize,
	scale: Float
): Offset {
	val maxX = viewportSize.width * (scale - 1f).coerceAtLeast(0f) / 2f
	val maxY = viewportSize.height * (scale - 1f).coerceAtLeast(0f) / 2f
	if (maxX == 0f && maxY == 0f) return Offset.Zero
	return Offset(
		x = if (maxX == 0f) 0f else offset.x.coerceIn(-maxX, maxX),
		y = if (maxY == 0f) 0f else offset.y.coerceIn(-maxY, maxY)
	)
}

/** 수신시각·운행 대수와 내 열번 안내를 한 줄 영역에 함께 표시한다. */
@Composable
private fun SubwayHeader(
	baseTime: String,
	count: Int,
	accent: Color,
	myTrainNos: List<String>,
	runningTrainNos: List<String>,
	previousTrainNo: String?,
	stations: List<SubwayStationUi>,
	limitedToCrewChange: Boolean,
	isLoading: Boolean
) {
	// matchKey(뒤 3자리)로 매칭되는 API 열번과 현재 역을 표시용으로 찾는다.
	val whereByTrainNo = remember(stations) {
		buildMap {
			stations.forEach { station ->
				station.trains.filter { it.isMine || it.isPrevious }.forEach { train ->
					train.dto.trainNo?.let { no -> put(no, station.name) }
				}
			}
		}
	}
	val statusText = when {
		isLoading -> "실시간 열차 위치를 불러오는 중입니다."
		myTrainNos.isEmpty() -> "내 열번 정보가 없습니다."
		previousTrainNo != null -> {
			val entry = whereByTrainNo.entries.firstOrNull { (apiNo, _) ->
				apiNo.takeLast(3) == previousTrainNo.takeLast(3)
			}
			val position = entry?.let { (apiNo, station) -> "$apiNo $station 부근" }
				?: previousTrainNo
			"내 열번 ${myTrainNos.first()} 운행 전 · 전 열번 $position"
		}
		runningTrainNos.isEmpty() ->
			"내 열번은 지금 운행 중이 아닙니다.\n아래는 현재 운행 중인 다른 열차입니다."
		else -> {
			val positions = runningTrainNos.map { no ->
				val entry = whereByTrainNo.entries.firstOrNull { (apiNo, _) ->
					apiNo.takeLast(3) == no.takeLast(3)
				}
				entry?.let { (apiNo, station) -> "$apiNo $station 부근" }
					?: if (limitedToCrewChange) "$no 신설동 앞뒤 10역 밖 또는 다른 방향에서 운행 중"
					else "$no 다른 방향/지선에서 운행 중"
			}
			"내 열번 ${positions.joinToString(" · ")}"
		}
	}

	Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
		Row(
			modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
				text = statusText,
				style = MaterialTheme.typography.labelSmall,
				fontWeight = if (runningTrainNos.isNotEmpty() || previousTrainNo != null)
					FontWeight.SemiBold
				else FontWeight.Normal,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.weight(1f)
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
	segmentId: String?,
	emphasizedStationNames: Set<String>,
	accent: Color,
	userScrollEnabled: Boolean,
	modifier: Modifier = Modifier
) {
	// 화면이 처음 배치되기 전부터 내 열차 행을 시작 위치로 지정한다.
	// 이후 중앙 정렬 애니메이션은 짧은 보정만 수행하므로 첫 프레임부터 내 열차가 보인다.
	val listState = rememberLazyListState(
		initialFirstVisibleItemIndex = initialTrainListIndex(myTrainIndex, stations.size)
	)

	// 최초 진입부터 이동을 눈으로 확인할 수 있게 애니메이션하고, 고정 픽셀 대신
	// 실제 화면 높이를 기준으로 내 열차 행을 중앙에 둔다.
	LaunchedEffect(selectedTabKey, myTrainIndex, stations.size) {
		val target = myTrainIndex ?: return@LaunchedEffect
		if (stations.isEmpty()) return@LaunchedEffect
		val index = target.coerceIn(0, stations.lastIndex)
		val layoutInfo = snapshotFlow { listState.layoutInfo }
			.first { info ->
				info.viewportSize.height > 0 &&
					info.totalItemsCount > index &&
					info.visibleItemsInfo.isNotEmpty()
			}
		val itemHeight = layoutInfo.visibleItemsInfo.first().size
		listState.animateScrollToItem(
			index = index,
			scrollOffset = centeredTrainScrollOffset(
				viewportHeightPx = layoutInfo.viewportSize.height,
				itemHeightPx = itemHeight
			)
		)
	}

	LazyColumn(
		state = listState,
		modifier = modifier,
		userScrollEnabled = userScrollEnabled
	) {
		itemsIndexed(stations, key = { _, s -> s.statnId }) { index, station ->
			StationRailRow(
				station = station,
				isFirst = index == 0,
				isLast = index == stations.lastIndex,
				segmentId = segmentId,
				isCrewChange = station.name in emphasizedStationNames,
				accent = accent
			)
		}
	}
}

internal fun centeredTrainScrollOffset(viewportHeightPx: Int, itemHeightPx: Int): Int =
	-((viewportHeightPx - itemHeightPx).coerceAtLeast(0) / 2)

internal fun initialTrainListIndex(myTrainIndex: Int?, stationCount: Int): Int =
	if (stationCount <= 0) 0 else (myTrainIndex ?: 0).coerceIn(0, stationCount - 1)

internal const val ZIGZAG_STATIONS_PER_ROW = 5

internal fun shouldUseZigzagDiagram(
	line: Int,
	segmentId: String?,
	isLoop: Boolean
): Boolean = line != 2 && segmentId == "main" && !isLoop

internal data class ZigzagNode(
	val stationIndex: Int,
	val row: Int,
	val column: Int
)

internal data class ZigzagLayout(
	val columnCount: Int,
	val rowCount: Int,
	val nodes: List<ZigzagNode>
) {
	fun nodeOf(stationIndex: Int): ZigzagNode? = nodes.getOrNull(stationIndex)
}

/**
 * 긴 본선을 왼쪽→오른쪽, 오른쪽→왼쪽으로 번갈아 접는다.
 * 배열 순서는 운행 순서를 그대로 유지하고 화면의 열 위치만 홀수 행에서 뒤집는다.
 */
internal fun buildZigzagLayout(
	stationCount: Int,
	stationsPerRow: Int = ZIGZAG_STATIONS_PER_ROW
): ZigzagLayout {
	if (stationCount <= 0) return ZigzagLayout(0, 0, emptyList())

	val columnCount = min(stationsPerRow.coerceAtLeast(2), stationCount)
	val rowCount = (stationCount + columnCount - 1) / columnCount
	val nodes = List(stationCount) { stationIndex ->
		val row = stationIndex / columnCount
		val offsetInRow = stationIndex % columnCount
		val column = if (row % 2 == 0) {
			offsetInRow
		} else {
			columnCount - 1 - offsetInRow
		}
		ZigzagNode(stationIndex = stationIndex, row = row, column = column)
	}
	return ZigzagLayout(columnCount, rowCount, nodes)
}

/** 내 열차 역을 지그재그 노선도의 0..1 확대 중심 좌표로 환산한다. */
internal fun zigzagStationFocusFraction(
	stationCount: Int,
	stationIndex: Int,
	stationsPerRow: Int = ZIGZAG_STATIONS_PER_ROW
): Offset? {
	val layout = buildZigzagLayout(stationCount, stationsPerRow)
	val node = layout.nodeOf(stationIndex) ?: return null
	if (layout.rowCount == 0) return null

	val x = if (layout.columnCount == 1) {
		0.5f
	} else {
		ZIGZAG_SIDE_FRACTION +
			(1f - ZIGZAG_SIDE_FRACTION * 2f) * node.column / (layout.columnCount - 1f)
	}
	return Offset(x = x, y = (node.row + 0.5f) / layout.rowCount)
}

private const val ZIGZAG_SIDE_FRACTION = 0.08f

/** 화면에 놓인 선로를 기준으로 한 열차 선두의 실제 진행 방향. */
internal enum class TrainHeading {
	LEFT,
	RIGHT,
	UP,
	DOWN;

	fun reversed(): TrainHeading = when (this) {
		LEFT -> RIGHT
		RIGHT -> LEFT
		UP -> DOWN
		DOWN -> UP
	}
}

/** 일반 노선은 0=상행(역 배열 감소), 1=하행(역 배열 증가)이다. */
internal fun zigzagTrainHeading(
	stationIndex: Int,
	stationCount: Int,
	updnLine: String?,
	stationsPerRow: Int = ZIGZAG_STATIONS_PER_ROW
): TrainHeading {
	val columns = min(stationsPerRow.coerceAtLeast(2), stationCount.coerceAtLeast(1))
	val row = stationIndex / columns
	val offsetInRow = stationIndex % columns
	val movesTowardLargerIndex = updnLine == "1"
	if (movesTowardLargerIndex && stationIndex < stationCount - 1 && offsetInRow == columns - 1) {
		return TrainHeading.DOWN
	}
	if (!movesTowardLargerIndex && stationIndex > 0 && offsetInRow == 0) {
		return TrainHeading.UP
	}
	val largerIndexHeading = if (row % 2 == 0) TrainHeading.RIGHT else TrainHeading.LEFT
	return if (movesTowardLargerIndex) largerIndexHeading else largerIndexHeading.reversed()
}

/** 2호선은 0=내선(역 배열 증가), 1=외선(역 배열 감소)이다. */
internal fun line2TrainHeading(
	increasingHeading: TrainHeading,
	updnLine: String?
): TrainHeading = if (updnLine == "0") increasingHeading else increasingHeading.reversed()

/** 성수지선은 화면 위 신설동에서 아래 성수로 향하는 열차가 방향 코드 0이다. */
internal fun linearTrainHeading(segmentId: String?, updnLine: String?): TrainHeading = when {
	segmentId == "seongsu" && updnLine == "0" -> TrainHeading.DOWN
	segmentId == "seongsu" && updnLine == "1" -> TrainHeading.UP
	updnLine == "0" -> TrainHeading.UP
	else -> TrainHeading.DOWN
}

/**
 * 1·3~9호선 본선: 첨부된 열번위치 화면처럼 긴 노선을 여러 가로 행으로 접은
 * 지그재그 노선도. 각 지선은 기존 세로 직선 표시를 유지한다.
 */
@Composable
private fun SubwayZigzagDiagram(
	stations: List<SubwayStationUi>,
	stationsPerRow: Int,
	emphasizedStationNames: Set<String>,
	accent: Color,
	modifier: Modifier = Modifier
) {
	if (stations.isEmpty()) return

	val layout = remember(stations.size, stationsPerRow) {
		buildZigzagLayout(stations.size, stationsPerRow)
	}
	val textMeasurer = rememberTextMeasurer()
	val surface = MaterialTheme.colorScheme.surface
	val onSurface = MaterialTheme.colorScheme.onSurface
	val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
	val mineColor = MaterialTheme.colorScheme.primary
	val mineContentColor = MaterialTheme.colorScheme.onPrimary
	val accessibilityDescription = remember(stations, emphasizedStationNames) {
		val positions = stations.flatMap { station ->
			station.trains.map { train -> "${train.dto.trainNo ?: "열차"} ${station.name}" }
		}
		val changeStation = if (emphasizedStationNames.isEmpty()) ""
		else "강조된 역 ${emphasizedStationNames.joinToString(", ")}. "
		if (positions.isEmpty()) {
			"지그재그 노선도. ${changeStation}현재 표시할 열차가 없습니다."
		} else {
			"지그재그 노선도. ${changeStation}현재 열차 위치: ${positions.joinToString(", ")}"
		}
	}
	val transition = rememberInfiniteTransition(label = "zigzagTrainPulse")
	val minePulse by transition.animateFloat(
		initialValue = 1f,
		targetValue = 1.42f,
		animationSpec = infiniteRepeatable(
			animation = tween(700),
			repeatMode = RepeatMode.Reverse
		),
		label = "zigzagTrainPulseScale"
	)

	Canvas(
		modifier = modifier
			.padding(horizontal = 3.dp, vertical = 2.dp)
			.semantics { contentDescription = accessibilityDescription }
	) {
		val rowHeight = size.height / layout.rowCount.coerceAtLeast(1)
		val markerWidth = min(45.dp.toPx(), size.width * 0.12f).coerceAtLeast(36.dp.toPx())
		val markerHeight = min(26.dp.toPx(), rowHeight * 0.43f).coerceAtLeast(20.dp.toPx())
		val sidePadding = maxOf(markerWidth / 2f + 2.dp.toPx(), 20.dp.toPx())
		val railWidth = min(4.dp.toPx(), rowHeight * 0.09f)
		val dotRadius = min(5.5.dp.toPx(), rowHeight * 0.11f)
		val availableRailWidth = (size.width - sidePadding * 2f).coerceAtLeast(1f)
		val columnGap = if (layout.columnCount <= 1) 0f
		else availableRailWidth / (layout.columnCount - 1)

		fun centerOf(node: ZigzagNode): Offset = Offset(
			x = if (layout.columnCount == 1) size.width / 2f
			else sidePadding + columnGap * node.column,
			y = rowHeight * (node.row + 0.5f)
		)

		val railPath = Path()
		layout.nodes.forEachIndexed { index, node ->
			val center = centerOf(node)
			if (index == 0) railPath.moveTo(center.x, center.y)
			else railPath.lineTo(center.x, center.y)
		}
		drawPath(
			path = railPath,
			color = accent,
			style = Stroke(
				width = railWidth,
				cap = StrokeCap.Round,
				join = StrokeJoin.Round
			)
		)

		layout.nodes.forEach { node ->
			val station = stations[node.stationIndex]
			val isCrewChange = station.name in emphasizedStationNames
			val center = centerOf(node)
			val orderedTrains = station.trains.sortedWith(
				compareByDescending<SubwayTrainUi> { it.isMine }
					.thenByDescending { it.isPrevious }
			)
			val shownTrain = orderedTrains.firstOrNull()

			if (shownTrain != null) {
				val heading = zigzagTrainHeading(
					stationIndex = node.stationIndex,
					stationCount = stations.size,
					updnLine = shownTrain.dto.updnLine,
					stationsPerRow = stationsPerRow
				)
				val markerCenterY = when (heading) {
					TrainHeading.UP -> center.y - dotRadius - 3.dp.toPx() - markerWidth / 2f
					TrainHeading.DOWN -> center.y + dotRadius + 3.dp.toPx() + markerWidth / 2f
					TrainHeading.LEFT, TrainHeading.RIGHT ->
						center.y - dotRadius - 3.dp.toPx() - markerHeight / 2f
				}
				val markerLeft = (center.x - markerWidth / 2f)
					.coerceIn(1.dp.toPx(), size.width - markerWidth - 1.dp.toPx())
				val markerTop = markerCenterY - markerHeight / 2f
				val background = when {
					shownTrain.isMine -> mineColor
					shownTrain.isPrevious -> Color(0xFFFF9800)
					else -> accent
				}
				val contentColor = if (shownTrain.isMine) mineContentColor else Color.White

				drawLine(
					color = background.copy(alpha = 0.7f),
					start = Offset(
						x = center.x,
						y = if (heading == TrainHeading.DOWN) center.y + dotRadius
						else center.y - dotRadius
					),
					end = Offset(
						x = center.x,
						y = when (heading) {
							TrainHeading.UP -> markerCenterY + markerWidth / 2f
							TrainHeading.DOWN -> markerCenterY - markerWidth / 2f
							TrainHeading.LEFT, TrainHeading.RIGHT -> markerTop + markerHeight
						}
					),
					strokeWidth = 1.5.dp.toPx()
				)
				drawTrainVehicleMarker(
					textMeasurer = textMeasurer,
					ui = shownTrain,
					topLeft = Offset(markerLeft, markerTop),
					size = Size(markerWidth, markerHeight),
					heading = heading,
					background = background,
					contentColor = contentColor,
					trainTextSize = 9.sp,
					destinationTextSize = 4.5.sp,
					additionalTrainCount = orderedTrains.size - 1
				)
			}

			if (station.trains.any { it.isMine }) {
				drawCircle(
					color = mineColor.copy(alpha = 0.18f),
					radius = dotRadius * minePulse,
					center = center
				)
			}
			drawCircle(color = surface, radius = dotRadius, center = center)
			drawCircle(
				color = accent,
				radius = dotRadius,
				center = center,
				style = Stroke(width = min(2.dp.toPx(), dotRadius * 0.42f))
			)
			if (station.trains.isNotEmpty()) {
				drawCircle(color = accent, radius = dotRadius * 0.34f, center = center)
			}

			val maxLabelWidth = if (layout.columnCount <= 1) size.width
			else (columnGap - 3.dp.toPx()).coerceAtLeast(1f)
			val badgePaddingX = if (isCrewChange) 5.dp.toPx() else 0f
			val labelLayout = textMeasurer.measure(
				text = station.name,
				style = TextStyle(
					fontSize = if (rowHeight >= 52.dp.toPx()) 9.sp else 8.sp,
					fontWeight = if (isCrewChange) FontWeight.ExtraBold
					else if (station.trains.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
					color = if (isCrewChange || station.trains.isNotEmpty()) onSurface
					else onSurfaceVariant
				),
				maxLines = 1,
				softWrap = false,
				overflow = TextOverflow.Ellipsis,
				constraints = Constraints(
					maxWidth = (maxLabelWidth - badgePaddingX * 2f).roundToInt().coerceAtLeast(1)
				)
			)
			val labelX = (center.x - labelLayout.size.width / 2f)
				.coerceIn(0f, (size.width - labelLayout.size.width).coerceAtLeast(0f))
			val labelY = center.y + dotRadius + 2.dp.toPx()
			if (isCrewChange) {
				val badgeTopLeft = Offset(labelX - badgePaddingX, labelY - 2.dp.toPx())
				val badgeSize = Size(labelLayout.size.width + badgePaddingX * 2f, labelLayout.size.height + 4.dp.toPx())
				drawRoundRect(
					color = surface,
					topLeft = badgeTopLeft,
					size = badgeSize,
					cornerRadius = CornerRadius(5.dp.toPx())
				)
				drawRoundRect(
					color = accent,
					topLeft = badgeTopLeft,
					size = badgeSize,
					cornerRadius = CornerRadius(5.dp.toPx()),
					style = Stroke(width = 1.5.dp.toPx())
				)
			}
			drawText(
				textLayoutResult = labelLayout,
				topLeft = Offset(labelX, labelY)
			)
		}
	}
}

/**
 * 2호선 본선: TOPIS 열번위치 화면처럼 43개 역을 한 화면에 담는 순환 고리.
 *
 * 고정 행 높이와 세로 스크롤을 쓰지 않고, 현재 화면에서 실제로 남은 높이를
 * 22개 행(상단 1 + 양옆 20 + 하단 1)으로 나눈다. 작은 기기나 큰 글꼴에서도
 * 노선 전체가 잘리지 않으며 열차 표식도 같은 비율로 함께 작아진다.
 */
@Composable
private fun SubwayLoopDiagram(
	stations: List<SubwayStationUi>,
	emphasizedStationNames: Set<String>,
	accent: Color,
	modifier: Modifier = Modifier
) {
	if (stations.isEmpty()) return

	val layout = remember(stations) { buildLoopLayout(stations) }
	val textMeasurer = rememberTextMeasurer()
	val surface = MaterialTheme.colorScheme.surface
	val onSurface = MaterialTheme.colorScheme.onSurface
	val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
	val mineColor = MaterialTheme.colorScheme.primary
	val mineContentColor = MaterialTheme.colorScheme.onPrimary
	val accessibilityDescription = remember(stations, emphasizedStationNames) {
		val positions = stations.flatMap { station ->
			station.trains.map { train -> "${train.dto.trainNo ?: "열차"} ${station.name}" }
		}
		val highlighted = if (emphasizedStationNames.isEmpty()) ""
		else "강조된 역 ${emphasizedStationNames.joinToString(", ")}. "
		if (positions.isEmpty()) {
			"2호선 순환 노선도. ${highlighted}현재 표시할 열차가 없습니다."
		} else {
			"2호선 순환 노선도. ${highlighted}현재 열차 위치: ${positions.joinToString(", ")}"
		}
	}
	val transition = rememberInfiniteTransition(label = "loopTrainPulse")
	val minePulse by transition.animateFloat(
		initialValue = 1f,
		targetValue = 1.42f,
		animationSpec = infiniteRepeatable(
			animation = tween(700),
			repeatMode = RepeatMode.Reverse
		),
		label = "loopTrainPulseScale"
	)

	BoxWithConstraints(
		modifier = modifier.padding(horizontal = 4.dp, vertical = 3.dp)
	) {
		val sideRows = maxOf(layout.left.size, layout.right.size).coerceAtLeast(1)
		val totalRows = sideRows + 2 // 상단 캡 + 양옆 + 하단 캡
		val rowHeight = maxHeight / totalRows
		val stationTextSize = when {
			rowHeight >= 28.dp -> 11.sp
			rowHeight >= 23.dp -> 9.sp
			else -> 8.sp
		}
		val trainTextSize = when {
			rowHeight >= 28.dp -> 11.sp
			rowHeight >= 23.dp -> 10.sp
			else -> 9.sp
		}
		val destinationTextSize = when {
			rowHeight >= 28.dp -> 5.sp
			rowHeight >= 23.dp -> 4.5.sp
			else -> 4.sp
		}

		Canvas(
			modifier = Modifier
				.fillMaxSize()
				.semantics { contentDescription = accessibilityDescription }
		) {
			val rowPx = size.height / totalRows
			val topY = rowPx / 2f
			val firstSideY = rowPx * 1.5f
			val lastSideY = rowPx * (sideRows + 0.5f)
			val bottomY = rowPx * (sideRows + 1.5f)
			val centerX = size.width / 2f
			// 참고 이미지와 비슷하게 레일 사이에는 역명, 바깥에는 열차 표식을 둔다.
			val leftRailX = size.width * LOOP_LEFT_RAIL_FRACTION
			val rightRailX = size.width * LOOP_RIGHT_RAIL_FRACTION
			val trackWidth = rightRailX - leftRailX
			val railWidth = min(4.dp.toPx(), rowPx * 0.18f)
			val dotRadius = min(6.dp.toPx(), rowPx * 0.27f)
			val labelGap = 3.dp.toPx()
			val innerLabelWidth = (trackWidth / 2f - dotRadius - labelGap - 1.dp.toPx())
				.coerceAtLeast(1f)

			// 위·아래를 둥글게 연결한 하나의 닫힌 순환선.
			val loopPath = Path().apply {
				moveTo(centerX, topY)
				cubicTo(
					centerX - trackWidth * 0.28f, topY,
					leftRailX, topY + rowPx * 0.42f,
					leftRailX, firstSideY
				)
				lineTo(leftRailX, lastSideY)
				cubicTo(
					leftRailX, bottomY - rowPx * 0.35f,
					leftRailX + trackWidth * 0.18f, bottomY,
					leftRailX + trackWidth * 0.28f, bottomY
				)
				lineTo(rightRailX - trackWidth * 0.28f, bottomY)
				cubicTo(
					rightRailX - trackWidth * 0.18f, bottomY,
					rightRailX, bottomY - rowPx * 0.35f,
					rightRailX, lastSideY
				)
				lineTo(rightRailX, firstSideY)
				cubicTo(
					rightRailX, topY + rowPx * 0.42f,
					centerX + trackWidth * 0.28f, topY,
					centerX, topY
				)
			}
			drawPath(loopPath, color = accent, style = Stroke(width = railWidth))

			drawLoopNode(
				textMeasurer = textMeasurer,
				station = layout.top,
				isEmphasized = layout.top.name in emphasizedStationNames,
				center = Offset(centerX, topY),
				labelOnRight = true,
				markerOnRight = false,
				increasingHeading = TrainHeading.RIGHT,
				labelMaxWidth = innerLabelWidth,
				rowHeight = rowPx,
				dotRadius = dotRadius,
				accent = accent,
				surface = surface,
				onSurface = onSurface,
				onSurfaceVariant = onSurfaceVariant,
				mineColor = mineColor,
				mineContentColor = mineContentColor,
				minePulse = minePulse,
				stationTextSize = stationTextSize,
				trainTextSize = trainTextSize,
				destinationTextSize = destinationTextSize
			)

			for (row in 0 until sideRows) {
				val y = rowPx * (row + 1.5f)
				layout.left.getOrNull(row)?.let { station ->
					drawLoopNode(
						textMeasurer, station, Offset(leftRailX, y),
						isEmphasized = station.name in emphasizedStationNames,
						labelOnRight = true, markerOnRight = false,
						increasingHeading = TrainHeading.UP,
						labelMaxWidth = innerLabelWidth, rowHeight = rowPx,
						dotRadius = dotRadius, accent = accent, surface = surface,
						onSurface = onSurface, onSurfaceVariant = onSurfaceVariant,
						mineColor = mineColor, mineContentColor = mineContentColor,
						minePulse = minePulse, stationTextSize = stationTextSize,
						trainTextSize = trainTextSize,
						destinationTextSize = destinationTextSize
					)
				}
				layout.right.getOrNull(row)?.let { station ->
					drawLoopNode(
						textMeasurer, station, Offset(rightRailX, y),
						isEmphasized = station.name in emphasizedStationNames,
						labelOnRight = false, markerOnRight = true,
						increasingHeading = TrainHeading.DOWN,
						labelMaxWidth = innerLabelWidth, rowHeight = rowPx,
						dotRadius = dotRadius, accent = accent, surface = surface,
						onSurface = onSurface, onSurfaceVariant = onSurfaceVariant,
						mineColor = mineColor, mineContentColor = mineContentColor,
						minePulse = minePulse, stationTextSize = stationTextSize,
						trainTextSize = trainTextSize,
						destinationTextSize = destinationTextSize
					)
				}
			}

			// 교대·강남처럼 가운데 남은 역을 하단 곡선 위에 고르게 배치한다.
			layout.bottom.forEachIndexed { index, station ->
				val fraction = (index + 1f) / (layout.bottom.size + 1f)
				val x = leftRailX + trackWidth * fraction
				val isLeftHalf = x <= centerX
				drawLoopNode(
					textMeasurer, station, Offset(x, bottomY),
					isEmphasized = station.name in emphasizedStationNames,
					labelOnRight = isLeftHalf, labelAbove = true,
					markerOnRight = !isLeftHalf,
					increasingHeading = TrainHeading.LEFT,
					labelMaxWidth = innerLabelWidth, rowHeight = rowPx,
					dotRadius = dotRadius, accent = accent, surface = surface,
					onSurface = onSurface, onSurfaceVariant = onSurfaceVariant,
					mineColor = mineColor, mineContentColor = mineContentColor,
					minePulse = minePulse, stationTextSize = stationTextSize,
					trainTextSize = trainTextSize,
					destinationTextSize = destinationTextSize
				)
			}
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

/** 2호선 순환 노선도에서 역 인덱스를 화면상의 0..1 좌표로 환산한다. */
internal fun loopStationFocusFraction(stationCount: Int, stationIndex: Int): Offset? {
	if (stationCount <= 0 || stationIndex !in 0 until stationCount) return null
	val bottomHint = if ((stationCount - 1) % 2 == 0) 1 else 2
	val sideCount = ((stationCount - 1 - bottomHint) / 2).coerceAtLeast(0)
	val actualBottomCount = stationCount - 1 - sideCount * 2
	val totalRows = sideCount + 2f

	return when {
		stationIndex == 0 -> Offset(0.5f, 0.5f / totalRows)
		stationIndex <= sideCount -> Offset(
			x = LOOP_RIGHT_RAIL_FRACTION,
			y = (stationIndex + 0.5f) / totalRows
		)
		stationIndex >= stationCount - sideCount -> {
			val row = stationCount - stationIndex
			Offset(
				x = LOOP_LEFT_RAIL_FRACTION,
				y = (row + 0.5f) / totalRows
			)
		}
		else -> {
			val lastBottomIndex = stationCount - sideCount - 1
			val visualIndex = lastBottomIndex - stationIndex
			val fraction = (visualIndex + 1f) / (actualBottomCount + 1f)
			Offset(
				x = LOOP_LEFT_RAIL_FRACTION +
					(LOOP_RIGHT_RAIL_FRACTION - LOOP_LEFT_RAIL_FRACTION) * fraction,
				y = (sideCount + 1.5f) / totalRows
			)
		}
	}
}

private const val LOOP_LEFT_RAIL_FRACTION = 0.34f
private const val LOOP_RIGHT_RAIL_FRACTION = 0.66f

/** Canvas 위에 역명·역 점·열차 표식을 한 묶음으로 그린다. */
private fun DrawScope.drawLoopNode(
	textMeasurer: TextMeasurer,
	station: SubwayStationUi,
	center: Offset,
	isEmphasized: Boolean,
	labelOnRight: Boolean,
	labelAbove: Boolean = false,
	markerOnRight: Boolean,
	increasingHeading: TrainHeading,
	labelMaxWidth: Float,
	rowHeight: Float,
	dotRadius: Float,
	accent: Color,
	surface: Color,
	onSurface: Color,
	onSurfaceVariant: Color,
	mineColor: Color,
	mineContentColor: Color,
	minePulse: Float,
	stationTextSize: TextUnit,
	trainTextSize: TextUnit,
	destinationTextSize: TextUnit
) {
	drawLoopTrainMarkers(
		textMeasurer = textMeasurer,
		trains = station.trains,
		stationCenter = center,
		markerOnRight = markerOnRight,
		increasingHeading = increasingHeading,
		rowHeight = rowHeight,
		dotRadius = dotRadius,
		accent = accent,
		mineColor = mineColor,
		mineContentColor = mineContentColor,
		trainTextSize = trainTextSize,
		destinationTextSize = destinationTextSize
	)

	val hasMine = station.trains.any { it.isMine }
	if (hasMine) {
		drawCircle(
			color = mineColor.copy(alpha = 0.18f),
			radius = dotRadius * minePulse,
			center = center
		)
	}
	drawCircle(color = surface, radius = dotRadius, center = center)
	drawCircle(
		color = accent,
		radius = dotRadius,
		center = center,
		style = Stroke(width = min(2.2.dp.toPx(), dotRadius * 0.42f))
	)
	if (station.trains.isNotEmpty()) {
		drawCircle(color = accent, radius = dotRadius * 0.34f, center = center)
	}

	val label = when {
		isEmphasized && station.name == "동대문역사문화공원" -> "동대문역사\n문화공원"
		isEmphasized -> station.name
		else -> compactLoopStationName(station.name)
	}
	val labelFontSize = if (isEmphasized && station.name == "동대문역사문화공원") {
		min(stationTextSize.value, 9f).sp
	} else stationTextSize
	val badgePaddingX = if (isEmphasized) 4.dp.toPx() else 0f
	val badgePaddingY = if (isEmphasized) 2.dp.toPx() else 0f
	val labelStyle = TextStyle(
		fontSize = labelFontSize,
		lineHeight = if (isEmphasized) labelFontSize else TextUnit.Unspecified,
		fontWeight = if (isEmphasized) FontWeight.ExtraBold
		else if (station.trains.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
		color = if (isEmphasized || station.trains.isNotEmpty()) onSurface else onSurfaceVariant
	)
	val labelLayout = textMeasurer.measure(
		text = label,
		style = labelStyle,
		maxLines = if (isEmphasized) 2 else 1,
		softWrap = isEmphasized,
		overflow = TextOverflow.Ellipsis,
		constraints = Constraints(
			maxWidth = (labelMaxWidth - badgePaddingX * 2f).roundToInt().coerceAtLeast(1)
		)
	)
	val gap = 3.dp.toPx()
	val badgeWidth = labelLayout.size.width + badgePaddingX * 2f
	val badgeHeight = labelLayout.size.height + badgePaddingY * 2f
	val badgeX = if (labelAbove) {
		center.x - badgeWidth / 2f
	} else if (labelOnRight) {
		center.x + dotRadius + gap
	} else {
		center.x - dotRadius - gap - badgeWidth
	}
	val badgeY = if (labelAbove) {
		center.y - dotRadius - gap - badgeHeight
	} else {
		center.y - badgeHeight / 2f
	}
	if (isEmphasized) {
		val badgeTopLeft = Offset(badgeX, badgeY)
		val badgeSize = Size(badgeWidth, badgeHeight)
		val cornerRadius = CornerRadius(5.dp.toPx())
		drawRoundRect(
			color = surface,
			topLeft = badgeTopLeft,
			size = badgeSize,
			cornerRadius = cornerRadius
		)
		drawRoundRect(
			color = accent,
			topLeft = badgeTopLeft,
			size = badgeSize,
			cornerRadius = cornerRadius,
			style = Stroke(width = 1.5.dp.toPx())
		)
	}
	drawText(
		textLayoutResult = labelLayout,
		topLeft = Offset(badgeX + badgePaddingX, badgeY + badgePaddingY)
	)
}

/**
 * 2호선 열차 표식은 레일 바깥쪽에 최대 두 대까지 나란히 둔다.
 * 열번이 눕지 않도록 열차 본체는 항상 가로로 그리고, 진행 방향만 삼각형으로 표시한다.
 */
private fun DrawScope.drawLoopTrainMarkers(
	textMeasurer: TextMeasurer,
	trains: List<SubwayTrainUi>,
	stationCenter: Offset,
	markerOnRight: Boolean,
	increasingHeading: TrainHeading,
	rowHeight: Float,
	dotRadius: Float,
	accent: Color,
	mineColor: Color,
	mineContentColor: Color,
	trainTextSize: TextUnit,
	destinationTextSize: TextUnit
) {
	if (trains.isEmpty()) return

	val ordered = trains.sortedWith(
		compareByDescending<SubwayTrainUi> { it.isMine }
			.thenByDescending { it.isPrevious }
	)
	val shown = ordered.take(2)
	val pillHeight = min(27.dp.toPx(), rowHeight * 0.84f)
	val pillWidth = min(45.dp.toPx(), rowHeight * 1.82f)
	val gap = 2.dp.toPx()

	shown.forEachIndexed { index, ui ->
		val pillX = if (markerOnRight) {
			stationCenter.x + dotRadius + gap + index * (pillWidth + gap)
		} else {
			stationCenter.x - dotRadius - gap - pillWidth - index * (pillWidth + gap)
		}
		val pillY = stationCenter.y - pillHeight / 2f
		val heading = line2TrainHeading(increasingHeading, ui.dto.updnLine)
		val background = when {
			ui.isMine -> mineColor
			ui.isPrevious -> Color(0xFFFF9800)
			else -> accent
		}
		val contentColor = if (ui.isMine) mineContentColor else Color.White

		drawLine(
			color = background.copy(alpha = 0.7f),
			start = stationCenter,
			end = Offset(
				x = if (markerOnRight) pillX else pillX + pillWidth,
				y = stationCenter.y
			),
			strokeWidth = 1.5.dp.toPx()
		)
		drawLine2HorizontalTrainMarker(
			textMeasurer = textMeasurer,
			ui = ui,
			topLeft = Offset(pillX, pillY),
			size = Size(pillWidth, pillHeight),
			heading = heading,
			background = background,
			contentColor = contentColor,
			trainTextSize = trainTextSize,
			destinationTextSize = destinationTextSize,
			additionalTrainCount = if (index == shown.lastIndex) ordered.size - shown.size else 0
		)
	}
}

/**
 * 첨부된 2호선 열차 표식처럼 가로형 본체 바깥에 진행 방향 삼각형을 붙인다.
 * 세로 선로에서도 본체와 열번은 회전하지 않으므로 위아래 역의 표식과 겹치지 않는다.
 */
private fun DrawScope.drawLine2HorizontalTrainMarker(
	textMeasurer: TextMeasurer,
	ui: SubwayTrainUi,
	topLeft: Offset,
	size: Size,
	heading: TrainHeading,
	background: Color,
	contentColor: Color,
	trainTextSize: TextUnit,
	destinationTextSize: TextUnit,
	additionalTrainCount: Int = 0
) {
	val left = topLeft.x
	val top = topLeft.y
	val right = left + size.width
	val bottom = top + size.height
	val centerX = left + size.width / 2f
	val centerY = top + size.height / 2f
	val pointerDepth = min(4.dp.toPx(), size.height * 0.16f)
	val pointerHalfBase = min(6.dp.toPx(), size.height * 0.24f)
	val pointerPath = Path().apply {
		when (heading) {
			TrainHeading.UP -> {
				moveTo(centerX, top - pointerDepth)
				lineTo(centerX - pointerHalfBase, top)
				lineTo(centerX + pointerHalfBase, top)
			}
			TrainHeading.DOWN -> {
				moveTo(centerX, bottom + pointerDepth)
				lineTo(centerX - pointerHalfBase, bottom)
				lineTo(centerX + pointerHalfBase, bottom)
			}
			TrainHeading.LEFT -> {
				moveTo(left - pointerDepth, centerY)
				lineTo(left, centerY - pointerHalfBase)
				lineTo(left, centerY + pointerHalfBase)
			}
			TrainHeading.RIGHT -> {
				moveTo(right + pointerDepth, centerY)
				lineTo(right, centerY - pointerHalfBase)
				lineTo(right, centerY + pointerHalfBase)
			}
		}
		close()
	}
	drawPath(pointerPath, color = background)
	drawRoundRect(
		color = background,
		topLeft = topLeft,
		size = size,
		cornerRadius = CornerRadius(min(6.dp.toPx(), size.height * 0.24f))
	)
	drawRoundRect(
		color = Color.Black.copy(alpha = 0.22f),
		topLeft = topLeft,
		size = size,
		cornerRadius = CornerRadius(min(6.dp.toPx(), size.height * 0.24f)),
		style = Stroke(width = min(1.dp.toPx(), size.height * 0.05f))
	)

	val textWidth = (size.width - 4.dp.toPx()).roundToInt().coerceAtLeast(1)
	val numberLayout = textMeasurer.measure(
		text = ui.dto.trainNo ?: "-",
		style = TextStyle(
			fontSize = trainTextSize,
			lineHeight = trainTextSize,
			fontWeight = FontWeight.ExtraBold,
			fontFamily = FontFamily.Monospace,
			color = contentColor
		),
		maxLines = 1,
		softWrap = false,
		overflow = TextOverflow.Clip,
		constraints = Constraints(maxWidth = textWidth)
	)
	val destination = if (additionalTrainCount > 0) {
		"+$additionalTrainCount 대"
	} else {
		ui.dto.statnTnm
			?.takeIf { it.isNotBlank() }
			?.let { if (it.endsWith("행")) it else "${it}행" }
	}
	val destinationLayout = destination?.let {
		textMeasurer.measure(
			text = it,
			style = TextStyle(
				fontSize = destinationTextSize,
				lineHeight = destinationTextSize,
				color = contentColor
			),
			maxLines = 1,
			softWrap = false,
			overflow = TextOverflow.Clip,
			constraints = Constraints(maxWidth = textWidth)
		)
	}
	val visibleDestination = destinationLayout?.takeIf {
		numberLayout.size.height + it.size.height <= size.height - 2.dp.toPx()
	}
	val contentHeight = numberLayout.size.height + (visibleDestination?.size?.height ?: 0)
	var textY = top + (size.height - contentHeight) / 2f
	drawText(
		textLayoutResult = numberLayout,
		topLeft = Offset(centerX - numberLayout.size.width / 2f, textY)
	)
	if (visibleDestination != null) {
		textY += numberLayout.size.height
		drawText(
			textLayoutResult = visibleDestination,
			topLeft = Offset(centerX - visibleDestination.size.width / 2f, textY)
		)
	}
}

/**
 * 열차 번호를 전동차 옆모습 안에 그린다. 진행 방향 쪽 운전실, 반대편 노란 띠와
 * 두 바퀴를 분리해 작은 크기에서도 일반 말풍선과 구분되게 한다.
 */
private fun DrawScope.drawTrainVehicleMarker(
	textMeasurer: TextMeasurer,
	ui: SubwayTrainUi,
	topLeft: Offset,
	size: Size,
	heading: TrainHeading,
	background: Color,
	contentColor: Color,
	trainTextSize: TextUnit,
	destinationTextSize: TextUnit,
	additionalTrainCount: Int = 0
) {
	// 세로 구간은 가로형 열차 전체(열번·운전실·후미 띠·바퀴)를 통째로 회전한다.
	// 오른쪽 선두를 기준으로 -90°는 상행, +90°는 하행이 된다.
	if (heading == TrainHeading.UP || heading == TrainHeading.DOWN) {
		val center = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f)
		rotate(
			degrees = if (heading == TrainHeading.UP) -90f else 90f,
			pivot = center
		) {
			drawTrainVehicleMarker(
				textMeasurer = textMeasurer,
				ui = ui,
				topLeft = topLeft,
				size = size,
				heading = TrainHeading.RIGHT,
				background = background,
				contentColor = contentColor,
				trainTextSize = trainTextSize,
				destinationTextSize = destinationTextSize,
				additionalTrainCount = additionalTrainCount
			)
		}
		return
	}

	val left = topLeft.x
	val top = topLeft.y
	val right = left + size.width
	val bodyBottom = top + size.height * 0.82f
	val bodyHeight = bodyBottom - top
	val noseDepthX = size.width * 0.16f
	val noseDepthY = bodyHeight * 0.20f
	val rearCorner = bodyHeight * 0.14f
	val trainPath = Path().apply {
		when (heading) {
			TrainHeading.RIGHT -> {
			moveTo(left + rearCorner, top)
			lineTo(right - noseDepthX, top)
			quadraticTo(right, top + bodyHeight * 0.16f, right, top + bodyHeight * 0.42f)
			lineTo(right, top + bodyHeight * 0.72f)
			quadraticTo(right, bodyBottom, right - noseDepthX, bodyBottom)
			lineTo(left + rearCorner, bodyBottom)
			quadraticTo(left, bodyBottom, left, bodyBottom - rearCorner)
			lineTo(left, top + rearCorner)
			quadraticTo(left, top, left + rearCorner, top)
			}
			TrainHeading.LEFT -> {
			moveTo(right - rearCorner, top)
			lineTo(left + noseDepthX, top)
			quadraticTo(left, top + bodyHeight * 0.16f, left, top + bodyHeight * 0.42f)
			lineTo(left, top + bodyHeight * 0.72f)
			quadraticTo(left, bodyBottom, left + noseDepthX, bodyBottom)
			lineTo(right - rearCorner, bodyBottom)
			quadraticTo(right, bodyBottom, right, bodyBottom - rearCorner)
			lineTo(right, top + rearCorner)
			quadraticTo(right, top, right - rearCorner, top)
			}
			TrainHeading.UP -> {
				moveTo(left + rearCorner, bodyBottom)
				quadraticTo(left, bodyBottom, left, bodyBottom - rearCorner)
				lineTo(left, top + noseDepthY)
				quadraticTo(left, top, left + size.width * 0.30f, top)
				lineTo(right - size.width * 0.30f, top)
				quadraticTo(right, top, right, top + noseDepthY)
				lineTo(right, bodyBottom - rearCorner)
				quadraticTo(right, bodyBottom, right - rearCorner, bodyBottom)
			}
			TrainHeading.DOWN -> {
				moveTo(left + rearCorner, top)
				quadraticTo(left, top, left, top + rearCorner)
				lineTo(left, bodyBottom - noseDepthY)
				quadraticTo(left, bodyBottom, left + size.width * 0.30f, bodyBottom)
				lineTo(right - size.width * 0.30f, bodyBottom)
				quadraticTo(right, bodyBottom, right, bodyBottom - noseDepthY)
				lineTo(right, top + rearCorner)
				quadraticTo(right, top, right - rearCorner, top)
			}
		}
		close()
	}
	drawPath(path = trainPath, color = background)
	drawPath(
		path = trainPath,
		color = Color.Black.copy(alpha = 0.25f),
		style = Stroke(width = min(1.dp.toPx(), bodyHeight * 0.05f))
	)

	val cabTopLeft: Offset
	val cabSize: Size
	when (heading) {
		TrainHeading.RIGHT -> {
			cabTopLeft = Offset(right - noseDepthX * 0.82f, top + bodyHeight * 0.18f)
			cabSize = Size(noseDepthX * 0.55f, bodyHeight * 0.32f)
		}
		TrainHeading.LEFT -> {
			cabTopLeft = Offset(left + noseDepthX * 0.27f, top + bodyHeight * 0.18f)
			cabSize = Size(noseDepthX * 0.55f, bodyHeight * 0.32f)
		}
		TrainHeading.UP -> {
			cabTopLeft = Offset(left + size.width * 0.34f, top + bodyHeight * 0.08f)
			cabSize = Size(size.width * 0.32f, bodyHeight * 0.16f)
		}
		TrainHeading.DOWN -> {
			cabTopLeft = Offset(left + size.width * 0.34f, bodyBottom - bodyHeight * 0.24f)
			cabSize = Size(size.width * 0.32f, bodyHeight * 0.16f)
		}
	}
	drawRoundRect(
		color = Color.White.copy(alpha = 0.5f),
		topLeft = cabTopLeft,
		size = cabSize,
		cornerRadius = CornerRadius(bodyHeight * 0.06f)
	)
	val stripeWidth = min(3.dp.toPx(), size.width * 0.07f)
	val stripeTopLeft: Offset
	val stripeSize: Size
	when (heading) {
		TrainHeading.RIGHT -> {
			stripeTopLeft = Offset(left + size.width * 0.05f, top + bodyHeight * 0.2f)
			stripeSize = Size(stripeWidth, bodyHeight * 0.58f)
		}
		TrainHeading.LEFT -> {
			stripeTopLeft = Offset(right - size.width * 0.05f - stripeWidth, top + bodyHeight * 0.2f)
			stripeSize = Size(stripeWidth, bodyHeight * 0.58f)
		}
		TrainHeading.UP -> {
			stripeTopLeft = Offset(left + size.width * 0.20f, bodyBottom - stripeWidth - 1.dp.toPx())
			stripeSize = Size(size.width * 0.60f, stripeWidth)
		}
		TrainHeading.DOWN -> {
			stripeTopLeft = Offset(left + size.width * 0.20f, top + 1.dp.toPx())
			stripeSize = Size(size.width * 0.60f, stripeWidth)
		}
	}
	drawRoundRect(
		color = Color(0xFFFFD600),
		topLeft = stripeTopLeft,
		size = stripeSize,
		cornerRadius = CornerRadius(stripeWidth / 2f)
	)

	val wheelRadius = min(2.dp.toPx(), size.height * 0.085f)
	val wheelY = bodyBottom + wheelRadius * 0.15f
	listOf(left + size.width * 0.27f, left + size.width * 0.72f).forEach { wheelX ->
		drawCircle(Color(0xFF263238), radius = wheelRadius, center = Offset(wheelX, wheelY))
		drawCircle(
			Color.White.copy(alpha = 0.55f),
			radius = wheelRadius * 0.38f,
			center = Offset(wheelX, wheelY)
		)
	}

	val textWidth = (size.width * 0.68f).roundToInt().coerceAtLeast(1)
	val numberLayout = textMeasurer.measure(
		text = ui.dto.trainNo ?: "-",
		style = TextStyle(
			fontSize = trainTextSize,
			lineHeight = trainTextSize,
			fontWeight = FontWeight.ExtraBold,
			fontFamily = FontFamily.Monospace,
			color = contentColor
		),
		maxLines = 1,
		softWrap = false,
		overflow = TextOverflow.Clip,
		constraints = Constraints(maxWidth = textWidth)
	)
	val destination = if (additionalTrainCount > 0) {
		"+$additionalTrainCount 대"
	} else {
		ui.dto.statnTnm
			?.takeIf { it.isNotBlank() }
			?.let { if (it.endsWith("행")) it else "${it}행" }
	}
	val destinationLayout = destination?.let {
		textMeasurer.measure(
			text = it,
			style = TextStyle(
				fontSize = destinationTextSize,
				lineHeight = destinationTextSize,
				color = contentColor
			),
			maxLines = 1,
			softWrap = false,
			overflow = TextOverflow.Clip,
			constraints = Constraints(maxWidth = textWidth)
		)
	}
	val visibleDestination = destinationLayout?.takeIf {
		numberLayout.size.height + it.size.height <= bodyHeight - 1.dp.toPx()
	}
	val contentHeight = numberLayout.size.height + (visibleDestination?.size?.height ?: 0)
	val contentCenterX = left + size.width * when (heading) {
		TrainHeading.RIGHT -> 0.46f
		TrainHeading.LEFT -> 0.54f
		TrainHeading.UP, TrainHeading.DOWN -> 0.50f
	}
	var textY = top + (bodyHeight - contentHeight) / 2f
	drawText(
		textLayoutResult = numberLayout,
		topLeft = Offset(contentCenterX - numberLayout.size.width / 2f, textY)
	)
	if (visibleDestination != null) {
		textY += numberLayout.size.height
		drawText(
			textLayoutResult = visibleDestination,
			topLeft = Offset(contentCenterX - visibleDestination.size.width / 2f, textY)
		)
	}
}

/** 좁은 고리 안쪽에서 잘리지 않도록 참고 화면과 같은 짧은 표기를 사용한다. */
internal fun compactLoopStationName(name: String): String = when (name) {
	"동대문역사문화공원" -> "동대문역사"
	"구로디지털단지" -> "구로디지털"
	else -> name
}

/** 세로 한 줄 노선도의 한 행: [좌 pill] [레일] [역명 + 우 pill]. */
@Composable
private fun StationRailRow(
	station: SubwayStationUi,
	isFirst: Boolean,
	isLast: Boolean,
	segmentId: String?,
	isCrewChange: Boolean,
	accent: Color
) {
	val leftTrains = station.trains.filter { it.dto.updnLine == "1" }
	val rightTrains = station.trains.filter { it.dto.updnLine != "1" }

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(if (station.trains.isEmpty()) 56.dp else 76.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Row(
			modifier = Modifier.weight(1f),
			horizontalArrangement = Arrangement.End,
			verticalAlignment = Alignment.CenterVertically
		) {
			leftTrains.forEach { TrainPill(it, alignEnd = true, accent = accent, segmentId = segmentId) }
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
				fontWeight = if (isCrewChange) FontWeight.ExtraBold
				else if (station.trains.isNotEmpty()) FontWeight.SemiBold
				else FontWeight.Normal,
				color = if (isCrewChange || station.trains.isNotEmpty()) MaterialTheme.colorScheme.onSurface
				else MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier
					.padding(start = 6.dp, end = 4.dp)
					.then(
						if (isCrewChange) Modifier
							.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
							.border(1.5.dp, accent, RoundedCornerShape(8.dp))
							.padding(horizontal = 9.dp, vertical = 5.dp)
						else Modifier
					)
			)
			rightTrains.forEach { TrainPill(it, alignEnd = false, accent = accent, segmentId = segmentId) }
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
	accent: Color,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier,
		horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
		verticalAlignment = Alignment.CenterVertically
	) {
		station.trains.forEach { TrainPill(it, alignEnd = alignEnd, accent = accent) }
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

/** 지선 세로 노선도에서 사용하는 열차 모양 위치 마커. */
@Composable
private fun TrainPill(
	ui: SubwayTrainUi,
	alignEnd: Boolean,
	accent: Color,
	segmentId: String? = null
) {
	val bg = when {
		ui.isMine -> MaterialTheme.colorScheme.primary
		ui.isPrevious -> Color(0xFFFF9800)
		else -> accent
	}
	val fg = if (ui.isMine) MaterialTheme.colorScheme.onPrimary else Color.White
	val textMeasurer = rememberTextMeasurer()
	val destination = ui.dto.statnTnm
		?.takeIf { it.isNotBlank() }
		?.let { if (it.endsWith("행")) it else "${it}행" }
	val description = listOfNotNull(ui.dto.trainNo, destination).joinToString(" ")

	Canvas(
		modifier = Modifier
			.size(width = 58.dp, height = 38.dp)
			.semantics { contentDescription = description }
	) {
		drawTrainVehicleMarker(
			textMeasurer = textMeasurer,
			ui = ui,
			topLeft = Offset.Zero,
			size = size,
			heading = linearTrainHeading(segmentId, ui.dto.updnLine),
			background = bg,
			contentColor = fg,
			trainTextSize = 14.sp,
			destinationTextSize = 7.sp
		)
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
