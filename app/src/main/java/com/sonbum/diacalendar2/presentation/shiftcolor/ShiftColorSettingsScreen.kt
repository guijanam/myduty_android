package com.sonbum.diacalendar2.presentation.shiftcolor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.sonbum.diacalendar2.data.local.datastore.ShiftDisplayColors
import com.sonbum.diacalendar2.presentation.shared.ShiftBadge
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftColorSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ShiftColorSettingsViewModel = koinViewModel()
) {
    val colors by viewModel.colors.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("근무 색상 설정") },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShiftColorPreview(colors = colors)

            ShiftColorSection(
                title = "주간근무",
                selectedHex = colors.dayShiftColorHex,
                onColorSelected = viewModel::updateDayShiftColor
            )

            ShiftColorSection(
                title = "야간근무",
                selectedHex = colors.nightShiftColorHex,
                onColorSelected = viewModel::updateNightShiftColor
            )
        }
    }
}

@Composable
private fun ShiftColorPreview(colors: ShiftDisplayColors) {
    val dayColor = colors.dayShiftColorHex.toColorOrNull()
    val nightColor = colors.nightShiftColorHex.toColorOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShiftBadge(
                shiftName = "주간",
                dayShiftBackgroundColor = dayColor,
                nightShiftBackgroundColor = nightColor,
                isNightShift = false,
                fontSize = 14f
            )
            ShiftBadge(
                shiftName = "야간",
                dayShiftBackgroundColor = dayColor,
                nightShiftBackgroundColor = nightColor,
                isNightShift = true,
                fontSize = 14f
            )
            ShiftBadge(
                shiftName = "휴",
                dayShiftBackgroundColor = dayColor,
                nightShiftBackgroundColor = nightColor,
                fontSize = 14f
            )
            ShiftBadge(
                shiftName = "대",
                dayShiftBackgroundColor = dayColor,
                nightShiftBackgroundColor = nightColor,
                fontSize = 14f
            )
        }
    }
}

@Composable
private fun ShiftColorSection(
    title: String,
    selectedHex: String,
    onColorSelected: (String) -> Unit
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    if (showPicker) {
        ShiftColorPickerDialog(
            title = title,
            initialColor = selectedHex.toColorOrNull() ?: MaterialTheme.colorScheme.primaryContainer,
            onDismiss = { showPicker = false },
            onApply = { hex ->
                onColorSelected(hex)
                showPicker = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                SelectedColorLabel(hex = selectedHex)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 5
            ) {
                val options = listOf(ShiftDisplayColors.DEFAULT_DAY_SHIFT_COLOR_HEX) + SHIFT_COLOR_OPTIONS
                options.forEach { colorOption ->
                    ColorOptionButton(
                        hex = colorOption,
                        selected = selectedHex.equals(colorOption, ignoreCase = true),
                        onClick = { onColorSelected(colorOption) }
                    )
                }
            }

            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("직접 선택")
            }
        }
    }
}

@Composable
private fun SelectedColorLabel(hex: String) {
    val color = hex.toColorOrNull() ?: MaterialTheme.colorScheme.primaryContainer
    val label = if (hex.isBlank()) "기본" else "선택한 색상"

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ColorOptionButton(
    hex: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    // hex가 비어 있으면 "기본" 선택지이며 테마의 primaryContainer를 그대로 보여준다.
    val isThemeDefault = hex.isBlank()
    val color = hex.toColorOrNull() ?: MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (color.luminance() > 0.5f) Color.Black else Color.White

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (isThemeDefault) "테마 기본색" else
                    "추천 색상 ${SHIFT_COLOR_OPTIONS.indexOf(hex) + 1}"
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            selected -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "선택됨",
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            isThemeDefault -> Text(
                text = "기본",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

private fun String.toColorOrNull(): Color? {
    // 빈 값(테마 기본색)이면 null을 돌려주고, 잘못된 값도 예외로 앱이 죽지 않게 한다.
    if (isBlank()) return null
    return try {
        Color(toColorInt())
    } catch (e: RuntimeException) {
        null
    }
}

private val SHIFT_COLOR_OPTIONS = listOf(
    // 충당/교체/휴·대·지근·지휴와 색이 겹치는 계열은 제외한다.
    // 제외: 초록(대기충당·"대"), 보라(휴무충당), 하늘·파랑(지근·지근충당), 주황(교체), 붉은색(지휴·휴일근무)
    "#FFF9C4",
    "#FFF59D",
    "#F8BBD0",
    "#F48FB1",
    "#D7CCC8",
    "#BCAAA4",
    "#CFD8DC",
    "#B0BEC5",
    "#E0E0E0",
    "#BDBDBD"
)
