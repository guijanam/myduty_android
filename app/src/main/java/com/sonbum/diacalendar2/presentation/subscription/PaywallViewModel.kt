package com.sonbum.diacalendar2.presentation.subscription

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.SubscriptionOption
import com.sonbum.diacalendar2.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaywallUiState(
    val isLoading: Boolean = true,
    val availablePackage: Package? = null,
    val freeTrialOption: SubscriptionOption? = null,   // free1month offer
    val priceText: String = "",
    val hasFreeTrial: Boolean = false,
    val errorMessage: String? = null,
    val isRestoring: Boolean = false
)

sealed interface PaywallEvent {
    data object SubscriptionSuccess : PaywallEvent
    data class Error(val message: String) : PaywallEvent
}

class PaywallViewModel(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<PaywallEvent>()
    val event = _event.asSharedFlow()

    init {
        loadOfferings()
    }

    private fun loadOfferings() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                val pkg = offerings.current?.availablePackages?.firstOrNull()
                val price = pkg?.product?.price?.formatted ?: ""

                // 모든 옵션 상세 로그
                pkg?.product?.subscriptionOptions?.forEachIndexed { i, opt ->
                    Log.d("Paywall", "option[$i] id=${opt.id} tags=${opt.tags}")
                    opt.pricingPhases.forEachIndexed { j, phase ->
                        Log.d("Paywall", "  phase[$j] amountMicros=${phase.price.amountMicros} period=${phase.billingPeriod} recurrence=${phase.recurrenceMode}")
                    }
                }

                val defaultOption = pkg?.product?.defaultOption
                Log.d("Paywall", "defaultOption=${defaultOption?.id} freePhase=${defaultOption?.freePhase} introPhase=${defaultOption?.introPhase}")

                val hasFreeTrial = defaultOption?.freePhase != null || defaultOption?.introPhase != null

                _state.update {
                    it.copy(
                        isLoading = false,
                        availablePackage = pkg,
                        freeTrialOption = defaultOption,
                        priceText = price,
                        hasFreeTrial = hasFreeTrial
                    )
                }
            }
            override fun onError(error: PurchasesError) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = error.message)
                }
            }
        })
    }

    fun onPurchaseSuccess() {
        viewModelScope.launch {
            _event.emit(PaywallEvent.SubscriptionSuccess)
        }
    }

    fun onPurchaseError(message: String) {
        viewModelScope.launch {
            _event.emit(PaywallEvent.Error(message))
        }
    }

    fun restorePurchases() {
        _state.update { it.copy(isRestoring = true) }
        viewModelScope.launch {
            val success = subscriptionRepository.restorePurchases()
            _state.update { it.copy(isRestoring = false) }
            if (success) {
                _event.emit(PaywallEvent.SubscriptionSuccess)
            } else {
                _event.emit(PaywallEvent.Error("복원할 수 있는 구독이 없습니다"))
            }
        }
    }
}
