package com.sonbum.diacalendar2.data.repository

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.sonbum.diacalendar2.domain.repository.SubscriptionRepository
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SubscriptionRepositoryImpl : SubscriptionRepository {

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
}
