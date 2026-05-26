package com.sonbum.diacalendar2.core.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

object DeviceIdProvider {

    @SuppressLint("HardwareIds")
    fun getSsaid(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
    }
}
