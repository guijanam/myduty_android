package com.sonbum.diacalendar2.domain.repository

interface SubscriptionRepository {
    suspend fun isSubscribed(): Boolean
    suspend fun restorePurchases(): Boolean
}
