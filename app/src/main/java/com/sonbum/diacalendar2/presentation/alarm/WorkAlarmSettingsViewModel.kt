package com.sonbum.diacalendar2.presentation.alarm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.core.notification.ShiftReminderWorker
import com.sonbum.diacalendar2.data.local.datastore.NotificationPreferences
import com.sonbum.diacalendar2.data.local.datastore.WorkAlarmPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkAlarmSettingsViewModel(
    private val prefs: NotificationPreferences,
    private val appContext: Context
) : ViewModel() {

    val state: StateFlow<WorkAlarmPrefs> = prefs.workAlarmPrefs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WorkAlarmPrefs()
        )

    fun setCommute(enabled: Boolean, minutesBefore: Int) = save {
        prefs.setCommute(enabled, minutesBefore)
    }

    fun setFirst(enabled: Boolean, minutesBefore: Int) = save {
        prefs.setFirst(enabled, minutesBefore)
    }

    fun setSecond(enabled: Boolean, minutesBefore: Int) = save {
        prefs.setSecond(enabled, minutesBefore)
    }

    fun setIntensity(fullScreen: Boolean, sound: Boolean, vibrate: Boolean) = save {
        prefs.setIntensity(fullScreen, sound, vibrate)
    }

    /** 저장 후 즉시 알람을 재등록 (설정 → 동작 연결고리) */
    private inline fun save(crossinline block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            ShiftReminderWorker.enqueue(appContext)
        }
    }
}
