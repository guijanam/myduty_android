package com.sonbum.diacalendar2.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class BirthdayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BirthdayWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MidnightWidgetWorker.scheduleNextMidnightUpdate(context)
    }
}
