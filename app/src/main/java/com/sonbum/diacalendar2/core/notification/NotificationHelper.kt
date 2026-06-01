package com.sonbum.diacalendar2.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sonbum.diacalendar2.MainActivity
import com.sonbum.diacalendar2.R
import com.sonbum.diacalendar2.presentation.alarm.AlarmRingActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_MEMO = "memo_reminders"
        const val CHANNEL_SHIFT = "shift_reminders"
        const val CHANNEL_SHIFT_ALARM = "shift_alarm"  // 풀스크린 근무 알람
        const val CHANNEL_FCM = "fcm_messages"
        private const val MEMO_NOTIFICATION_BASE_ID = 10000
        private const val SHIFT_NOTIFICATION_BASE_ID = 20000
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

    /**
     * 풀스크린 근무 알람.
     * 백그라운드/잠금 상태에서 액티비티를 직접 실행할 수 없으므로(Android 10+ BAL 제한),
     * full-screen intent를 단 고우선순위 노티로 AlarmRingActivity를 기동한다.
     * 화면이 켜진 상태에서는 헤드업 노티로, 잠금/꺼짐 상태에서는 전체화면으로 나타난다.
     */
    fun showShiftAlarm(
        shiftName: String,
        dateString: String,
        slot: Int,
        sound: Boolean,
        vibrate: Boolean,
        snoozeMinutes: Int = 5
    ) {
        val notiId = notificationId(dateString, slot)
        val fullScreenIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(AlarmScheduler.EXTRA_SHIFT_NAME, shiftName)
            putExtra(AlarmScheduler.EXTRA_DATE_STRING, dateString)
            putExtra(AlarmScheduler.EXTRA_SLOT, slot)
            putExtra(AlarmScheduler.EXTRA_SOUND, sound)
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

        notificationManager().notify(notiId, notification)
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
