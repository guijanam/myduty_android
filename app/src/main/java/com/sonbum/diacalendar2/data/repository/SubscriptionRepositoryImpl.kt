package com.sonbum.diacalendar2.data.repository

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.sonbum.diacalendar2.data.local.datastore.VipPreferences
import com.sonbum.diacalendar2.data.remote.SupabaseConfig
import com.sonbum.diacalendar2.data.remote.api.SupabaseApi
import com.sonbum.diacalendar2.domain.repository.SubscriptionRepository
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SubscriptionRepositoryImpl(
    private val supabaseApi: SupabaseApi,
    private val vipPreferences: VipPreferences
) : SubscriptionRepository {

    override suspend fun isSubscribed(): Boolean = suspendCoroutine { cont ->
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                cont.resume(customerInfo.entitlements["coffee"]?.isActive == true)
            }
            override fun onError(error: PurchasesError) {
                cont.resume(false)
            }
        })
    }

    override suspend fun restorePurchases(): Boolean = suspendCoroutine { cont ->
        Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                cont.resume(customerInfo.entitlements["coffee"]?.isActive == true)
            }
            override fun onError(error: PurchasesError) {
                cont.resume(false)
            }
        })
    }

    override suspend fun isDeviceVip(deviceId: String): Boolean {
        if (deviceId.isBlank()) return false

        val (cached, lastChecked) = vipPreferences.readSnapshot()
        if (cached != null) {
            val ttl = if (cached) VipPreferences.CACHE_TTL_VIP_MS
                      else        VipPreferences.CACHE_TTL_NON_VIP_MS
            if (System.currentTimeMillis() - lastChecked < ttl) {
                return cached
            }
        }

        return try {
            val result = supabaseApi.checkDeviceVip(
                apiKey = SupabaseConfig.apiKey,
                authorization = "Bearer ${SupabaseConfig.apiKey}",
                body = mapOf("p_device_id" to deviceId)
            )
            vipPreferences.saveDeviceVipStatus(result)
            result
        } catch (e: Exception) {
            cached ?: false
        }
    }

    override suspend fun refreshVipStatus(deviceId: String): Boolean {
        vipPreferences.clearCache()
        return isDeviceVip(deviceId)
    }

    override suspend fun isVip(deviceId: String): Boolean {
        if (isSubscribed()) return true
        return isDeviceVip(deviceId)
    }
}
