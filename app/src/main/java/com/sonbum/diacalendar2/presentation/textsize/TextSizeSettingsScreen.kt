package com.sonbum.diacalendar2.presentation.textsize

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonbum.diacalendar2.data.local.datastore.CalendarTextSizes
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSizeSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: TextSizeSettingsViewModel = koinViewModel()
) {
    val textSizes by viewModel.textSizes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("텍스트 크기 설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.resetToDefault() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "초기화",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "초기화",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {

            // 미리보기 카드
            PreviewCard(textSizes = textSizes)

            Spacer(modifier = Modifier.height(1.dp))

	        // 근무조 패턴 텍스트 크기
	        TextSizeSlider(
		        title = "근무조 패턴",
		        description = "셀 상단 근무조 패턴 텍스트 크기",
		        currentSize = textSizes.crewPatternFontSize,
		        minSize = CalendarTextSizes.MIN_CREW_PATTERN_SIZE,
		        maxSize = CalendarTextSizes.MAX_CREW_PATTERN_SIZE,
		        onSizeChange = { viewModel.updateCrewPatternFontSize(it) }
	        )

            // 날짜 텍스트 크기
            TextSizeSlider(
                title = "날짜",
                description = "달력의 날짜 숫자 크기",
                currentSize = textSizes.dateFontSize,
                minSize = CalendarTextSizes.MIN_DATE_SIZE,
                maxSize = CalendarTextSizes.MAX_DATE_SIZE,
                onSizeChange = { viewModel.updateDateFontSize(it) }
            )

            // 근무 텍스트 크기
            TextSizeSlider(
                title = "근무",
                description = "교번/교대/근태 표시 크기",
                currentSize = textSizes.shiftFontSize,
                minSize = CalendarTextSizes.MIN_SHIFT_SIZE,
                maxSize = CalendarTextSizes.MAX_SHIFT_SIZE,
                onSizeChange = { viewModel.updateShiftFontSize(it) }
            )

            // 연동 캘린더 일정 텍스트 크기
            TextSizeSlider(
                title = "연동 캘린더 일정",
                description = "기기 캘린더 일정 텍스트 크기",
                currentSize = textSizes.eventFontSize,
                minSize = CalendarTextSizes.MIN_EVENT_SIZE,
                maxSize = CalendarTextSizes.MAX_EVENT_SIZE,
                onSizeChange = { viewModel.updateEventFontSize(it) }
            )

            // 메모 텍스트 크기
            TextSizeSlider(
                title = "메모",
                description = "메모 일정 텍스트 크기",
                currentSize = textSizes.memoFontSize,
                minSize = CalendarTextSizes.MIN_MEMO_SIZE,
                maxSize = CalendarTextSizes.MAX_MEMO_SIZE,
                onSizeChange = { viewModel.updateMemoFontSize(it) }
            )


        }
    }
}

@Composable
private fun PreviewCard(textSizes: CalendarTextSizes) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
//            Text(
//                text = "미리보기",
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )

            Spacer(modifier = Modifier.height(5.dp))

            // 미리보기 달력 셀
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 근무조 패턴 미리보기
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AB",
                            fontSize = textSizes.crewPatternFontSize.sp,
                            lineHeight = (textSizes.crewPatternFontSize + 1).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
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

                    // 날짜 미리보기
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "15",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = TextStyle(
                                fontSize = textSizes.dateFontSize.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                        Text(
                            text = "설날",
                            color = MaterialTheme.colorScheme.error,
                            style = TextStyle(
                                fontSize = (textSizes.dateFontSize - 2).coerceAtLeast(6f).sp,
                                fontWeight = FontWeight.Medium,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // 근무 미리보기
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "근무",
                                fontSize = textSizes.shiftFontSize.sp,
                                lineHeight = (textSizes.shiftFontSize - 4).coerceAtLeast(8f).sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
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

                    Spacer(modifier = Modifier.height(2.dp))

                    // 연동 캘린더 일정 미리보기
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF4285F4).copy(alpha = 0.3f))
                            .padding(horizontal = 2.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4285F4))
                        )
                        Text(
                            text = "연동캘린더일정",
                            fontSize = textSizes.eventFontSize.sp,
                            lineHeight = (textSizes.eventFontSize + 2).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            ),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }

                    // 메모 미리보기
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFFF9800).copy(alpha = 0.3f))
                            .padding(horizontal = 2.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF9800))
                        )
                        Text(
                            text = "메모표시",
                            fontSize = textSizes.memoFontSize.sp,
                            lineHeight = (textSizes.memoFontSize + 2).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            ),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextSizeSlider(
    title: String,
    description: String,
    currentSize: Float,
    minSize: Float,
    maxSize: Float,
    onSizeChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${currentSize.toInt()}sp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${minSize.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = currentSize,
                    onValueChange = onSizeChange,
                    valueRange = minSize..maxSize,
                    steps = ((maxSize - minSize) - 1).toInt(),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${maxSize.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
