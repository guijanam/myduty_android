package com.sonbum.diacalendar2.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sonbum.diacalendar2.MainActivity
import com.sonbum.diacalendar2.domain.repository.BirthdayRepository
import com.sonbum.diacalendar2.domain.util.AgeCalculator
import com.sonbum.diacalendar2.domain.util.BirthdayDateResolver
import org.koin.java.KoinJavaComponent.getKoin
import java.time.temporal.ChronoUnit

data class BirthdayWidgetItem(val name: String, val ageText: String, val daysUntil: Long)

class BirthdayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = loadItems()
        provideContent {
            val prefs = currentState<Preferences>()
            val updated = prefs[longPreferencesKey("birthday_last_updated")] ?: 0L
            val items by produceState(initialValue = initial, key1 = updated) { value = loadItems() }
            GlanceTheme { BirthdayWidgetContent(items) }
        }
    }

    private suspend fun loadItems(): List<BirthdayWidgetItem> = runCatching {
        val koin = getKoin()
        val repository = koin.get<BirthdayRepository>()
        val resolver = koin.get<BirthdayDateResolver>()
        val ages = koin.get<AgeCalculator>()
        repository.getPeopleOnce().mapNotNull { person ->
            val today = ages.todayFor(person)
            val next = resolver.nextBirthday(person, today) ?: return@mapNotNull null
            BirthdayWidgetItem(person.name, ages.displayText(person), ChronoUnit.DAYS.between(today, next))
        }.sortedBy { it.daysUntil }.take(4)
    }.getOrDefault(emptyList())
}

@Composable
private fun BirthdayWidgetContent(items: List<BirthdayWidgetItem>) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.background)
            .padding(12.dp).clickable(
                actionStartActivity(
                    Intent(androidx.glance.LocalContext.current, MainActivity::class.java).apply {
                        putExtra("navigate_to_birthdays", true)
                    }
                )
            ),
        verticalAlignment = Alignment.Top
    ) {
        Text("🎂 다가오는 생일", style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary, fontSize = 16.sp))
        Spacer(GlanceModifier.height(6.dp))
        if (items.isEmpty()) {
            Text("등록된 생일이 없습니다", style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 13.sp))
        } else {
            items.forEach { item ->
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("${item.name} · ${item.ageText}", modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 13.sp))
                    Text(if (item.daysUntil == 0L) "오늘" else "D-${item.daysUntil}", style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.primary, fontSize = 13.sp))
                }
            }
        }
    }
}
