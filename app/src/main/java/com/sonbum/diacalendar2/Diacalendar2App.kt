package com.sonbum.diacalendar2

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.sonbum.diacalendar2.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class Diacalendar2App : Application() {

    companion object {
        lateinit var instance: Diacalendar2App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Koin 초기화
        startKoin {
            androidLogger(Level.NONE) // 릴리즈 시 Level.NONE으로 변경
            androidContext(this@Diacalendar2App)
            modules(appModules)
        }

        // RevenueCat 초기화
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(
            PurchasesConfiguration.Builder(this, "goog_IaRZhJLijfECqxDstYcTWuWBpBw").build()
        )
    }
}
