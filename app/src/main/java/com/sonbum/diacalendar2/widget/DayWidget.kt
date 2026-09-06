package com.sonbum.diacalendar2.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.util.Log
import com.sonbum.diacalendar2.MainActivity
import com.sonbum.diacalendar2.widget.data.WidgetDataProvider
import com.sonbum.diacalendar2.widget.data.WidgetDayData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.getKoin
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class DayWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d("DayWidget", "provideGlance() called - reloading data")

        // 최초 데이터는 suspend 컨텍스트(백그라운드)에서 미리 로딩한다.
        val initialData = loadData()

        provideContent {
            // currentState를 읽어서 상태 변경 시 recomposition 트리거
            val prefs = currentState<Preferences>()
            val lastUpdated = prefs[longPreferencesKey("last_updated")] ?: 0L
            Log.d("DayWidget", "provideContent() recomposing, lastUpdated=$lastUpdated")

            // lastUpdated가 바뀌면 백그라운드에서 다시 로딩한다(메인 스레드 차단 금지).
            val dayDataList by produceState(initialValue = initialData, key1 = lastUpdated) {
                if (lastUpdated != 0L) {
                    value = loadData()
                }
            }

            val size = LocalSize.current
            // 너비/높이를 모두 고려해 글자 크기를 비례 조절한다.
            // 기준 크기(300 x 200dp)를 1.0으로 두고, 작아지면 줄이고 커지면 키운다.
            // 세로를 더 작게 줄여도 글자는 너무 작아지지 않도록 너비 비중을 크게 둔다.
            val widthScale = size.width.value / 300f
            val heightScale = size.height.value / 200f
            // 높이가 줄어도 글자가 크게 작아지지 않게 너비 기준을 우선하고,
            // 하한을 높여 작은 위젯에서도 글자가 충분히 크게 보이도록 한다.
            val scaleFactor = (widthScale * 0.7f + heightScale * 0.3f).coerceIn(0.85f, 2.0f)
            val isSmallMode = size.width < 200.dp || size.height < 110.dp

            GlanceTheme {
                DayWidgetContent(
                    todayData = dayDataList.getOrNull(0),
                    tomorrowData = dayDataList.getOrNull(1),
                    scaleFactor = scaleFactor,
                    isSmallMode = isSmallMode
                )
            }
        }
    }

    private suspend fun loadData(): List<WidgetDayData> {
        return try {
            val koin = getKoin()
            val provider = WidgetDataProvider(
                shiftScheduleDao = koin.get(),
                shiftSwapRecordDao = koin.get(),
                shiftInputRecordDao = koin.get(),
                lateWorkRecordDao = koin.get(),
                lateHolidayRecordDao = koin.get(),
                userShiftConfigDao = koin.get(),
                diaDao = koin.get(),
                localDiaDao = koin.get(),
                memoDao = koin.get(),
                holidayDao = koin.get(),
                deviceCalendarRepository = koin.get(),
                vacationRecordDao = koin.get()
            )
            val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
            val tomorrow = today.plusDays(1)
            withContext(Dispatchers.IO) {
                provider.loadDayDataList(listOf(today, tomorrow))
            }
        } catch (e: Exception) {
            Log.e("DayWidget", "Data loading failed", e)
            emptyList()
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun DayWidgetContent(
    todayData: WidgetDayData?,
    tomorrowData: WidgetDayData?,
    scaleFactor: Float,
    isSmallMode: Boolean
) {
    // 주간 위젯처럼 오늘(앞)/내일(뒤)을 좌우 두 칸으로 배치한다.
    // 각 칸은 세로로: 날짜 → worktime → 근무 → 이벤트/메모 순서.
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(vertical = (1 * scaleFactor).dp, horizontal = (2 * scaleFactor).dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            if (todayData != null) {
                DaySection(todayData, scaleFactor, isSmallMode)
            }
        }

        Box(
            modifier = GlanceModifier
                .width(0.5.dp)
                .fillMaxHeight()
                .background(ColorProvider(Color.Gray.copy(alpha = 0.5f)))
        ) {}

        Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            if (tomorrowData != null) {
                DaySection(tomorrowData, scaleFactor, isSmallMode)
            }
        }
    }
}

@Composable
private fun DaySection(
    data: WidgetDayData,
    scaleFactor: Float,
    isSmallMode: Boolean
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = (2 * scaleFactor).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.Top
    ) {
        DateRow(data, isSmallMode, scaleFactor)
        Spacer(modifier = GlanceModifier.height(if (isSmallMode) 0.dp else (1 * scaleFactor).dp))
        WorkTimeRow(data.workTime, isSmallMode, scaleFactor)
        ShiftNameRow(data, isSmallMode, scaleFactor)
        Spacer(modifier = GlanceModifier.height(if (isSmallMode) 0.dp else (1 * scaleFactor).dp))
        Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
            MemoAndEventColumn(data, isSmallMode, scaleFactor)
        }
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd(E)", Locale.KOREAN)

