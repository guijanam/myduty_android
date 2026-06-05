package com.sonbum.diacalendar2

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.sonbum.diacalendar2.tile.DiaCalendarTileService

class DataListenerService : WearableListenerService() {

	companion object {
		private const val TAG = "DataListenerService"
	}

	override fun onCreate() {
		super.onCreate()
		Log.d(TAG, "✅ DataListenerService 생성됨")
	}

	override fun onDataChanged(dataEvents: DataEventBuffer) {
		Log.d(TAG, "✅ onDataChanged 호출됨, 이벤트 수: ${dataEvents.count}")

		dataEvents.forEach { event ->
			if (event.type == DataEvent.TYPE_CHANGED) {
				val uri = event.dataItem.uri

				if (uri.path == "/today_shift") {
					val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

					val isPremium = dataMap.getBoolean("isPremium", false)
					val date = dataMap.getString("date")
					val turn = dataMap.getString("turn")
					val worktime = dataMap.getString("worktime")
					val firsttime = dataMap.getString("firsttime")
					val secondtime = dataMap.getString("secondtime")
					val tableName = dataMap.getString("tableName")
					val numtr1 = dataMap.getString("numtr1")
					val numtr2 = dataMap.getString("numtr2")

					getSharedPreferences("shift_data", Context.MODE_PRIVATE).edit {
						putBoolean("hasReceivedData", true)
						putBoolean("isPremium", isPremium)
						putString("date", date)
						putString("turn", turn)
						putString("worktime", worktime)
						putString("firsttime", firsttime)
						putString("secondtime", secondtime)
						putString("tableName", tableName)
						putString("numtr1", numtr1)
						putString("numtr2", numtr2)
					}

					Log.d(TAG, "✅ SharedPreferences 저장 완료")

					// Tile 갱신 요청
					TileService.getUpdater(this)
						.requestUpdate(DiaCalendarTileService::class.java)
				}
			}
		}
	}
}
