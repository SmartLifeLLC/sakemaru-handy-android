package biz.smt_life.android.feature.main

import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.PendingCounts
import biz.smt_life.android.core.domain.model.Warehouse

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(
        val pickerCode: String?,
        val pickerName: String?,
        val warehouse: Warehouse,
        val pendingCounts: PendingCounts,
        val currentDate: String,
        val shippingDate: String,
        val hostUrl: String,
        val appVersion: String,
        val showWarehouseDialog: Boolean = false,
        val availableWarehouses: List<IncomingWarehouse> = emptyList(),
        val isLoadingWarehouses: Boolean = false,
        val showDatePicker: Boolean = false
    ) : MainUiState

    data class Error(
        val message: String
    ) : MainUiState
}
