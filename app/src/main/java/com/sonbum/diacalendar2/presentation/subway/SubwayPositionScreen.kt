package com.sonbum.diacalendar2.presentation.subway

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionDto
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 실시간 전용 안내 배너
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "위치 정보는 선택한 날짜와 무관하게 현재 시각 기준이며, 10초마다 자동 갱신됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            when {
                state.isLoading ->
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

                state.notRunning || state.trains.isEmpty() ->
                    CenterBox {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "열번 ${myTrainNo} 열차를 현재 운행 목록에서 찾지 못했습니다.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${line}호선에서 지금 운행 중인 열차만 표시합니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                "차량기지에서 막 출고했거나 본선 진입 전인 열차는 " +
                                        "실시간 위치에 아직 표시되지 않을 수 있습니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                else ->
                    SubwayLineMap(
                        trains = state.trains,
                        line = line,
                        modifier = Modifier.fillMaxSize()
                    )
            }
        }
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

/** 같은 역(seq)에 위치한 열차들을 묶은 노선도 노드. */
private data class StationNode(
    val seq: Int,
    val statnNm: String,
    val trains: List<SubwayTrainUi>
)

/**
 * 현재 응답에 등장하는 역들을 seq 순으로 세운 세로 노선도.
 * 각 역 위에 열차 마커를 얹고, 갱신 시 마커가 슬라이드 인/아웃으로 이동을 표현한다.
 */
@Composable
private fun SubwayLineMap(
    trains: List<SubwayTrainUi>,
    line: Int,
    modifier: Modifier = Modifier
) {
    // seq 오름차순으로 역 노드 구성 (같은 역에 여러 열차가 있을 수 있음).
    val nodes = remember(trains) {
        trains
            .groupBy { it.seq }
            .toSortedMap()
            .map { (seq, group) ->
                StationNode(
                    seq = seq,
                    statnNm = group.first().dto.statnNm ?: "-",
                    trains = group
                )
            }
    }
    // 진행 방향 라벨 (첫 열차 기준).
    val direction = trains.firstOrNull()?.let { directionLabel(it.dto.updnLine, line) } ?: ""

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            "${line}호선 · $direction · 현재 시각 기준",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        nodes.forEachIndexed { index, node ->
            StationRow(
                node = node,
                line = line,
                isFirst = index == 0,
                isLast = index == nodes.lastIndex
            )
        }
    }
}

/** 노선도 한 행: 왼쪽 연결선+역 원, 오른쪽 역 이름 + 열차 마커들. */
@Composable
private fun StationRow(
    node: StationNode,
    line: Int,
    isFirst: Boolean,
    isLast: Boolean
) {
    val hasTrain = node.trains.isNotEmpty()
    val hasMine = node.trains.any { it.isMine }
    val hasPrevious = node.trains.any { it.isPrevious }
    val nodeColor = when {
        hasMine -> MaterialTheme.colorScheme.primary
        hasPrevious -> Color(0xFFFF9800)
        hasTrain -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // 왼쪽 레일: 위/아래 연결선 + 역 원.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .background(
                        if (isFirst) Color.Transparent
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
            StationDot(color = nodeColor, pulsing = hasMine)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .weight(1f)
                    .background(
                        if (isLast) Color.Transparent
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }

        Spacer(Modifier.width(10.dp))

        // 오른쪽: 역 이름 + 해당 역 열차 마커들.
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                node.statnNm,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (hasTrain) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            node.trains.forEach { ui ->
                key(ui.dto.trainNo) {
                    // 열차가 이 역에 들어올 때 위에서 슬라이드 인, 떠날 때 슬라이드 아웃.
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(animationSpec = tween(450)) { -it / 2 } +
                                fadeIn(animationSpec = tween(450)),
                        exit = slideOutVertically(animationSpec = tween(300)) { it / 2 } +
                                fadeOut(animationSpec = tween(300))
                    ) {
                        TrainMarker(ui = ui, line = line)
                    }
                }
            }
        }
    }
}

/** 역 원. 내 열차가 있으면 은은한 펄스로 강조. */
@Composable
private fun StationDot(color: Color, pulsing: Boolean) {
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
            .size(14.dp)
            .scale(scale)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .border(3.dp, color, CircleShape)
    )
}

/** 역 옆에 표시되는 열차 카드형 마커. */
@Composable
private fun TrainMarker(ui: SubwayTrainUi, line: Int) {
    val dto: SubwayPositionDto = ui.dto
    val accent = when {
        ui.isMine -> MaterialTheme.colorScheme.primary
        ui.isPrevious -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.outline
    }
    val bgColor = when {
        ui.isMine -> MaterialTheme.colorScheme.primaryContainer
        ui.isPrevious -> Color(0xFFFF9800).copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(accent, RoundedCornerShape(3.dp))
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dto.directAt == "1") Chip("급행", Color(0xFFFF9800))
                    if (ui.isMine) Chip("내 열차", MaterialTheme.colorScheme.primary)
                    else if (ui.isPrevious) Chip("전 열번", Color(0xFFFF9800))
                    if (dto.lstcarAt == "1") Chip("막차", MaterialTheme.colorScheme.error)
                }
                Text(
                    text = "${statusLabel(dto.trainSttus)} · ${dto.statnTnm ?: ""}행",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "열번 ${dto.trainNo ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(start = 6.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

/** updnLine -> 한국어. 2호선은 내/외선, 그 외 상/하행. */
private fun directionLabel(updnLine: String?, line: Int): String =
    if (line == 2) {
        if (updnLine == "0") "내선순환" else "외선순환"
    } else {
        if (updnLine == "0") "상행" else "하행"
    }

/** trainSttus -> 한국어. */
private fun statusLabel(code: String?): String = when (code) {
    "0" -> "진입"
    "1" -> "도착"
    "2" -> "출발"
    "3" -> "전역 출발"
    else -> "운행중"
}
