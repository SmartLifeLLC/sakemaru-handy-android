package biz.smt_life.android.core.domain.model

data class IncomingWarehouse(
    val id: Int,
    val code: String,
    val name: String,
    val kanaName: String? = null,
    val outOfStockOption: String? = null
)

data class Location(
    val id: Int,
    val code1: String? = null,
    val code2: String? = null,
    val code3: String? = null,
    val name: String? = null,
    val displayName: String? = null
) {
    val fullDisplayName: String
        get() = displayName ?: listOfNotNull(code1, code2, code3).joinToString("-")
}

data class IncomingProduct(
    val itemId: Int,
    val itemCode: String,
    val itemName: String,
    val searchCode: String? = null,
    val janCodes: List<String> = emptyList(),
    val volume: String? = null,
    val volumeUnit: String? = null,
    val capacityCase: Int? = null,
    val temperatureType: String? = null,
    val images: List<String> = emptyList(),
    val defaultLocation: Location? = null,
    val totalExpectedQuantity: Int = 0,
    val totalReceivedQuantity: Int = 0,
    val totalRemainingQuantity: Int = 0,
    val warehouses: List<IncomingWarehouseSummary> = emptyList(),
    val schedules: List<IncomingSchedule> = emptyList()
) {
    val primaryJanCode: String?
        get() = janCodes.firstOrNull()

    val fullVolume: String?
        get() = if (volume != null && volumeUnit != null) "$volume$volumeUnit" else volume

    val hasRemainingQuantity: Boolean
        get() = totalRemainingQuantity > 0
}

data class IncomingWarehouseSummary(
    val warehouseId: Int,
    val warehouseCode: String,
    val warehouseName: String,
    val expectedQuantity: Int = 0,
    val receivedQuantity: Int = 0,
    val remainingQuantity: Int = 0
)

enum class IncomingScheduleStatus {
    PENDING,
    PARTIAL,
    CONFIRMED,
    TRANSMITTED,
    CANCELLED;

    companion object {
        fun fromString(value: String?): IncomingScheduleStatus = when (value?.uppercase()) {
            "PENDING" -> PENDING
            "PARTIAL" -> PARTIAL
            "CONFIRMED" -> CONFIRMED
            "TRANSMITTED" -> TRANSMITTED
            "CANCELLED" -> CANCELLED
            else -> PENDING
        }
    }

    val canStartWork: Boolean
        get() = this == PENDING || this == PARTIAL

    val canEditFromHistory: Boolean
        get() = this == CONFIRMED
}

data class IncomingSchedule(
    val id: Int,
    val warehouseId: Int,
    val warehouseName: String? = null,
    val expectedQuantity: Int = 0,
    val receivedQuantity: Int = 0,
    val remainingQuantity: Int = 0,
    val quantityType: IncomingQuantityType = IncomingQuantityType.PIECE,
    val expectedArrivalDate: String? = null,
    val expirationDate: String? = null,
    val status: IncomingScheduleStatus = IncomingScheduleStatus.PENDING,
    val location: Location? = null
) {
    val progressText: String
        get() = "$receivedQuantity/$expectedQuantity"

    val isComplete: Boolean
        get() = remainingQuantity == 0 && expectedQuantity > 0
}

enum class IncomingQuantityType {
    PIECE,
    CASE;

    companion object {
        fun fromString(value: String?): IncomingQuantityType = when (value?.uppercase()) {
            "PIECE" -> PIECE
            "CASE" -> CASE
            else -> PIECE
        }
    }
}

enum class IncomingWorkStatus {
    WORKING,
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromString(value: String?): IncomingWorkStatus = when (value?.uppercase()) {
            "WORKING" -> WORKING
            "COMPLETED" -> COMPLETED
            "CANCELLED" -> CANCELLED
            else -> WORKING
        }
    }

    val canEdit: Boolean
        get() = this == WORKING || this == COMPLETED
}

data class IncomingWorkItem(
    val id: Int,
    val incomingScheduleId: Int,
    val pickerId: Int,
    val warehouseId: Int,
    val locationId: Int? = null,
    val location: Location? = null,
    val workQuantity: Int = 0,
    val workArrivalDate: String? = null,
    val workExpirationDate: String? = null,
    val status: IncomingWorkStatus = IncomingWorkStatus.WORKING,
    val startedAt: String? = null,
    val schedule: WorkItemSchedule? = null
) {
    val isWorking: Boolean
        get() = status == IncomingWorkStatus.WORKING

    val isCompleted: Boolean
        get() = status == IncomingWorkStatus.COMPLETED

    val canEditFromHistory: Boolean
        get() {
            val scheduleStatus = schedule?.status ?: return false
            return status.canEdit && (scheduleStatus.canEditFromHistory || scheduleStatus.canStartWork)
        }
}

data class WorkItemSchedule(
    val id: Int,
    val itemId: Int,
    val itemCode: String? = null,
    val itemName: String? = null,
    val janCodes: List<String> = emptyList(),
    val warehouseId: Int? = null,
    val warehouseName: String? = null,
    val expectedQuantity: Int = 0,
    val receivedQuantity: Int = 0,
    val remainingQuantity: Int = 0,
    val quantityType: IncomingQuantityType = IncomingQuantityType.PIECE,
    val expectedArrivalDate: String? = null,
    val status: IncomingScheduleStatus = IncomingScheduleStatus.PENDING
) {
    val primaryJanCode: String?
        get() = janCodes.firstOrNull()
}

data class StartWorkData(
    val incomingScheduleId: Int,
    val pickerId: Int,
    val warehouseId: Int
)

data class UpdateWorkItemData(
    val workQuantity: Int,
    val workArrivalDate: String? = null,
    val workExpirationDate: String? = null,
    val locationId: Int? = null
)
