package biz.smt_life.android.core.domain.model

data class ProxyShipmentListPayload(
    val items: List<ProxyShipmentAllocation>,
    val summary: ProxyShipmentSummary,
    val businessDate: String
)

data class ProxyShipmentSummary(
    val totalCount: Int,
    val byDeliveryCourse: List<ProxyShipmentCourseSummary>
)

data class ProxyShipmentCourseSummary(
    val id: Int,
    val code: String,
    val name: String,
    val count: Int
)

data class ProxyShipmentAllocation(
    val allocationId: Int,
    val shortageId: Int,
    val shipmentDate: String,
    val status: ProxyShipmentStatus,
    val pickupWarehouse: ProxyShipmentWarehouse,
    val destinationWarehouse: ProxyShipmentWarehouse,
    val deliveryCourse: ProxyShipmentDeliveryCourse?,
    val item: ProxyShipmentItem,
    val assignQty: Int,
    val assignQtyType: ProxyShipmentQuantityType,
    val pickedQty: Int,
    val remainingQty: Int,
    val customer: ProxyShipmentCustomer?,
    val slipNumber: Int?,
    val isEditable: Boolean
)

data class ProxyShipmentDetail(
    val allocation: ProxyShipmentAllocation,
    val shortageDetail: ProxyShipmentShortageDetail?,
    val candidateLocations: List<ProxyShipmentCandidateLocation>
)

data class ProxyShipmentCompletionResult(
    val allocation: ProxyShipmentAllocation,
    val stockTransferQueueId: Int?
)

data class ProxyShipmentWarehouse(
    val id: Int,
    val code: String,
    val name: String
)

data class ProxyShipmentDeliveryCourse(
    val id: Int,
    val code: String,
    val name: String
)

data class ProxyShipmentCustomer(
    val code: String,
    val name: String
)

data class ProxyShipmentItem(
    val id: Int,
    val code: String,
    val name: String,
    val janCodes: List<String>,
    val volume: String?,
    val capacityCase: Int?,
    val temperatureType: String?,
    val images: List<String>
)

data class ProxyShipmentShortageDetail(
    val orderQty: Int,
    val plannedQty: Int,
    val pickedQty: Int,
    val shortageQty: Int,
    val qtyTypeAtOrder: String
)

data class ProxyShipmentCandidateLocation(
    val locationId: Int,
    val code: String,
    val availableQty: Int
)

enum class ProxyShipmentStatus {
    RESERVED,
    PICKING,
    FULFILLED,
    SHORTAGE;

    val isActive: Boolean
        get() = this == RESERVED || this == PICKING

    val label: String
        get() = when (this) {
            RESERVED -> "未着手"
            PICKING -> "ピッキング中"
            FULFILLED -> "完了"
            SHORTAGE -> "欠品あり完了"
        }

    companion object {
        fun fromString(value: String): ProxyShipmentStatus = when (value.uppercase()) {
            "RESERVED" -> RESERVED
            "PICKING" -> PICKING
            "FULFILLED" -> FULFILLED
            "SHORTAGE" -> SHORTAGE
            else -> RESERVED
        }
    }
}

enum class ProxyShipmentQuantityType {
    CASE,
    PIECE,
    CARTON;

    val label: String
        get() = when (this) {
            CASE -> "ケース"
            PIECE -> "バラ"
            CARTON -> "ボール"
        }

    companion object {
        fun fromString(value: String): ProxyShipmentQuantityType = when (value.uppercase()) {
            "CASE" -> CASE
            "PIECE" -> PIECE
            "CARTON" -> CARTON
            else -> PIECE
        }
    }
}
