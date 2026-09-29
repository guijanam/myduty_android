package com.sonbum.diacalendar2.presentation.shiftcolor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.sonbum.diacalendar2.presentation.shared.ShiftBadge
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun ShiftColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    val initialHsv = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by rememberSaveable { mutableFloatStateOf(initialHsv[0]) }
    var saturation by rememberSaveable { mutableFloatStateOf(initialHsv[1]) }
    var brightness by rememberSaveable { mutableFloatStateOf(initialHsv[2]) }
    val color = Color.hsv(hue, saturation, brightness)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title 색상 선택") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ShiftBadge(
                        shiftName = if (title == "야간근무") "야간" else "주간",
                        dayShiftBackgroundColor = color,
                        nightShiftBackgroundColor = color,
                        isNightShift = title == "야간근무",
                        fontSize = 18f
                    )
                }
                Text("영역을 터치하거나 드래그하여 색의 진하기와 밝기를 조절하세요.")
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .semantics {
                            contentDescription = "색의 진하기와 밝기 선택"
                            stateDescription = "진하기 ${(saturation * 100).roundToInt()}%, 밝기 ${(brightness * 100).roundToInt()}%"
                            customActions = listOf(
                                CustomAccessibilityAction("더 진하게") {
                                    saturation = (saturation + 0.05f).coerceAtMost(1f); true
                                },
                                CustomAccessibilityAction("더 연하게") {
                                    saturation = (saturation - 0.05f).coerceAtLeast(0f); true
                                },
                                CustomAccessibilityAction("더 밝게") {
                                    brightness = (brightness + 0.05f).coerceAtMost(1f); true
                                },
                                CustomAccessibilityAction("더 어둡게") {
                                    brightness = (brightness - 0.05f).coerceAtLeast(0f); true
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                fun select(position: Offset) {
                                    saturation = (position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    brightness = 1f - (position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                }
                                select(down.position)
                                down.consume()
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    select(change.position)
                                    change.consume()
                                } while (change.pressed)
                            }
                        }
                ) {
                    drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    val center = Offset(saturation * size.width, (1f - brightness) * size.height)
                    drawCircle(Color.Black, 8.dp.toPx(), center, style = Stroke(4.dp.toPx()))
                    drawCircle(Color.White, 8.dp.toPx(), center, style = Stroke(2.dp.toPx()))
                }
                Text("색조", style = MaterialTheme.typography.labelLarge)
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(
                                (0..6).map { Color.hsv(it * 60f, 1f, 1f) }
                            ))
                    )
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.semantics { contentDescription = "색조 선택" },
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(String.format(Locale.ROOT, "#%06X", color.toArgb() and 0xFFFFFF))
            }) { Text("적용") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
