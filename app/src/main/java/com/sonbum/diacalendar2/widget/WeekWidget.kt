package com.sonbum.diacalendar2.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.util.Log
import androidx.glance.ImageProvider
import com.sonbum.diacalendar2.MainActivity
import com.sonbum.diacalendar2.R
import com.sonbum.diacalendar2.widget.data.WidgetDataProvider
import com.sonbum.diacalendar2.widget.data.WidgetDayData
import kotlinx.coroutines.Dispatchers
import org.koin.java.KoinJavaComponent.getKoin
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeekWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d("WeekWidget", "provideGlance() called")

        provideContent {
            // currentState를 읽어서 상태 변경 시 recomposition 트리거
            val prefs = currentState<Preferences>()
            val lastUpdated = prefs[longPreferencesKey("last_updated")] ?: 0L
            Log.d("WeekWidget", "provideContent() recomposing, lastUpdated=$lastUpdated")

            val dayDataList = loadDataBlocking()

            val size = LocalSize.current
            val scaleFactor = (size.width.value / 300f).coerceIn(1.0f, 2.0f)

            GlanceTheme {
                WeekWidgetContent(dayDataList, scaleFactor)
            }
        }
    }

    private fun loadDataBlocking(): List<WidgetDayData> {
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
                deviceCalendarRepository = koin.get()
            )
            val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
            val weekDates = (0L..6L).map { today.plusDays(it) }
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                provider.loadDayDataList(weekDates)
            }
        } catch (e: Exception) {
            Log.e("WeekWidget", "Data loading failed", e)
            emptyList()
        }
    }
}

private val WEEK_DAY_FORMATTER = DateTimeFormatter.ofPattern("E", Locale.KOREAN)

@Composable
private fun WeekWidgetContent(
    dayDataList: List<WidgetDayData>,
    scaleFactor: Float
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(0.dp)
            .background(GlanceTheme.colors.background)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top
    ) {
        // Date row
        WeekDayRow(dayDataList, scaleFactor)
        // Memo row
        WeekMemoRow(dayDataList, scaleFactor)
        // WorkTime row
        WeekWorkTimeRow(dayDataList, scaleFactor)
        // Shift name row
        WeekShiftRow(dayDataList, scaleFactor)
    }
}

@Composable
private fun WeekDayRow(dayDataList: List<WidgetDayData>, scaleFactor: Float) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        for (data in dayDataList) {
            val textColor = when {
                data.isHoliday -> GlanceTheme.colors.error
                data.date.dayOfWeek == DayOfWeek.SATURDAY -> GlanceTheme.colors.primary
                else -> GlanceTheme.colors.onBackground
            }

//            val backgroundColor = if (data.isToday) {
//                Color.Yellow.copy(alpha = 0.5f)
//            } else {
//                Color.LightGray
//            }
	        // 2. Text에 적용할 Modifier를 조건에 따라 조립 (이름을 textModifier로 변경!)
	        val textModifier = GlanceModifier.defaultWeight().let { baseModifier ->
		        if (data.isToday) {
			        // 오늘일 때는 우리가 만든 쨍한 블루(#00FFF0 -> #0083FE) 그라데이션 XML 적용!
			        baseModifier.background(ImageProvider(R.drawable.widget_gradient_bg))
		        } else {
			        // 오늘이 아닐 때는 반투명 밝은 회색 적용
			        baseModifier.background(Color.LightGray.copy(alpha = 0.3f))
		        }
	        }

            val dateText = "${data.date.dayOfMonth}(${data.date.format(WEEK_DAY_FORMATTER)})"

            Text(
	            modifier = textModifier,
                text = dateText,
                maxLines = 1,
                style = TextStyle(
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic,
                    color = textColor,
                    fontSize = (12 * scaleFactor).sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun WeekMemoRow(dayDataList: List<WidgetDayData>, scaleFactor: Float) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(0.dp)
    ) {
        for (data in dayDataList) {
            val allItems = data.memoTitles + data.calendarEventTitles
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(horizontal = 2.dp)
            ) {
                if (allItems.isNotEmpty()) {
                    allItems.take(3).forEach { title ->
                        Text(
                            text = if (title.length > 8) "${title.take(6)}..." else title,
                            style = TextStyle(
                                fontWeight = FontWeight.Normal,
                                color = GlanceTheme.colors.onBackground,
                                fontSize = (10 * scaleFactor).sp,
                                textAlign = TextAlign.Start
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekWorkTimeRow(dayDataList: List<WidgetDayData>, scaleFactor: Float) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(all = 0.dp)
    ) {
        for (data in dayDataList) {
            Text(
                modifier = GlanceModifier.defaultWeight(),
                text = data.workTime ?: "",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onBackground,
                    fontSize = (11 * scaleFactor).sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun WeekShiftRow(dayDataList: List<WidgetDayData>, scaleFactor: Float) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(all = 0.dp)
    ) {
        for (data in dayDataList) {
            val shiftName = data.effectiveShiftName ?: ""
            Text(
                modifier = GlanceModifier.defaultWeight(),
                text = shiftName,
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = getShiftTextColor(shiftName),
                    fontSize = (12 * scaleFactor).sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
