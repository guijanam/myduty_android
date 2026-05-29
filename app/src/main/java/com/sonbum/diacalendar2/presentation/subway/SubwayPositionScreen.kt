package com.sonbum.diacalendar2.presentation.subway

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    IconButton(onClick = { viewModel.refresh(myTrainNo, line, officeName) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "새로고침")
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
                    text = "위치 정보는 선택한 날짜와 무관하게 현재 시각 기준입니다.",
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
                        }
                    }

                else ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                "${line}호선 · 현재 시각 기준",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(state.trains, key = { "${it.dto.trainNo}-${it.dto.statnId}" }) { ui ->
                            TrainCard(ui = ui, line = line)
                        }
                    }
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

@Composable
private fun TrainCard(ui: SubwayTrainUi, line: Int) {
    val dto: SubwayPositionDto = ui.dto
    val barColor = when {
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
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(barColor, RoundedCornerShape(3.dp))
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dto.statnNm ?: "-",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (dto.directAt == "1") Chip("급행", Color(0xFFFF9800))
                    if (ui.isMine) Chip("내 열차", MaterialTheme.colorScheme.primary)
                    else if (ui.isPrevious) Chip("전 열번", Color(0xFFFF9800))
                    if (dto.lstcarAt == "1") Chip("막차", MaterialTheme.colorScheme.error)
                }
                Text(
                    text = "${directionLabel(dto.updnLine, line)} · " +
                            "${statusLabel(dto.trainSttus)} · ${dto.statnTnm ?: ""}행",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
