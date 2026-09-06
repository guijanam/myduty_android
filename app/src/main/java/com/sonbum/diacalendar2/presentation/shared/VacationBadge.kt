package com.sonbum.diacalendar2.presentation.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 근태(휴가) 배지. 일반 근무를 덮어쓰는 근태일에 [ShiftBadge] 대신 사용한다.
 */
@Composable
fun VacationBadge(shortName: String, fontSize: Float = 14f) {
	Box(
		modifier = Modifier
			.clip(RoundedCornerShape(3.dp))
			.background(MaterialTheme.colorScheme.errorContainer)
			.padding(horizontal = 4.dp, vertical = 1.dp),
		contentAlignment = Alignment.Center
	) {
		Text(
			text = shortName,
			fontSize = fontSize.sp,
			lineHeight = (fontSize - 4).coerceAtLeast(8f).sp,
			fontWeight = FontWeight.Bold,
			color = MaterialTheme.colorScheme.onErrorContainer,
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
