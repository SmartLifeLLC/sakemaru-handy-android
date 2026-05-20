package biz.smt_life.android.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StockDisposalSearchResponse(
    val items: List<StockDisposalItemDto>
)

@Serializable
data class StockDisposalItemDto(
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_code") val itemCode: String,
    @SerialName("item_name") val itemName: String,
    @SerialName("display_name") val displayName: String? = null,
    val packaging: String? = null,
    @SerialName("display_packaging") val displayPackaging: String? = null,
    @SerialName("name_with_packaging") val nameWithPackaging: String? = null,
    @SerialName("order_jan_code") val orderJanCode: String? = null,
    @SerialName("capacity_case") val capacityCase: Int? = null,
    @SerialName("capacity_carton") val capacityCarton: Int? = null,
    @SerialName("matched_field") val matchedField: String? = null,
    @SerialName("matched_value") val matchedValue: String? = null,
    val stock: StockSummaryDto = StockSummaryDto()
)

@Serializable
data class StockSummaryDto(
    @SerialName("actual_quantity") val actualQuantity: Int = 0,
    @SerialName("theoretical_quantity") val theoreticalQuantity: Int = 0
)

@Serializable
data class StockDisposalRequestBody(
    @SerialName("stock_disposals") val stockDisposals: List<StockDisposalEntryDto>
)

@Serializable
data class StockDisposalEntryDto(
    @SerialName("request_id") val requestId: String,
    @SerialName("process_date") val processDate: String,
    @SerialName("disposal_date") val disposalDate: String,
    @SerialName("warehouse_code") val warehouseCode: String,
    val reason: String,
    @SerialName("slip_number") val slipNumber: String? = null,
    val note: String? = null,
    val details: List<StockDisposalDetailDto>
)

@Serializable
data class StockDisposalDetailDto(
    @SerialName("item_code") val itemCode: String,
    @SerialName("stock_allocation_code") val stockAllocationCode: String? = null,
    val quantity: Int,
    @SerialName("quantity_type") val quantityType: String,
    val note: String? = null
)

@Serializable
data class StockDisposalCreateResponse(
    @SerialName("stock_disposal_queues") val stockDisposalQueues: List<StockDisposalQueueDto>
)

@Serializable
data class StockDisposalQueueDto(
    @SerialName("request_id") val requestId: String,
    @SerialName("queue_id") val queueId: Int,
    val status: String,
    val duplicated: Boolean = false
)
