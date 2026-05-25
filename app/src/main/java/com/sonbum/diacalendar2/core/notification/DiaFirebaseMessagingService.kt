package com.sonbum.diacalendar2.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sonbum.diacalendar2.MainActivity
import com.sonbum.diacalendar2.R

class DiaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "DiaFCM"
        const val CHANNEL_FCM = "fcm_messages"
        private const val FCM_NOTIFICATION_BASE_ID = 50000
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "From: ${message.from}")

        val title = message.notification?.title ?: message.data["title"] ?: "DiaCalendar"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val dateString = message.data["date"]

        showFcmNotification(title, body, dateString)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
    }

    private fun showFcmNotification(title: String, body: String, dateString: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (dateString != null) {
                putExtra("navigate_to_date", dateString)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_FCM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            FCM_NOTIFICATION_BASE_ID + System.currentTimeMillis().toInt().and(0xFFFF),
            notification
        )
    }
}
