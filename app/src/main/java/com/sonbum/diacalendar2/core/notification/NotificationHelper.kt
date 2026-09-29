package com.sonbum.diacalendar2.core.notification

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sonbum.diacalendar2.MainActivity
import com.sonbum.diacalendar2.R
import com.sonbum.diacalendar2.presentation.alarm.AlarmRingActivity

internal enum class ShiftAlarmPresentation {
    SIMPLE_NOTIFICATION,
    FULL_SCREEN_NOTIFICATION,
    FULL_SCREEN_ACTIVITY
}

internal fun resolveShiftAlarmPresentation(
    fullScreenEnabled: Boolean,
    isInteractive: Boolean,
    isKeyguardLocked: Boolean,
    canDrawOverlays: Boolean
): ShiftAlarmPresentation = when {
    !fullScreenEnabled -> ShiftAlarmPresentation.SIMPLE_NOTIFICATION
    isInteractive && !isKeyguardLocked && canDrawOverlays ->
        ShiftAlarmPresentation.FULL_SCREEN_ACTIVITY
    else -> ShiftAlarmPresentation.FULL_SCREEN_NOTIFICATION
}

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_MEMO = "memo_reminders"
        const val CHANNEL_SHIFT = "shift_reminders"
        const val CHANNEL_SHIFT_ALARM = "shift_alarm"  // 풀스크린 근무 알람
        const val CHANNEL_FCM = "fcm_messages"
        const val CHANNEL_BIRTHDAY = "birthday_reminders"
        private const val MEMO_NOTIFICATION_BASE_ID = 10000
        private const val SHIFT_NOTIFICATION_BASE_ID = 20000
        private const val TAG = "NotificationHelper"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val memoChannel = NotificationChannel(
            CHANNEL_MEMO,
            "메모 알림",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "메모 리마인더 알림"
        }

        val shiftChannel = NotificationChannel(
            CHANNEL_SHIFT,
            "교번 알림",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "근무 시작 전 알림"
        }

        val fcmChannel = NotificationChannel(
            CHANNEL_FCM,
            "공지 알림",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "개발자 공지 및 업데이트 알림"
        }

        val birthdayChannel = NotificationChannel(
            CHANNEL_BIRTHDAY,
            "생일과 주요 나이 알림",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "생일 및 환갑·칠순 등 주요 나이 알림" }

        // 풀스크린 근무 알람 채널: 소리/진동은 액티비티가 직접 제어하므로 채널은 무음 처리
        val shiftAlarmChannel = NotificationChannel(
            CHANNEL_SHIFT_ALARM,
            "근무 알람 (전체화면)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "잠금화면 위로 화면을 깨우는 근무 알람"
            setSound(null, null)
            enableVibration(false)
        }

        notificationManager.createNotificationChannel(memoChannel)
        notificationManager.createNotificationChannel(shiftChannel)
        notificationManager.createNotificationChannel(shiftAlarmChannel)
        notificationManager.createNotificationChannel(fcmChannel)
        notificationManager.createNotificationChannel(birthdayChannel)
    }

    fun showBirthdayNotification(
        personId: Long,
        personName: String,
        eventName: String,
        eventDate: String,
        daysBefore: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_birthdays", true)
            putExtra("birthday_person_id", personId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, personId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val content = when {
            daysBefore == 0 -> "$personName 님의 $eventName 당일입니다"
            else -> "$personName 님의 ${eventName}까지 ${daysBefore}일 남았습니다"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_BIRTHDAY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎂 $eventName 알림")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$content · $eventDate"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager().notify(300_000 + personId.hashCode().and(0xFFFF), notification)
    }

    fun showMemoNotification(title: String, content: String, memoId: String, dateString: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_date", dateString)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            memoId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MEMO)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content.ifEmpty { "메모 알림" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            MEMO_NOTIFICATION_BASE_ID + memoId.hashCode().and(0xFFFF),
            notification
        )
    }

    /** 근무 알람을 기기 상태와 권한에 맞는 방식으로 표시한다. */
    fun showShiftAlarm(
        shiftName: String,
        dateString: String,
        slot: Int,
        fullScreen: Boolean,
        sound: Boolean,
        soundUri: String?,
        vibrate: Boolean,
        snoozeMinutes: Int = 5
    ) {
        val presentation = if (fullScreen) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val keyguardManager =
                context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            resolveShiftAlarmPresentation(
                fullScreenEnabled = true,
                isInteractive = powerManager.isInteractive,
                isKeyguardLocked = keyguardManager.isKeyguardLocked,
                canDrawOverlays = Settings.canDrawOverlays(context)
            )
        } else {
            ShiftAlarmPresentation.SIMPLE_NOTIFICATION
        }

        if (presentation == ShiftAlarmPresentation.SIMPLE_NOTIFICATION) {
            showShiftNotificationSimple(shiftName, dateString, slot)
            return
        }

        val notiId = notificationId(dateString, slot)
        val fullScreenIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_SHIFT_NAME, shiftName)
            putExtra(AlarmScheduler.EXTRA_DATE_STRING, dateString)
            putExtra(AlarmScheduler.EXTRA_SLOT, slot)
            putExtra(AlarmScheduler.EXTRA_SOUND, sound)
            putExtra(AlarmScheduler.EXTRA_SOUND_URI, soundUri)
            putExtra(AlarmScheduler.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notiId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SHIFT_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(slotTitle(slot, shiftName))
            .setContentText("곧 ${slotLabel(slot)} 시간입니다")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        // 직접 실행이 제조사 정책 등으로 차단되더라도 알람 전달 경로가 남도록 먼저 게시한다.
        notificationManager().notify(notiId, notification)

        if (presentation == ShiftAlarmPresentation.FULL_SCREEN_ACTIVITY) {
            try {
                context.startActivity(fullScreenIntent)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Unable to launch full-screen shift alarm activity", e)
            }
        }
    }

    /** 풀스크린 옵션 OFF일 때: 소리 나는 일반(헤드업) 노티 */
    fun showShiftNotificationSimple(shiftName: String, dateString: String, slot: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_date", dateString)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId(dateString, slot),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SHIFT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(slotTitle(slot, shiftName))
            .setContentText("곧 ${slotLabel(slot)} 시간입니다")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager().notify(notificationId(dateString, slot), notification)
    }

    /** 풀스크린 알람 해제 시 트레이의 알람 노티 제거 */
    fun cancelShiftNotification(dateString: String, slot: Int) {
        notificationManager().cancel(notificationId(dateString, slot))
    }

    private fun notificationManager(): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun notificationId(dateString: String, slot: Int): Int =
        SHIFT_NOTIFICATION_BASE_ID + slot * 100000 + dateString.hashCode().and(0xFFFF)

    private fun slotLabel(slot: Int): String = when (slot) {
        1 -> "전반사업"
        2 -> "후반사업"
        else -> "출근"
    }

    private fun slotTitle(slot: Int, shiftName: String): String {
        val label = slotLabel(slot)
        return if (shiftName.isNotBlank()) "$label 알람 ($shiftName)" else "$label 알람"
    }
}
