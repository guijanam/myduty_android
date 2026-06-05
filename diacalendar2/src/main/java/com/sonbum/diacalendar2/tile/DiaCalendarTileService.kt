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

		// 구독 상태 확인
		val isPremium = sharedPrefs.getBoolean("isPremium", false)

		// 데이터 수신 여부 확인 (한번이라도 폰에서 데이터를 받았는지)
		val hasReceivedData = sharedPrefs.getBoolean("hasReceivedData", false)

		val shiftData = ShiftData(
			date = sharedPrefs.getString("date", "-") ?: "-",
			turn = sharedPrefs.getString("turn", "-") ?: "-",
			worktime = sharedPrefs.getString("worktime", "-") ?: "-",
			firsttime = sharedPrefs.getString("firsttime", "") ?: "",
			secondtime = sharedPrefs.getString("secondtime", "") ?: "",
			tableName = sharedPrefs.getString("tableName", "") ?: "",
			numtr1 = sharedPrefs.getString("numtr1", "") ?: "",
			numtr2 = sharedPrefs.getString("numtr2", "") ?: "",
		)

		// 레이아웃 결정 로직
		val layout = when {
			// 폰에서 데이터를 받은 적이 없으면 → 데모 모드
			!hasReceivedData -> createDemoLayout()
			// 비구독자 → 프리미엄 안내
			!isPremium -> createPremiumRequiredLayout()
			// 구독자 → 실제 데이터
			else -> createShiftLayout(shiftData)
		}

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

	// 데모 모드 레이아웃 (폰 연결 전 표시)
	private fun createDemoLayout(): LayoutElement {
		val demoData = ShiftData(
			date = "예시 화면",
			turn = "1",
			worktime = "07:00",
			firsttime = "06:00-14:00",
			secondtime = "17:00-18:00",
			tableName = "평일",
			numtr1 = "2222",
			numtr2 = "3333"
		)

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
							.setAll(dp(12f))
							.build()
					)
					.build()
			)
			.addContent(
				Text.Builder()
					.setText("미리보기")
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(10f))
							.setColor(argb(0xFFFFD700.toInt()))
							.build()
					)
					.build()
			)
			.addContent(
				Text.Builder()
					.setText(demoData.date)
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(14f))
							.setColor(argb(0xFFB0B0B0.toInt()))
							.build()
					)
					.build()
			)
			.addContent(
				Text.Builder()
					.setText("Dia: ${demoData.turn}")
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(18f))
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
			.addContent(
				Text.Builder()
					.setText("${demoData.worktime} (${demoData.tableName})")
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(17f))
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
			.addContent(
				Text.Builder()
					.setText("전반: ${demoData.firsttime}")
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(13f))
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
			.addContent(
				Text.Builder()
					.setText("폰 앱 연결 필요")
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(10f))
							.setColor(argb(0xFF808080.toInt()))
							.build()
					)
					.setModifiers(
						Modifiers.Builder()
							.setPadding(Padding.Builder().setTop(dp(6f)).build())
							.build()
					)
					.build()
			)
			.build()
	}

	// 비구독자용 레이아웃
	private fun createPremiumRequiredLayout(): LayoutElement {
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
							.setAll(dp(16f))
							.build()
					)
					.build()
			)
			.addContent(
				Text.Builder()
					.setText("👑")
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(32f))
							.build()
					)
					.build()
			)
			.addContent(
				Text.Builder()
					.setText("프리미엄 전용")
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(16f))
							.setWeight(FONT_WEIGHT_BOLD)
							.setColor(argb(0xFFFFD700.toInt()))
							.build()
					)
					.setModifiers(
						Modifiers.Builder()
							.setPadding(Padding.Builder().setTop(dp(12f)).build())
							.build()
					)
					.build()
			)
			.addContent(
				Text.Builder()
					.setText("구독 후 이용 가능")
					.setFontStyle(
						FontStyle.Builder()
							.setSize(sp(12f))
							.setColor(argb(0xFFB0B0B0.toInt()))
							.build()
					)
					.setModifiers(
						Modifiers.Builder()
							.setPadding(Padding.Builder().setTop(dp(8f)).build())
							.build()
					)
					.build()
			)
			.build()
	}

	// 구독자용 레이아웃
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
)