@SuppressLint("SuspiciousIndentation")
@Composable
private fun DateRow(data: WidgetDayData, isSmallMode: Boolean, scaleFactor: Float) {
    val textColor = when {
        data.isHoliday -> GlanceTheme.colors.error
        data.date.dayOfWeek == DayOfWeek.SATURDAY -> GlanceTheme.colors.primary
        else -> GlanceTheme.colors.onBackground
    }

//    val backgroundColor = if (data.isToday) {
//	    ImageProvider(R.drawable.widget_gradient_bg)
//    } else {
//        Color.LightGray.copy(alpha = 0.3f)
//    }
	// 모든 날짜에 동일한 배경 적용 (오늘 강조 배경 제거 - 항상 첫 칸이 오늘)
	val rowModifier = GlanceModifier
		.fillMaxWidth()
		.background(Color.LightGray.copy(alpha = 0.3f))

    val baseSize = if (isSmallMode) 10 else 16

    Row(
	    modifier = rowModifier,
	    horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = GlanceModifier.padding(horizontal = 2.dp),
            text = data.date.format(DATE_FORMATTER),
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                color = textColor,
                fontSize = (baseSize * scaleFactor).sp,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun WorkTimeRow(workTime: String?, isSmallMode: Boolean, scaleFactor: Float) {
    val baseSize = if (isSmallMode) 18 else 24
    Text(
        modifier = GlanceModifier.fillMaxWidth(),
        text = workTime ?: "",
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            color = GlanceTheme.colors.onBackground,
            fontSize = (baseSize * scaleFactor).sp,
            textAlign = TextAlign.Center
        )
    )
}

@Composable
private fun ShiftNameRow(data: WidgetDayData, isSmallMode: Boolean, scaleFactor: Float) {
    val shiftName = data.effectiveShiftName ?: return
    val baseSize = if (isSmallMode) 15 else 20

    Text(
        modifier = GlanceModifier.fillMaxWidth(),
        text = shiftName,
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            color = getShiftTextColor(shiftName, data.isSwap, data.shiftInputColorHex, data.isVacation),
            fontSize = (baseSize * scaleFactor).sp,
            textAlign = TextAlign.Center
        )
    )
}

@Composable
private fun MemoAndEventColumn(data: WidgetDayData, isSmallMode: Boolean, scaleFactor: Float) {
    val allItems = data.calendarEventTitles.map { "📅$it" } + data.memoTitles

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(1.dp)
            .background(GlanceTheme.colors.surfaceVariant)
    ) {
        if (allItems.isEmpty()) {
            Text(
                text = "",
                style = TextStyle(
                    fontSize = (9 * scaleFactor).sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        } else {
            val maxVisible = if (isSmallMode) 3 else 5
            val baseSize = if (isSmallMode) 14 else 15

            allItems.take(maxVisible).forEachIndexed { index, title ->
                Text(
                    text = title,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onBackground,
                        fontSize = (baseSize * scaleFactor).sp,
                        textAlign = TextAlign.Start
                    ),
                    maxLines = if (isSmallMode) 1 else 2
                )
                if (index < minOf(allItems.size - 1, maxVisible - 1)) {
                    Spacer(modifier = GlanceModifier.height((if (isSmallMode) 1f else 2f * scaleFactor).dp))
                }
            }

            if (allItems.size > maxVisible) {
                val moreBaseSize = if (isSmallMode) 9 else 12
                Text(
                    text = "외 ${allItems.size - maxVisible}개",
                    style = TextStyle(
                        fontSize = (moreBaseSize * scaleFactor).sp,
                        color = GlanceTheme.colors.onBackground,
                        textAlign = TextAlign.Start
                    )
                )
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
internal fun getShiftTextColor(turn: String): ColorProvider {
    return when {
        turn.contains("~") || turn.contains("비") -> GlanceTheme.colors.onSurfaceVariant
        turn.contains("대") -> ColorProvider(Color(0xFF4CAF50))
        turn.contains("휴") || turn.contains("연") -> GlanceTheme.colors.error
        turn.contains("지근") -> GlanceTheme.colors.primary
        else -> GlanceTheme.colors.onBackground
    }
}

/**
 * 근태/교체/충당으로 바뀐 날은 그 색을 글자색으로 우선 적용한다.
 * - 근태(휴가): 빨간색
 * - 충당: shiftInputColorHex(예: "#4CAF50") 색
 * - 교체: 주황색
 * - 그 외: 교번 이름 기반 기본 색([getShiftTextColor])
 */
@SuppressLint("RestrictedApi")
@Composable
internal fun getShiftTextColor(
    turn: String,
    isSwap: Boolean,
    shiftInputColorHex: String?,
    isVacation: Boolean = false
): ColorProvider {
    if (isVacation) return GlanceTheme.colors.error
    if (shiftInputColorHex != null) {
        val color = try {
            Color(shiftInputColorHex.toColorInt())
        } catch (_: Exception) {
            null
        }
        if (color != null) return ColorProvider(color)
    }
    if (isSwap) return ColorProvider(Color(0xFFFF9800))
    return getShiftTextColor(turn)
}
