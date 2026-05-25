package com.sonbum.diacalendar2

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sonbum.diacalendar2.shared.displayText
import java.time.YearMonth

@Composable
fun SimpleCalendarTitle(
	modifier: Modifier,
	currentMonth: YearMonth,
	isHorizontal: Boolean = true,
	onMenuClick: () -> Unit = {},
	goToday: () -> Unit,
	onMonthSelected: (YearMonth) -> Unit = {},
	restCount: Int? = null,
	coverCount: Int? = null,
) {
	var showMonthPicker by remember { mutableStateOf(false) }

	Row(
		modifier = modifier.height(40.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		// 메뉴 아이콘 (네비게이션 드로어)
		CalendarNavigationIcon(
			imageVector = Icons.Default.Menu,
			contentDescription = "Menu",
			onClick = onMenuClick,
			isHorizontal = true,
		)
		Row(
			modifier = Modifier
				.weight(1f)
				.clip(RoundedCornerShape(8.dp))
				.clickable { showMonthPicker = true }
				.padding(vertical = 4.dp, horizontal = 4.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.Center,
		) {
			if (restCount != null) {
				Text(
					text = "휴무:${restCount}개",
					color = MaterialTheme.colorScheme.onErrorContainer,
					fontSize = 14.sp,
					fontWeight = FontWeight.Bold,
				)
				Spacer(modifier = Modifier.width(10.dp))
			}
			Text(
				modifier = Modifier
					.testTag("MonthTitle")
					.clip(RoundedCornerShape(10.dp))
					.background(MaterialTheme.colorScheme.primaryContainer)
					.border(
						BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
						RoundedCornerShape(10.dp)
					)
					.padding(horizontal = 12.dp, vertical = 2.dp),
				text = currentMonth.displayText(),
				color = MaterialTheme.colorScheme.onPrimaryContainer,
				fontSize = 22.sp,
				textAlign = TextAlign.Center,
				fontWeight = FontWeight.Bold,
			)
			if (coverCount != null) {
				Spacer(modifier = Modifier.width(10.dp))
				Text(
					text = "휴근:${coverCount}개",
					color = MaterialTheme.colorScheme.onBackground,
					fontSize = 14.sp,
					fontWeight = FontWeight.Bold,
				)
			}
		}
		CalendarNavigationIcon(
			imageVector = Icons.Default.Today,
			contentDescription = "Today",
			onClick = goToday,
			isHorizontal = true,
		)
	}

	if (showMonthPicker) {
		MonthYearPickerDialog(
			currentYearMonth = currentMonth,
			onDismiss = { showMonthPicker = false },
			onConfirm = { selectedYearMonth ->
				onMonthSelected(selectedYearMonth)
				showMonthPicker = false
			}
		)
	}
}

@Composable
private fun CalendarNavigationIcon(
	imageVector: ImageVector,
	contentDescription: String,
	isHorizontal: Boolean = true,
	onClick: () -> Unit,
) = Box(
	modifier = Modifier
		.fillMaxHeight()
		.aspectRatio(1f)
		.clip(shape = CircleShape)
		.clickable(role = Role.Button, onClick = onClick),
) {
	val rotation by animateFloatAsState(
		targetValue = if (isHorizontal) 0f else 90f,
		label = "CalendarNavigationIconAnimation",
	)
	Icon(
		modifier = Modifier
			.fillMaxSize()
			.padding(4.dp)
			.align(Alignment.Center)
			.rotate(rotation),
		imageVector = imageVector,
		tint = MaterialTheme.colorScheme.onBackground,
		contentDescription = contentDescription,
	)
}

@Composable
private fun MonthYearPickerDialog(
	currentYearMonth: YearMonth,
	onDismiss: () -> Unit,
	onConfirm: (YearMonth) -> Unit
) {
	var selectedYear by remember { mutableIntStateOf(currentYearMonth.year) }
	var selectedMonth by remember { mutableIntStateOf(currentYearMonth.monthValue) }

	val months = listOf(
		"1월", "2월", "3월", "4월", "5월", "6월",
		"7월", "8월", "9월", "10월", "11월", "12월"
	)

	Dialog(onDismissRequest = onDismiss) {
		Surface(
			shape = RoundedCornerShape(16.dp),
			color = MaterialTheme.colorScheme.surface,
			tonalElevation = 6.dp,
			modifier = Modifier.fillMaxWidth(0.85f)
		) {
			Column(
				modifier = Modifier.padding(16.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				// 년도 선택
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.Center,
					verticalAlignment = Alignment.CenterVertically
				) {
					Box(
						modifier = Modifier
							.size(36.dp)
							.clip(CircleShape)
							.clickable { selectedYear-- },
						contentAlignment = Alignment.Center
					) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
							contentDescription = "Previous Year",
							modifier = Modifier.size(24.dp)
						)
					}
					Text(
						text = "${selectedYear}년",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						modifier = Modifier.padding(horizontal = 16.dp)
					)
					Box(
						modifier = Modifier
							.size(36.dp)
							.clip(CircleShape)
							.clickable { selectedYear++ },
						contentAlignment = Alignment.Center
					) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
							contentDescription = "Next Year",
							modifier = Modifier.size(24.dp)
						)
					}
				}

				Spacer(modifier = Modifier.height(12.dp))

				// 월 선택 그리드 (Column + Row로 변경하여 높이 고정 문제 해결)
				Column(
					verticalArrangement = Arrangement.spacedBy(6.dp)
				) {
					for (row in 0 until 3) {
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(6.dp)
						) {
							for (col in 0 until 4) {
								val index = row * 4 + col
								val month = index + 1
								val isSelected = selectedMonth == month
								val isCurrentMonth = selectedYear == currentYearMonth.year && month == currentYearMonth.monthValue

								Box(
									modifier = Modifier
										.weight(1f)
										.height(40.dp)
										.clip(RoundedCornerShape(8.dp))
										.background(
											when {
												isSelected -> MaterialTheme.colorScheme.primary
												isCurrentMonth -> MaterialTheme.colorScheme.primaryContainer
												else -> MaterialTheme.colorScheme.surfaceVariant
											}
										)
										.clickable { selectedMonth = month },
									contentAlignment = Alignment.Center
								) {
									Text(
										text = months[index],
										fontSize = 14.sp,
										color = when {
											isSelected -> MaterialTheme.colorScheme.onPrimary
											isCurrentMonth -> MaterialTheme.colorScheme.onPrimaryContainer
											else -> MaterialTheme.colorScheme.onSurfaceVariant
										},
										fontWeight = if (isSelected || isCurrentMonth) FontWeight.Bold else FontWeight.Normal
									)
								}
							}
						}
					}
				}

				Spacer(modifier = Modifier.height(16.dp))

				// 버튼
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.End
				) {
					TextButton(onClick = onDismiss) {
						Text("취소")
					}
					Spacer(modifier = Modifier.width(8.dp))
					TextButton(
						onClick = {
							onConfirm(YearMonth.of(selectedYear, selectedMonth))
						}
					) {
						Text("확인")
					}
				}
			}
		}
	}
}