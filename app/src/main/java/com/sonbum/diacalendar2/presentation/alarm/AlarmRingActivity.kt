package com.sonbum.diacalendar2.presentation.alarm

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonbum.diacalendar2.core.notification.AlarmScheduler
import com.sonbum.diacalendar2.core.notification.NotificationHelper
import com.sonbum.diacalendar2.ui.theme.DiaCalendar2Theme
import org.koin.android.ext.android.inject

/**
 * 풀스크린 근무 알람 화면.
 * - 화면이 꺼져 있거나 잠금 상태여도 화면을 켜고(잠금화면 위) 전체를 덮는다.
 * - 알람음 루프 + 진동, "해제"를 눌러야 멈춘다.
 * - 재부팅 후에도 BootReceiver가 알람을 재등록하므로 동일하게 동작한다.
 */
class AlarmRingActivity : ComponentActivity() {

    private val notificationHelper: NotificationHelper by inject()
    private val alarmScheduler: AlarmScheduler by inject()

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var dateString: String = ""
    private var shiftName: String = ""
    private var slot: Int = AlarmScheduler.SLOT_COMMUTE
    private var sound: Boolean = true
    private var vibrate: Boolean = true
    private var snoozeMinutes: Int = 5

    private val autoStopHandler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { dismissAndFinish() }

    companion object {
        private const val AUTO_STOP_MILLIS = 5 * 60 * 1000L  // 5분 후 자동 정지
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()

        shiftName = intent.getStringExtra(AlarmScheduler.EXTRA_SHIFT_NAME) ?: ""
        dateString = intent.getStringExtra(AlarmScheduler.EXTRA_DATE_STRING) ?: ""
        slot = intent.getIntExtra(AlarmScheduler.EXTRA_SLOT, AlarmScheduler.SLOT_COMMUTE)
        sound = intent.getBooleanExtra(AlarmScheduler.EXTRA_SOUND, true)
        vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true)
        snoozeMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 5)

        if (sound) startAlarmSound()
        if (vibrate) startVibration()

        // 5분간 끄지 않으면 자동 정지 (무한 루프 방지)
        autoStopHandler.postDelayed(autoStopRunnable, AUTO_STOP_MILLIS)

        setContent {
            DiaCalendar2Theme {
                AlarmRingScreen(
                    title = slotLabel(slot),
                    shiftName = shiftName,
                    dateString = dateString,
                    snoozeMinutes = snoozeMinutes,
                    onDismiss = { dismissAndFinish() },
                    onSnooze = { snoozeAndFinish() }
                )
            }
        }
    }

    private fun slotLabel(slot: Int): String = when (slot) {
        AlarmScheduler.SLOT_FIRST -> "전반사업 알람"
        AlarmScheduler.SLOT_SECOND -> "후반사업 알람"
        else -> "출근 알람"
    }

    /** 화면 꺼짐/잠금 상태에서도 켜고 잠금화면 위로 표시 */
    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startAlarmSound() {
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                play()
            }
        } catch (_: Exception) {
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        val pattern = longArrayOf(0, 1000, 1000) // 대기0, 진동1s, 멈춤1s 반복
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun dismissAndFinish() {
        autoStopHandler.removeCallbacks(autoStopRunnable)
        stopAlarm()
        notificationHelper.cancelShiftNotification(dateString, slot)
        finish()
    }

    /** N분 뒤 같은 알람을 다시 등록하고 종료 */
    private fun snoozeAndFinish() {
        autoStopHandler.removeCallbacks(autoStopRunnable)
        stopAlarm()
        notificationHelper.cancelShiftNotification(dateString, slot)

        val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
        alarmScheduler.scheduleShiftAlarm(
            dateString = dateString,
            shiftName = shiftName,
            triggerAtMillis = triggerAt,
            slot = slot,
            fullScreen = true,
            sound = sound,
            vibrate = vibrate
        )
        finish()
    }

    override fun onDestroy() {
        autoStopHandler.removeCallbacks(autoStopRunnable)
        stopAlarm()
        super.onDestroy()
    }
}

@Composable
private fun AlarmRingScreen(
    title: String,
    shiftName: String,
    dateString: String,
    snoozeMinutes: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (shiftName.isNotBlank()) "오늘 근무: $shiftName" else "곧 근무가 시작됩니다",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (dateString.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = dateString,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("알람 해제", fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("${snoozeMinutes}분 후 다시 알림", fontSize = 18.sp)
            }
        }
    }
}
