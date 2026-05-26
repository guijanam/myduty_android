package com.sonbum.diacalendar2.domain.repository

interface SubscriptionRepository {
    suspend fun isSubscribed(): Boolean
    suspend fun restorePurchases(): Boolean

    // SSAID 기반 평생 무료 권한 (coworker_list.device_id 매칭)
    suspend fun isDeviceVip(deviceId: String): Boolean

    // RevenueCat 구독 OR SSAID 평생 무료
    suspend fun isVip(deviceId: String): Boolean
}
