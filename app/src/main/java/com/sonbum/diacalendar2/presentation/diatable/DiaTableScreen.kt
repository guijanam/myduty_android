package com.sonbum.diacalendar2.presentation.diatable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonbum.diacalendar2.domain.model.Dia
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


@Composable
fun DiaTableScreen(
    onBack: () -> Unit,
    onNavigateToServerDiaEdit: (Long) -> Unit = {},
    onNavigateToServerOfficeEdit: (Long) -> Unit = {},
    viewModel: DiaTableViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is DiaTableEvent.RestoreResult -> {
                    snackbarHostState.showSnackbar("${event.count}개 항목이 복원되었습니다")
                }
                is DiaTableEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    DiaTableContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onCategorySelect = viewModel::selectCategory,
        onToggleEditMode = viewModel::toggleEditMode,
        onDiaClick = { dia ->
            if (state.isEditMode && state.isServerOffice) {
                onNavigateToServerDiaEdit(dia.id)
            }
        },
        onOfficeEditClick = {
            state.currentOfficeCode?.let { onNavigateToServerOfficeEdit(it) }
        },
        onRestoreBackups = viewModel::restoreBackups,
        onClearBackups = viewModel::clearBackups
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaTableContent(
    state: DiaTableState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCategorySelect: (Int) -> Unit,
    onToggleEditMode: () -> Unit,
    onDiaClick: (Dia) -> Unit,
    onOfficeEditClick: () -> Unit,
    onRestoreBackups: () -> Unit,
    onClearBackups: () -> Unit
) {
    var showRestoreDialog by remember { mutableStateOf(false) }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("백업 복원") },
            text = { Text("이전에 편집한 ${state.backupCount}개 항목을 복원하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    onRestoreBackups()
                    showRestoreDialog = false
                }) {
                    Text("복원")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.officeName.isNotEmpty()) "${state.officeName} 근무표" else "근무표",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
//                actions = {
//                    // 서버 승무소일 때만 편집 모드 토글 표시
//                    if (state.isServerOffice) {
//                        IconButton(onClick = onToggleEditMode) {
//                            Icon(
//                                imageVector = if (state.isEditMode) Icons.Default.EditOff else Icons.Default.Edit,
//                                contentDescription = if (state.isEditMode) "편집 모드 끄기" else "편집 모드"
//                            )
//                        }
//                    }
//                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.categories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "근무표 데이터가 없습니다.\n교번 설정에서 승무소를 먼저 선택해주세요.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 편집 모드 배너
                if (state.isEditMode && state.isServerOffice) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "편집 모드 · 항목을 탭하여 수정",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = onOfficeEditClick) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(" 패턴", style = MaterialTheme.typography.labelSmall)
                            }
                            if (state.backupCount > 0) {
                                TextButton(onClick = { showRestoreDialog = true }) {
                                    Icon(
                                        Icons.Default.Restore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(" 복원(${state.backupCount})", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                val pagerState = rememberPagerState(
                    initialPage = state.selectedCategoryIndex,
                    pageCount = { state.categories.size }
                )
                val coroutineScope = rememberCoroutineScope()

                // 페이저 스와이프 → ViewModel 상태 동기화
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }.collect { page ->
                        onCategorySelect(page)
                    }
                }

                // ViewModel 상태 변경 → 페이저 동기화
                LaunchedEffect(state.selectedCategoryIndex) {
                    if (pagerState.currentPage != state.selectedCategoryIndex) {
                        pagerState.animateScrollToPage(state.selectedCategoryIndex)
                    }
                }

                // 헤더
                DiaTableHeader()
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // 페이저 (스와이프 가능한 본문)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val category = state.categories[page]
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = category.dias,
                            key = { it.id }
                        ) { dia ->
                            DiaTableRow(
                                dia = dia,
                                isClickable = state.isEditMode && state.isServerOffice,
                                onClick = { onDiaClick(dia) }
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // 하단 탭
                ScrollableTabRow(
                    selectedTabIndex = state.selectedCategoryIndex,
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    state.categories.forEachIndexed { index, category ->
                        Tab(
                            selected = state.selectedCategoryIndex == index,
                            onClick = {
                                onCategorySelect(index)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = "${category.name} (${category.dias.size})",
                                    fontWeight = if (state.selectedCategoryIndex == index)
                                        FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 1.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //HeaderCell(text = "교번", weight = 0.8f)
        //HeaderCell(text = "출근", weight = 1f)
        //HeaderCell(text = "전반", weight = 1f)
        //HeaderCell(text = "후반", weight = 1f)
        //HeaderCell(text = "실근무", weight = 1f)
    }
}

@Composable
private fun HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier.width((weight * 70).dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

@Composable
private fun DiaTableRow(
    dia: Dia,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
	// Column을 사용하여 내용 행(Row)과 구분선(Divider)을 수직으로 배치
	Column(
		modifier = Modifier
			.fillMaxWidth()
			// 배경색을 지정하여 이미지와 같은 느낌을 줍니다. 필요 없으면 제거하세요.
			.background(MaterialTheme.colorScheme.surface)
			.then(if (isClickable) Modifier.clickable(onClick = onClick) else Modifier),
		horizontalAlignment = Alignment.Start
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				// 상하 패딩을 주어 내용이 너무 붙지 않게 합니다.
				.padding(vertical = 2.dp, horizontal = 1.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// --- 1. ID 영역 (배경색 추가) ---
			Box(
				modifier = Modifier
					.weight(0.6f) // 너비 비율 설정
					.padding(end = 1.dp), // 옆 아이템과의 간격
				contentAlignment = Alignment.Center
			) {
				// 텍스트를 감싸는 배경 박스
				Box(
					modifier = Modifier
						// 원형 배경 (또는 RoundedCornerShape(4.dp) 등으로 사각형 변경 가능)
						.clip(RoundedCornerShape(2.dp))
						// 테마의 프라이머리 컨테이너 색상 사용 (눈에 잘 띄면서도 어울림)
						.background(MaterialTheme.colorScheme.primaryContainer)
						// 텍스트와 배경 사이의 내부 여백
						.padding(horizontal = 1.dp, vertical = 1.dp)
				) {
					Text(
						text = dia.diaId,
						fontSize = 12.sp,
						// 배경색 위에 올라가는 텍스트 색상 (onPrimaryContainer)
						color = MaterialTheme.colorScheme.onPrimaryContainer,
						textAlign = TextAlign.Center,
						style = TextStyle(
							platformStyle = PlatformTextStyle(includeFontPadding = false),
							fontWeight = FontWeight.Normal
						),
						maxLines = 2
					)
				}
			}

			// --- 2. 업무 시간 (workTime) ---
			Text(
				text = dia.workTime ?: "-",
				modifier = Modifier
					.weight(0.9f),
					//.background(Color.LightGray.copy(0.5f)),
				fontSize = 12.sp,

				// 일반 텍스트 색상
				color = MaterialTheme.colorScheme.onSurface,
				textAlign = TextAlign.Start,
				style = TextStyle(
					platformStyle = PlatformTextStyle(includeFontPadding = false),
					fontWeight = FontWeight.Bold
				),
				maxLines = 1
			)

			// --- 3. 첫 번째 시간대 (firstTime) ---
			Row(
				modifier = Modifier.weight(2.5f),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = dia.firstTime ?: "-",
					fontSize = 12.sp,
					color = MaterialTheme.colorScheme.onSurface,

					style = TextStyle(
						platformStyle = PlatformTextStyle(includeFontPadding = false),
						fontWeight = FontWeight.SemiBold

					),
					maxLines = 2,
					//overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
				)

			}

			// --- 4. 두 번째 시간대 (secondTime) ---
			Text(
				text = dia.secondTime ?: "-",
				modifier = Modifier.weight(2.5f),
				fontSize = 12.sp,
				color = MaterialTheme.colorScheme.onSurface,
				textAlign = TextAlign.Start,
				style = TextStyle(
					platformStyle = PlatformTextStyle(includeFontPadding = false),
					fontWeight = FontWeight.SemiBold
				),
				maxLines = 2,
				//overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
			)
		}

		// --- 가로 구분선 강조 ---
		// 행의 가장 하단에 구분선을 추가합니다.
		HorizontalDivider(
			modifier = Modifier.fillMaxWidth(),
			thickness = 1.dp, // 선의 두께
			// 테마의 외곽선 색상 중 연한 것을 사용하여 자연스럽게 구분
			color = MaterialTheme.colorScheme.outlineVariant
		)
	}
}


