package com.sonbum.diacalendar2.core.routing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DeepLinkNavigator {
    private val _pendingRouteFlow = MutableStateFlow<String?>(null)
    val pendingRouteFlow = _pendingRouteFlow.asStateFlow()

    var pendingRoute: String?
        get() = _pendingRouteFlow.value
        set(value) { _pendingRouteFlow.value = value }

    fun consume() {
        _pendingRouteFlow.value = null
    }
}
