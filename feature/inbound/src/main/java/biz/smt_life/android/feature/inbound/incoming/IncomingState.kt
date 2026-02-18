package biz.smt_life.android.feature.inbound.incoming

import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.Location

/**
 * Shared state for all incoming screens (P10-P14).
 */
data class IncomingState(
    // Session
    val pickerId: Int? = null,
    val pickerName: String? = null,

    // P10: Warehouse selection
    val warehouses: List<IncomingWarehouse> = emptyList(),
    val selectedWarehouse: IncomingWarehouse? = null,
    val isLoadingWarehouses: Boolean = false,

    // P11: Product list
    val products: List<IncomingProduct> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val workingScheduleIds: Set<Int> = emptySet(),

    // P12: Schedule list
    val selectedProduct: IncomingProduct? = null,

    // P13: Input
    val selectedSchedule: IncomingSchedule? = null,
    val currentWorkItem: IncomingWorkItem? = null,
    val isFromHistory: Boolean = false,
    val inputQuantity: String = "",
    val inputExpirationDate: String = "",
    val inputLocationSearch: String = "",
    val inputLocationId: Int? = null,
    val inputLocation: Location? = null,
    val locationSuggestions: List<Location> = emptyList(),
    val isLoadingLocations: Boolean = false,
    val isSubmitting: Boolean = false,

    // P14: History
    val historyItems: List<IncomingWorkItem> = emptyList(),
    val isLoadingHistory: Boolean = false,

    // Common
    val errorMessage: String? = null,
    val successMessage: String? = null
)
