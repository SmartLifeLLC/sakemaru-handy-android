package biz.smt_life.android.core.domain.model

/**
 * Domain models for Incoming (入庫) feature.
 * Used by P10-P14 screens.
 */

data class IncomingWarehouse(
    val id: Int,
    val code: String,
    val name: String,
    val kanaName: String,
    val outOfStockOption: String
)

data class IncomingProduct(
    val itemId: Int,
    val itemCode: String,
    val itemName: String,
    val searchCode: String,
    val janCodes: List<String>,
    val volume: String?,
    val temperatureType: String?,
    val images: List<String>,
    val totalExpectedQuantity: Int,
    val totalReceivedQuantity: Int,
    val totalRemainingQuantity: Int,
    val warehouses: List<IncomingWarehouseSummary>,
    val schedules: List<IncomingSchedule>
)

data class IncomingWarehouseSummary(
    val warehouseId: Int,
    val warehouseCode: String,
    val warehouseName: String,
    val expectedQuantity: Int,
    val receivedQuantity: Int,
    val remainingQuantity: Int
)

data class IncomingSchedule(
    val id: Int,
    val warehouseId: Int,
    val warehouseName: String,
    val expectedQuantity: Int,
    val receivedQuantity: Int,
    val remainingQuantity: Int,
    val quantityType: String,
    val expectedArrivalDate: String,
    val status: IncomingScheduleStatus
)

enum class IncomingScheduleStatus {
    PENDING, PARTIAL, CONFIRMED, TRANSMITTED, CANCELLED;

    companion object {
        fun fromString(value: String): IncomingScheduleStatus =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }

    val isSelectable: Boolean
        get() = this == PENDING || this == PARTIAL
}

data class IncomingWorkItem(
    val id: Int,
    val incomingScheduleId: Int,
    val pickerId: Int,
    val warehouseId: Int,
    val locationId: Int?,
    val location: Location?,
    val workQuantity: Int,
    val workArrivalDate: String,
    val workExpirationDate: String?,
    val status: IncomingWorkStatus,
    val startedAt: String,
    val schedule: WorkItemSchedule?
)

data class WorkItemSchedule(
    val id: Int,
    val itemId: Int,
    val itemCode: String,
    val itemName: String,
    val warehouseId: Int,
    val warehouseName: String,
    val expectedQuantity: Int,
    val receivedQuantity: Int,
    val remainingQuantity: Int,
    val quantityType: String
)

enum class IncomingWorkStatus {
    WORKING, COMPLETED, CANCELLED;

    companion object {
        fun fromString(value: String): IncomingWorkStatus =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: WORKING
    }
}

data class Location(
    val id: Int,
    val code1: String,
    val code2: String,
    val code3: String,
    val name: String,
    val displayName: String
)
