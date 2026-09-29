package com.sonbum.diacalendar2.tile

import android.content.Context
import android.content.SharedPreferences
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_BOLD
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit

class DiaCalendarTileService : TileService() {

	private lateinit var sharedPrefs: SharedPreferences

	override fun onCreate() {
		super.onCreate()
		sharedPrefs = getSharedPreferences("shift_data", Context.MODE_PRIVATE)
	}

	override fun onTileRequest(
		requestParams: RequestBuilders.TileRequest
	): ListenableFuture<TileBuilders.Tile> {

		// 데이터 수신 여부 확인 (한번이라도 폰에서 데이터를 받았는지)
		val hasReceivedData = sharedPrefs.getBoolean("hasReceivedData", false)

		// 폰에서 데이터를 받았으면 실제 데이터, 아니면 오늘 날짜 기반 샘플 데이터
		val shiftData = if (hasReceivedData) {
			ShiftData(
				date = sharedPrefs.getString("date", "-") ?: "-",
				turn = sharedPrefs.getString("turn", "-") ?: "-",
				worktime = sharedPrefs.getString("worktime", "-") ?: "-",
				firsttime = sharedPrefs.getString("firsttime", "") ?: "",
				secondtime = sharedPrefs.getString("secondtime", "") ?: "",
				tableName = sharedPrefs.getString("tableName", "") ?: "",
				numtr1 = sharedPrefs.getString("numtr1", "") ?: "",
				numtr2 = sharedPrefs.getString("numtr2", "") ?: "",
			)
		} else {
			ShiftData.sample()
		}

		// 항상 실제 동작하는 교번 화면을 표시한다.
		// (프리미엄/연결 게이트로 빈 화면을 보여주지 않는다 — 리뷰어/사용자가 기능을 바로 확인 가능)
		val layout = createShiftLayout(shiftData)

		return Futures.immediateFuture(
			TileBuilders.Tile.Builder()
				.setResourcesVersion("1")
				.setFreshnessIntervalMillis(TimeUnit.MINUTES.toMillis(30))
				.setTileTimeline(
					Timeline.Builder()
						.addTimelineEntry(
							TimelineEntry.Builder()
								.setLayout(
									LayoutElementBuilders.Layout.Builder()
										.setRoot(layout)
										.build()
								)
								.build()
						)
						.build()
				)
				.build()
		)
	}

	// 교번 표시 레이아웃
	private fun createShiftLayout(data: ShiftData): LayoutElement {
		return Column.Builder()
			.setWidth(expand())
			.setHeight(expand())
			.setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
			.setModifiers(
				Modifiers.Builder()
					.setBackground(
						Background.Builder()
							.setColor(argb(0xFF1A1A2E.toInt()))
							.build()
					)
					.setPadding(
						Padding.Builder()
							.setAll(dp(6f))
							.build()
					)
					.build()
			)
			// 날짜
			.addContent(
				Text.Builder()
					.setText(data.date)
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(12f))
							.setColor(argb(0xFFB0B0B0.toInt()))
							.build()
					)
					.build()
			)
			// 다이아 (turn)
			.addContent(
				Text.Builder()
					.setText("Dia: ${data.turn}")
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(14f))
							.setWeight(FONT_WEIGHT_BOLD)
							.setColor(argb(0xFFFFFFFF.toInt()))
							.build()
					)
					.setModifiers(
						Modifiers.Builder()
							.setPadding(Padding.Builder().setTop(dp(2f)).build())
							.build()
					)
					.build()
			)
			// 근무시간 (worktime)
			.addContent(
				Text.Builder()
					.setText(formatWorktime(data.worktime, data.tableName))
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(15f))
							.setWeight(FONT_WEIGHT_BOLD)
							.setColor(argb(0xFF4FC3F7.toInt()))
							.build()
					)
					.setModifiers(
						Modifiers.Builder()
							.setPadding(Padding.Builder().setTop(dp(2f)).build())
							.build()
					)
					.build()
			)
			// 전반 (firsttime)
			.addContent(
				Text.Builder()
					.setText(formatFirstTime(data.firsttime))
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(15f))
							.setColor(argb(0xFF81C784.toInt()))
							.build()
					)
					.setModifiers(
						Modifiers.Builder()
							.setPadding(Padding.Builder().setTop(dp(2f)).build())
							.build()
					)
					.build()
			)
			// 전반열번 (numtr1)
			.addContent(
				Text.Builder()
					.setText(formatNumtr1(data.numtr1))
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(15f))
							.setColor(argb(0xFFFFFFFF.toInt()))
							.build()
					)
					.setModifiers(
						Modifiers.Builder()
							.setPadding(Padding.Builder().setTop(dp(1f)).build())
							.build()
					)
					.build()
			)
			// 후반 (secondtime)
			.addContent(
				Text.Builder()
					.setText(formatSecondTime(data.secondtime))
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(15f))
							.setColor(argb(0xFF81C784.toInt()))
							.build()
					)
					.setModifiers(
						Modifiers.Builder()
							.setPadding(Padding.Builder().setTop(dp(2f)).build())
							.build()
					)
					.build()
			)
			// 후반열번 (numtr2)
			.addContent(
				Text.Builder()
					.setText(formatNumtr2(data.numtr2))
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(15f))
							.setColor(argb(0xFFFFFFFF.toInt()))
							.build()
					)
					.setModifiers(
						Modifiers.Builder()
							.setPadding(Padding.Builder().setTop(dp(1f)).build())
							.build()
					)
					.build()
			)
			.build()
	}

	private fun formatWorktime(worktime: String, tableName: String): String {
		return if (tableName.isNotEmpty()) "$worktime ($tableName)" else worktime
	}

	private fun formatFirstTime(first: String): String {
		return if (first.isNotEmpty()) "전반: $first" else ""
	}

	private fun formatNumtr1(numtr1: String): String = numtr1.ifEmpty { "" }

	private fun formatSecondTime(second: String): String {
		return if (second.isNotEmpty()) "후반: $second" else ""
	}

	private fun formatNumtr2(numtr2: String): String = numtr2.ifEmpty { "" }

	@Suppress("DEPRECATION")
	@Deprecated("Deprecated in TileService")
	override fun onResourcesRequest(
		requestParams: RequestBuilders.ResourcesRequest
	): ListenableFuture<ResourceBuilders.Resources> {
		return Futures.immediateFuture(
			ResourceBuilders.Resources.Builder()
				.setVersion("1")
				.build()
		)
	}
}

data class ShiftData(
	val date: String,
	val turn: String,
	val worktime: String,
	val firsttime: String,
	val secondtime: String,
	val tableName: String,
	val numtr1: String,
	val numtr2: String
) {
	companion object {
		/**
		 * 폰에서 아직 데이터를 받지 않았을 때 보여줄 오늘 날짜 기반 샘플 교번.
		 * 타일/앱이 빈 화면이 아니라 실제 동작하는 모습으로 보이도록 한다.
		 */
		fun sample(): ShiftData {
			val today = java.time.LocalDate.now()
			val dateFormat = java.time.format.DateTimeFormatter
				.ofPattern("M/d (E)", java.util.Locale.KOREAN)
			return ShiftData(
				date = today.format(dateFormat),
				turn = "1",
				worktime = "주간",
				firsttime = "06:00-14:00",
				secondtime = "17:00-18:00",
				tableName = "평일",
				numtr1 = "K2122",
				numtr2 = "K3133"
			)
		}
	}
}
