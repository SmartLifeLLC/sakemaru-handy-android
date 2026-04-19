package biz.smt_life.android.feature.inbound.incoming

import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.Location

data class IncomingState(
    val pickerId: Int? = null,
    val pickerName: String? = null,
    val warehouses: List<IncomingWarehouse> = emptyList(),
    val selectedWarehouse: IncomingWarehouse? = null,
    val isLoadingWarehouses: Boolean = false,
    val products: List<IncomingProduct> = emptyList(),
    val searchQuery: String = "",
    val selectedProductIndex: Int = 0,
    val workingScheduleIds: Set<Int> = emptySet(),
    val isLoadingProducts: Boolean = false,
    val isSearching: Boolean = false,
    val selectedProduct: IncomingProduct? = null,
    val selectedScheduleIndex: Int = 0,
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
    val historyItems: List<IncomingWorkItem> = emptyList(),
    val selectedHistoryIndex: Int = 0,
    val isLoadingHistory: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
