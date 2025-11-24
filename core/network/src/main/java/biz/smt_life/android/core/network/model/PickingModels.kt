package biz.smt_life.android.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data models for Picking Tasks API responses per handy_swagger_20251109.json.
 * These mirror the server response structure exactly.
 */

@Serializable
data class PickingTaskResponse(
    val course: CourseInfo,
    @SerialName("picking_area") val pickingArea: PickingAreaInfo,
    val wave: WaveInfo,
    @SerialName("picking_list") val pickingList: List<PickingItem>
)

@Serializable
data class CourseInfo(
    val code: String,
    val name: String
)

@Serializable
data class PickingAreaInfo(
    val code: String,
    val name: String
)

@Serializable
data class WaveInfo(
    @SerialName("wms_picking_task_id") val wmsPickingTaskId: Int,
    @SerialName("wms_wave_id") val wmsWaveId: Int
)

@Serializable
data class PickingItem(
    @SerialName("wms_picking_item_result_id") val wmsPickingItemResultId: Int,
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_name") val itemName: String,
    @SerialName("planned_qty_type") val plannedQtyType: String, // "CASE" or "PIECE"
    @SerialName("planned_qty") val plannedQty: String,
    @SerialName("picked_qty") val pickedQty: String,
    @SerialName("slip_number") val slipNumber: Int
)
