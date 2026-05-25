package com.sonbum.diacalendar2.presentation.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt

@Composable
fun ShiftBadge(
    shiftName: String,
    isSwap: Boolean = false,
    isShiftInput: Boolean = false,
    shiftInputColorHex: String? = null,
    isHolidayWork: Boolean = false,
    fontSize: Float = 14f
) {
    val isContainsTilde = shiftName.contains("~")
    val displayText = if (shiftName.contains("~")) "~" else shiftName
    val isDark = isSystemInDarkTheme()
    val (rawBackgroundColor, rawTextColor) = when {
        isHolidayWork ->
            (if (isDark) Color(0xFFC62828) else Color(0xFFFFCDD2)) to (if (isDark) Color.White else Color.Black)
        isShiftInput && shiftInputColorHex != null -> {
            when (shiftInputColorHex.uppercase()) {
                "#4CAF50" ->
                    if (isDark) Color(0xFF388E3C) to Color.White
                    else Color(0xFFC8E6C9) to Color(0xFF1B5E20)
                "#9C27B0" ->
                    if (isDark) Color(0xFF7B1FA2) to Color.White
                    else Color(0xFFE1BEE7) to Color(0xFF4A148C)
                "#03A9F4" ->
                    if (isDark) Color(0xFF0288D1) to Color.White
                    else Color(0xFFB3E5FC) to Color(0xFF01579B)
                else -> {
                    val baseColor = try {
                        Color(shiftInputColorHex.toColorInt())
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.tertiary
                    }
                    if (isDark) baseColor to Color.White
                    else baseColor.copy(alpha = 0.3f) to Color.Black
                }
            }
        }
        isSwap ->
            (if (isDark) Color(0xFFF57C00) else Color(0xFFFF9800)) to (if (isDark) Color.White else Color.Black)
        shiftName == "지근" ->
            (if (isDark) Color(0xFF0288D1) else Color(0xFF81D4FA)) to (if (isDark) Color.White else Color.Black)
        shiftName == "지휴" ->
            (if (isDark) Color(0xFFC62828) else Color(0xFFFFCDD2)) to (if (isDark) Color.White else Color.Black)
        shiftName.contains("휴") ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        shiftName.contains("대") ->
            if (isDark) {
                Color(0xFF2E7D32) to Color.White
            } else {
                Color(0xFFC8E6C9) to Color(0xFF1B5E20)
            }
        else ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }

    val backgroundColor = if (isContainsTilde) rawBackgroundColor.copy(alpha = 0.3f) else rawBackgroundColor
    val textColor = if (isContainsTilde) rawTextColor.copy(alpha = 0.6f) else rawTextColor

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            fontSize = fontSize.sp,
            lineHeight = (fontSize - 4).coerceAtLeast(8f).sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
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
