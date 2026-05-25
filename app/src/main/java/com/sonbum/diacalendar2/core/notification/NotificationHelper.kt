package com.sonbum.diacalendar2.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sonbum.diacalendar2.MainActivity
import com.sonbum.diacalendar2.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_MEMO = "memo_reminders"
        const val CHANNEL_SHIFT = "shift_reminders"
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

        notificationManager.createNotificationChannel(memoChannel)
        notificationManager.createNotificationChannel(shiftChannel)
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

    fun showShiftNotification(shiftName: String, dateString: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_date", dateString)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            dateString.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SHIFT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("오늘 근무: $shiftName")
            .setContentText("곧 근무가 시작됩니다")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            SHIFT_NOTIFICATION_BASE_ID + dateString.hashCode().and(0xFFFF),
            notification
        )
    }
}
