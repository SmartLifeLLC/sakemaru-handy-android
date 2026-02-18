package biz.smt_life.android.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API response/request models for Incoming (入庫) feature.
 */

// ─── Warehouse ───

@Serializable
data class WarehouseResponse(
    val id: Int,
    val code: String,
    val name: String,
    @SerialName("kana_name") val kanaName: String = "",
    @SerialName("out_of_stock_option") val outOfStockOption: String = "IGNORE_STOCK"
)

// ─── Incoming Schedules ───

@Serializable
data class IncomingScheduleProductResponse(
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_code") val itemCode: String = "",
    @SerialName("item_name") val itemName: String,
    @SerialName("search_code") val searchCode: String = "",
    @SerialName("jan_codes") val janCodes: List<String> = emptyList(),
    val volume: String? = null,
    @SerialName("temperature_type") val temperatureType: String? = null,
    val images: List<String> = emptyList(),
    @SerialName("total_expected_quantity") val totalExpectedQuantity: Int = 0,
    @SerialName("total_received_quantity") val totalReceivedQuantity: Int = 0,
    @SerialName("total_remaining_quantity") val totalRemainingQuantity: Int = 0,
    val warehouses: List<WarehouseSummaryResponse> = emptyList(),
    val schedules: List<ScheduleResponse> = emptyList()
)

@Serializable
data class WarehouseSummaryResponse(
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("warehouse_code") val warehouseCode: String = "",
    @SerialName("warehouse_name") val warehouseName: String = "",
    @SerialName("expected_quantity") val expectedQuantity: Int = 0,
    @SerialName("received_quantity") val receivedQuantity: Int = 0,
    @SerialName("remaining_quantity") val remainingQuantity: Int = 0
)

@Serializable
data class ScheduleResponse(
    val id: Int,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("warehouse_name") val warehouseName: String = "",
    @SerialName("expected_quantity") val expectedQuantity: Int = 0,
    @SerialName("received_quantity") val receivedQuantity: Int = 0,
    @SerialName("remaining_quantity") val remainingQuantity: Int = 0,
    @SerialName("quantity_type") val quantityType: String = "PIECE",
    @SerialName("expected_arrival_date") val expectedArrivalDate: String = "",
    val status: String = "PENDING"
)

// ─── Schedule Detail ───

@Serializable
data class ScheduleDetailResponse(
    val id: Int,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("warehouse_code") val warehouseCode: String = "",
    @SerialName("warehouse_name") val warehouseName: String = "",
    @SerialName("item_id") val itemId: Int = 0,
    @SerialName("item_code") val itemCode: String = "",
    @SerialName("item_name") val itemName: String = "",
    @SerialName("search_code") val searchCode: String = "",
    @SerialName("jan_codes") val janCodes: List<String> = emptyList(),
    @SerialName("expected_quantity") val expectedQuantity: Int = 0,
    @SerialName("received_quantity") val receivedQuantity: Int = 0,
    @SerialName("remaining_quantity") val remainingQuantity: Int = 0,
    @SerialName("quantity_type") val quantityType: String = "PIECE",
    @SerialName("expected_arrival_date") val expectedArrivalDate: String = "",
    val status: String = "PENDING"
)

// ─── Work Items ───

@Serializable
data class IncomingWorkItemResponse(
    val id: Int,
    @SerialName("incoming_schedule_id") val incomingScheduleId: Int,
    @SerialName("picker_id") val pickerId: Int,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("location_id") val locationId: Int? = null,
    val location: LocationResponse? = null,
    @SerialName("work_quantity") val workQuantity: Int = 0,
    @SerialName("work_arrival_date") val workArrivalDate: String = "",
    @SerialName("work_expiration_date") val workExpirationDate: String? = null,
    val status: String = "WORKING",
    @SerialName("started_at") val startedAt: String = "",
    val schedule: WorkItemScheduleResponse? = null
)

@Serializable
data class WorkItemScheduleResponse(
    val id: Int,
    @SerialName("item_id") val itemId: Int = 0,
    @SerialName("item_code") val itemCode: String = "",
    @SerialName("item_name") val itemName: String = "",
    @SerialName("warehouse_id") val warehouseId: Int = 0,
    @SerialName("warehouse_name") val warehouseName: String = "",
    @SerialName("expected_quantity") val expectedQuantity: Int = 0,
    @SerialName("received_quantity") val receivedQuantity: Int = 0,
    @SerialName("remaining_quantity") val remainingQuantity: Int = 0,
    @SerialName("quantity_type") val quantityType: String = "PIECE"
)

// ─── Locations ───

@Serializable
data class LocationResponse(
    val id: Int,
    val code1: String = "",
    val code2: String = "",
    val code3: String = "",
    val name: String = "",
    @SerialName("display_name") val displayName: String = ""
)

// ─── Requests ───

@Serializable
data class StartWorkRequest(
    @SerialName("incoming_schedule_id") val incomingScheduleId: Int,
    @SerialName("picker_id") val pickerId: Int,
    @SerialName("warehouse_id") val warehouseId: Int
)

@Serializable
data class UpdateWorkRequest(
    @SerialName("work_quantity") val workQuantity: Int,
    @SerialName("work_arrival_date") val workArrivalDate: String,
    @SerialName("work_expiration_date") val workExpirationDate: String? = null,
    @SerialName("location_id") val locationId: Int? = null
)
