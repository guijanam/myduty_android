/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.sonbum.diacalendar2.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.sonbum.diacalendar2.tile.ShiftData
import com.sonbum.diacalendar2.presentation.theme.DiaCalendar2Theme

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()

		super.onCreate(savedInstanceState)

		setTheme(android.R.style.Theme_DeviceDefault)

		setContent {
			WearApp()
		}
	}
}

/** 타일과 동일한 SharedPreferences 에서 오늘 교번을 읽는다. 없으면 샘플. */
private fun loadShiftData(context: Context): Pair<ShiftData, Boolean> {
	val prefs = context.getSharedPreferences("shift_data", Context.MODE_PRIVATE)
	val hasReceivedData = prefs.getBoolean("hasReceivedData", false)
	val data = if (hasReceivedData) {
		ShiftData(
			date = prefs.getString("date", "-") ?: "-",
			turn = prefs.getString("turn", "-") ?: "-",
			worktime = prefs.getString("worktime", "-") ?: "-",
			firsttime = prefs.getString("firsttime", "") ?: "",
			secondtime = prefs.getString("secondtime", "") ?: "",
			tableName = prefs.getString("tableName", "") ?: "",
			numtr1 = prefs.getString("numtr1", "") ?: "",
			numtr2 = prefs.getString("numtr2", "") ?: "",
		)
	} else {
		ShiftData.sample()
	}
	return data to hasReceivedData
}

@Composable
fun WearApp() {
	val context = LocalContext.current
	val (shiftData, hasReceivedData) = loadShiftData(context)

	DiaCalendar2Theme {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colors.background),
			contentAlignment = Alignment.Center
		) {
			TimeText()
			ShiftCard(shiftData, isSample = !hasReceivedData)
		}
	}
}

@Composable
fun ShiftCard(data: ShiftData, isSample: Boolean) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(2.dp)
	) {
		Text(
			text = data.date,
			fontSize = 12.sp,
			color = Color(0xFFB0B0B0),
			textAlign = TextAlign.Center
		)
		Text(
			text = "Dia: ${data.turn}",
			fontSize = 16.sp,
			fontWeight = FontWeight.Bold,
			color = Color.White,
			textAlign = TextAlign.Center
		)
		val worktime = if (data.tableName.isNotEmpty()) "${data.worktime} (${data.tableName})" else data.worktime
		Text(
			text = worktime,
			fontSize = 15.sp,
			fontWeight = FontWeight.Bold,
			color = Color(0xFF4FC3F7),
			textAlign = TextAlign.Center
		)
		if (data.firsttime.isNotEmpty()) {
			Text(
				text = "전반: ${data.firsttime}",
				fontSize = 13.sp,
				color = Color(0xFF81C784),
				textAlign = TextAlign.Center
			)
		}
		if (data.numtr1.isNotEmpty()) {
			Text(
				text = data.numtr1,
				fontSize = 13.sp,
				color = Color.White,
				textAlign = TextAlign.Center
			)
		}
		if (data.secondtime.isNotEmpty()) {
			Text(
				text = "후반: ${data.secondtime}",
				fontSize = 13.sp,
				color = Color(0xFF81C784),
				textAlign = TextAlign.Center
			)
		}
		if (data.numtr2.isNotEmpty()) {
			Text(
				text = data.numtr2,
				fontSize = 13.sp,
				color = Color.White,
				textAlign = TextAlign.Center
			)
		}
		if (isSample) {
			Text(
				text = "예시 · 폰 앱 연결 시 실제 표시",
				fontSize = 10.sp,
				color = Color(0xFF808080),
				textAlign = TextAlign.Center,
				modifier = Modifier.padding(top = 4.dp)
			)
		}
	}
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
	WearApp()
}
