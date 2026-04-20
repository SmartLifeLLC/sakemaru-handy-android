package biz.smt_life.android.feature.outbound.proxyshipment

import biz.smt_life.android.core.domain.model.ProxyShipmentCompletionResult

data class ProxyShipmentResultState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val completion: ProxyShipmentCompletionResult? = null
)
