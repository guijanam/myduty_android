package com.sonbum.diacalendar2.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sonbum.diacalendar2.domain.model.CalendarEvent
import com.sonbum.diacalendar2.domain.model.Memo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val HOUR_HEIGHT = 60.dp
private val TIME_LABEL_WIDTH = 48.dp
private val MIN_BLOCK_HEIGHT = 20.dp
private const val SNAP_MINUTES = 15

@Composable
fun DayTimeblockView(
    state: DateDetailState,
    onEventClick: (CalendarEvent) -> Unit,
    onMemoClick: (Memo) -> Unit,
    onMemoTimeChanged: (memoId: String, newStart: LocalTime, newEnd: LocalTime) -> Unit = { _, _, _ -> },
    onEventTimeChanged: (eventId: Long, newStart: LocalDateTime, newEnd: LocalDateTime) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val date = state.date
    val isToday = date == LocalDate.now()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    // 드래그 상태
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // 종일 이벤트/메모와 시간 이벤트 분리
    val allDayEvents = state.calendarEvents.filter { it.isAllDay }
    val allDayMemos = state.memos.filter { it.isAllDay }
    val timedItems = remember(state.calendarEvents, state.memos, date) {
        val eventItems = state.calendarEvents.mapNotNull { it.toTimeblockItem(date) }
        val memoItems = state.memos.mapNotNull { it.toTimeblockItem() }
        (eventItems + memoItems).sortedBy { it.startMinutes }
    }

    // 겹침 레이아웃 계산
    val layoutItems = remember(timedItems) { calculateOverlapLayout(timedItems) }

    val scrollState = rememberScrollState()
    val hourHeightPx = with(density) { HOUR_HEIGHT.toPx() }

    // 자동 스크롤: 첫 이벤트 또는 현재 시간으로
    LaunchedEffect(layoutItems, isToday) {
        val targetMinutes = when {
            timedItems.isNotEmpty() -> (timedItems.first().startMinutes - 60).coerceAtLeast(0)
            isToday -> (LocalTime.now().hour * 60 - 60).coerceAtLeast(0)
            else -> 7 * 60 // 07:00
        }
        val targetPx = with(density) { (HOUR_HEIGHT * targetMinutes / 60f).toPx() }
        scrollState.scrollTo(targetPx.toInt())
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 종일 헤더
        AllDayHeader(
            shiftName = state.effectiveShiftName ?: state.shiftName,
            holidayName = state.holidayName,
            allDayEvents = allDayEvents,
            allDayMemos = allDayMemos,
            onEventClick = onEventClick,
            onMemoClick = onMemoClick
        )

        // 시간별 타임라인
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState, enabled = draggingItemId == null)
        ) {
            val totalHeight = HOUR_HEIGHT * 24
            val eventAreaWidth = maxWidth - TIME_LABEL_WIDTH

            Box(modifier = Modifier.height(totalHeight).fillMaxWidth()) {
                // 시간 레이블 + 시간선
                for (hour in 0..23) {
                    val yOffset = HOUR_HEIGHT * hour

                    Text(
                        text = String.format("%02d:00", hour),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.offset(x = 4.dp, y = yOffset - 6.dp)
                    )

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = TIME_LABEL_WIDTH, y = yOffset),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                // 드래그 중 15분 스냅 가이드라인
                if (draggingItemId != null) {
                    for (quarter in 0 until 96) {
                        if (quarter % 4 == 0) continue // 정시 라인은 이미 표시됨
                        val qY = HOUR_HEIGHT * quarter / 4f
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(x = TIME_LABEL_WIDTH, y = qY),
                            thickness = 0.25.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    }
                }

                // 이벤트/메모 블록 렌더링
                for (item in layoutItems) {
                    val tbItem = item.timeblockItem
                    val isDragging = draggingItemId == tbItem.id
                    val canDrag = when (tbItem.type) {
                        TimeblockItemType.CALENDAR_EVENT -> tbItem.originalEvent?.rrule == null
                        TimeblockItemType.MEMO -> true
                    }

                    val baseYPx = hourHeightPx * tbItem.startMinutes / 60f
                    val blockHeightDp = (HOUR_HEIGHT * (tbItem.endMinutes - tbItem.startMinutes) / 60f)
                        .coerceAtLeast(MIN_BLOCK_HEIGHT)
                    val itemWidth = eventAreaWidth / item.totalColumns
                    val xOffset = TIME_LABEL_WIDTH + itemWidth * item.column

                    // 드래그 중이면 오프셋 적용
                    val actualYPx = if (isDragging) baseYPx + dragOffsetY else baseYPx
                    val snappedMinutes = if (isDragging) {
                        val rawMin = (actualYPx / hourHeightPx * 60).roundToInt()
                        ((rawMin / SNAP_MINUTES) * SNAP_MINUTES).coerceIn(0, 1440 - (tbItem.endMinutes - tbItem.startMinutes))
                    } else tbItem.startMinutes
                    val snappedYPx = if (isDragging) hourHeightPx * snappedMinutes / 60f else actualYPx

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = with(density) { xOffset.roundToPx() }, y = snappedYPx.roundToInt()) }
                            .width(itemWidth - 2.dp)
                            .height(blockHeightDp)
                            .zIndex(if (isDragging) 10f else 0f)
                            .then(
                                if (canDrag) {
                                    Modifier.pointerInput(tbItem.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingItemId = tbItem.id
                                                dragOffsetY = 0f
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                            },
                                            onDragEnd = {
                                                // 새 시간 계산
                                                val newYPx = baseYPx + dragOffsetY
                                                val rawMin = (newYPx / hourHeightPx * 60).roundToInt()
                                                val duration = tbItem.endMinutes - tbItem.startMinutes
                                                val newStartMin = ((rawMin / SNAP_MINUTES) * SNAP_MINUTES)
                                                    .coerceIn(0, 1440 - duration)
                                                val newEndMin = newStartMin + duration

                                                val newStartTime = LocalTime.of(newStartMin / 60, newStartMin % 60)
                                                val newEndTime = LocalTime.of(
                                                    (newEndMin / 60).coerceAtMost(23),
                                                    if (newEndMin >= 1440) 59 else newEndMin % 60
                                                )

                                                when (tbItem.type) {
                                                    TimeblockItemType.MEMO -> {
                                                        tbItem.originalMemo?.let { memo ->
                                                            onMemoTimeChanged(memo.objectId, newStartTime, newEndTime)
                                                        }
                                                    }
                                                    TimeblockItemType.CALENDAR_EVENT -> {
                                                        tbItem.originalEvent?.let { event ->
                                                            val newStart = LocalDateTime.of(date, newStartTime)
                                                            val newEnd = LocalDateTime.of(date, newEndTime)
                                                            onEventTimeChanged(event.id, newStart, newEnd)
                                                        }
                                                    }
                                                }

                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                draggingItemId = null
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggingItemId = null
                                                dragOffsetY = 0f
                                            }
                                        )
                                    }
                                } else Modifier
                            )
                            .then(
                                if (!isDragging) {
                                    Modifier.clickable {
                                        when (tbItem.type) {
                                            TimeblockItemType.CALENDAR_EVENT ->
                                                tbItem.originalEvent?.let(onEventClick)
                                            TimeblockItemType.MEMO ->
                                                tbItem.originalMemo?.let(onMemoClick)
                                        }
                                    }
                                } else Modifier
                            )
                    ) {
                        TimeblockEventBlock(
                            item = tbItem,
                            isDragging = isDragging,
                            previewStartMinutes = if (isDragging) snappedMinutes else null,
                            previewEndMinutes = if (isDragging) snappedMinutes + (tbItem.endMinutes - tbItem.startMinutes) else null
                        )
                    }
                }

                // 현재 시간 표시선 (오늘일 때만)
                if (isToday) {
                    val now = LocalTime.now()
                    val nowMinutes = now.hour * 60 + now.minute
                    val nowY = HOUR_HEIGHT * nowMinutes / 60f

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset(x = TIME_LABEL_WIDTH - 4.dp, y = nowY)
                    ) {
                        drawCircle(
                            color = Color.Red,
                            radius = 5.dp.toPx(),
                            center = Offset(0f, size.height / 2)
                        )
                        drawLine(
                            color = Color.Red,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllDayHeader(
    shiftName: String?,
    holidayName: String?,
    allDayEvents: List<CalendarEvent>,
    allDayMemos: List<Memo>,
    onEventClick: (CalendarEvent) -> Unit,
    onMemoClick: (Memo) -> Unit
) {
    val hasContent = shiftName != null || holidayName != null || allDayEvents.isNotEmpty() || allDayMemos.isNotEmpty()
    if (!hasContent) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (shiftName != null || holidayName != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (shiftName != null) {
                    Text(
                        text = "교번: $shiftName",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (holidayName != null) {
                    if (shiftName != null) Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = holidayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        allDayEvents.forEach { event ->
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(event.color).copy(alpha = 0.15f))
                    .clickable { onEventClick(event) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(event.color),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 종일 메모
        allDayMemos.forEach { memo ->
            val memoColor = try {
                Color(android.graphics.Color.parseColor(memo.hexColorString))
            } catch (e: Exception) {
                Color(0xFF4CAF50)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(memoColor.copy(alpha = 0.2f))
                    .clickable { onMemoClick(memo) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = memo.title.ifEmpty { memo.content },
                    style = MaterialTheme.typography.labelMedium,
                    color = memoColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    HorizontalDivider(thickness = 0.5.dp)
}

@Composable
private fun TimeblockEventBlock(
    item: TimeblockItem,
    isDragging: Boolean = false,
    previewStartMinutes: Int? = null,
    previewEndMinutes: Int? = null,
    modifier: Modifier = Modifier
) {
    val bgAlpha = when {
        isDragging -> if (item.type == TimeblockItemType.CALENDAR_EVENT) 0.3f else 0.4f
        else -> if (item.type == TimeblockItemType.CALENDAR_EVENT) 0.15f else 0.2f
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val displayStartMin = previewStartMinutes ?: item.startMinutes
    val displayEndMin = previewEndMinutes ?: item.endMinutes

    val startTime = LocalTime.of(displayStartMin / 60, displayStartMin % 60)
    val endTime = LocalTime.of(
        (displayEndMin / 60).coerceAtMost(23),
        if (displayEndMin >= 1440) 59 else displayEndMin % 60
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isDragging) Modifier.shadow(8.dp, RoundedCornerShape(4.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(4.dp))
            .background(item.color.copy(alpha = bgAlpha))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Column {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 14.sp
                ),
                color = item.color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (isDragging) 10.sp else 9.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isDragging) item.color else item.color.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

// --- 겹침 레이아웃 알고리즘 (Column-packing) ---

data class LayoutItem(
    val timeblockItem: TimeblockItem,
    val column: Int,
    val totalColumns: Int
)

private fun calculateOverlapLayout(items: List<TimeblockItem>): List<LayoutItem> {
    if (items.isEmpty()) return emptyList()

    val groups = mutableListOf<MutableList<TimeblockItem>>()

    for (item in items) {
        val existingGroup = groups.find { group ->
            group.any { it.startMinutes < item.endMinutes && item.startMinutes < it.endMinutes }
        }
        if (existingGroup != null) {
            existingGroup.add(item)
        } else {
            groups.add(mutableListOf(item))
        }
    }

    var merged = true
    while (merged) {
        merged = false
        outer@ for (i in groups.indices) {
            for (j in i + 1 until groups.size) {
                val groupI = groups[i]
                val groupJ = groups[j]
                val overlaps = groupI.any { a ->
                    groupJ.any { b ->
                        a.startMinutes < b.endMinutes && b.startMinutes < a.endMinutes
                    }
                }
                if (overlaps) {
                    groupI.addAll(groupJ)
                    groups.removeAt(j)
                    merged = true
                    break@outer
                }
            }
        }
    }

    val result = mutableListOf<LayoutItem>()

    for (group in groups) {
        val sorted = group.sortedBy { it.startMinutes }
        val columns = mutableListOf<MutableList<TimeblockItem>>()

        for (item in sorted) {
            val col = columns.indexOfFirst { col ->
                col.last().endMinutes <= item.startMinutes
            }
            if (col >= 0) {
                columns[col].add(item)
            } else {
                columns.add(mutableListOf(item))
            }
        }

        val totalCols = columns.size.coerceAtMost(4)
        for ((colIndex, col) in columns.withIndex()) {
            for (item in col) {
                result.add(
                    LayoutItem(
                        timeblockItem = item,
                        column = colIndex.coerceAtMost(totalCols - 1),
                        totalColumns = totalCols
                    )
                )
            }
        }
    }

    return result
}
