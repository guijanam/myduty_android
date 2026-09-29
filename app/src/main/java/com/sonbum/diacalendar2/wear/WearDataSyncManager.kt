package com.sonbum.diacalendar2.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 폰 → 워치(Wear OS 타일) 오늘 교번 데이터 전송 매니저.
 * Data Layer API(/today_shift)로 PutDataMapRequest 를 전송한다.
 * 키 스키마는 워치 [DataListenerService] / [DiaCalendarTileService] 와 동일하게 유지한다.
 */
class WearDataSyncManager(private val context: Context) {

	companion object {
		private const val TAG = "WearDataSync"
		private const val PATH_TODAY_SHIFT = "/today_shift"
	}

	private val dataClient by lazy { Wearable.getDataClient(context) }
	private val nodeClient by lazy { Wearable.getNodeClient(context) }

	suspend fun checkConnectedNodes(): Boolean {
		return try {
			val nodes = nodeClient.connectedNodes.await()
			Log.d(TAG, "연결된 노드 수: ${nodes.size}")
			nodes.isNotEmpty()
		} catch (e: Exception) {
			Log.e(TAG, "노드 확인 실패", e)
			false
		}
	}

	suspend fun syncTodayShift(
		date: LocalDate,
		turn: String?,
		worktime: String?,
		firsttime: String?,
		secondtime: String?,
		tableName: String?,
		numtr1: String?,
		numtr2: String?,
		isPremium: Boolean
	) {
		Log.d(TAG, "📤 syncTodayShift 호출됨 turn=$turn worktime=$worktime isPremium=$isPremium")

		val dateFormat = DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN)

		val request = PutDataMapRequest.create(PATH_TODAY_SHIFT).apply {
			dataMap.apply {
				putBoolean("isPremium", isPremium)
				putString("date", date.format(dateFormat))
				putString("turn", turn ?: "-")
				putString("worktime", worktime ?: "-")
				putString("firsttime", firsttime ?: "")
				putString("secondtime", secondtime ?: "")
				putString("tableName", tableName ?: "")
				putString("numtr1", numtr1 ?: "")
				putString("numtr2", numtr2 ?: "")
				putLong("timestamp", System.currentTimeMillis())
			}
		}.asPutDataRequest().setUrgent()

		try {
			val result = dataClient.putDataItem(request).await()
			Log.d(TAG, "✅ 데이터 전송 성공: ${result.uri}, isPremium: $isPremium")
		} catch (e: Exception) {
			Log.e(TAG, "❌ 워치 동기화 실패", e)
		}
	}
}
