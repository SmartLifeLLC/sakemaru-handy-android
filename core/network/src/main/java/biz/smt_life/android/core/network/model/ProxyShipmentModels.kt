package biz.smt_life.android.core.network.model

import biz.smt_life.android.core.domain.model.ProxyShipmentAllocation
import biz.smt_life.android.core.domain.model.ProxyShipmentCandidateLocation
import biz.smt_life.android.core.domain.model.ProxyShipmentCompletionResult
import biz.smt_life.android.core.domain.model.ProxyShipmentCourseSummary
import biz.smt_life.android.core.domain.model.ProxyShipmentCustomer
import biz.smt_life.android.core.domain.model.ProxyShipmentDeliveryCourse
import biz.smt_life.android.core.domain.model.ProxyShipmentDetail
import biz.smt_life.android.core.domain.model.ProxyShipmentItem
import biz.smt_life.android.core.domain.model.ProxyShipmentListPayload
import biz.smt_life.android.core.domain.model.ProxyShipmentQuantityType
import biz.smt_life.android.core.domain.model.ProxyShipmentShortageDetail
import biz.smt_life.android.core.domain.model.ProxyShipmentStatus
import biz.smt_life.android.core.domain.model.ProxyShipmentSummary
import biz.smt_life.android.core.domain.model.ProxyShipmentWarehouse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ProxyShipmentListResponse(
    val items: List<ProxyShipmentAllocationDto>,
    val summary: ProxyShipmentSummaryDto,
    val meta: ProxyShipmentMetaDto
)

@Serializable
data class ProxyShipmentSummaryDto(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("by_delivery_course") val byDeliveryCourse: List<ProxyShipmentCourseSummaryDto> = emptyList()
)

@Serializable
data class ProxyShipmentMetaDto(
    @SerialName("business_date") val businessDate: String
)

@Serializable
data class ProxyShipmentCourseSummaryDto(
    val id: Int,
    @Serializable(with = StringLikeSerializer::class)
    val code: String,
    val name: String,
    val count: Int
)

@Serializable
data class ProxyShipmentAllocationDto(
    @SerialName("allocation_id") val allocationId: Int,
    @SerialName("shortage_id") val shortageId: Int,
    @SerialName("shipment_date") val shipmentDate: String,
    val status: String,
    @SerialName("pickup_warehouse") val pickupWarehouse: ProxyShipmentWarehouseDto,
    @SerialName("destination_warehouse") val destinationWarehouse: ProxyShipmentWarehouseDto,
    @SerialName("delivery_course") val deliveryCourse: ProxyShipmentDeliveryCourseDto? = null,
    val item: ProxyShipmentItemDto,
    @SerialName("assign_qty") val assignQty: Int,
    @SerialName("assign_qty_type") val assignQtyType: String,
    @SerialName("picked_qty") val pickedQty: Int,
    @SerialName("remaining_qty") val remainingQty: Int,
    val customer: ProxyShipmentCustomerDto? = null,
    @SerialName("slip_number") val slipNumber: Int? = null,
    @SerialName("is_editable") val isEditable: Boolean = true,
    @SerialName("shortage_detail") val shortageDetail: ProxyShipmentShortageDetailDto? = null,
    @SerialName("candidate_locations") val candidateLocations: List<ProxyShipmentCandidateLocationDto> = emptyList(),
    @SerialName("stock_transfer_queue_id") val stockTransferQueueId: Int? = null
)

@Serializable
data class ProxyShipmentWarehouseDto(
    val id: Int,
    @Serializable(with = StringLikeSerializer::class)
    val code: String,
    val name: String
)

@Serializable
data class ProxyShipmentDeliveryCourseDto(
    val id: Int,
    @Serializable(with = StringLikeSerializer::class)
    val code: String,
    val name: String
)

@Serializable
data class ProxyShipmentItemDto(
    val id: Int,
    @Serializable(with = StringLikeSerializer::class)
    val code: String,
    val name: String,
    @SerialName("jan_codes") val janCodes: List<String> = emptyList(),
    val volume: String? = null,
    @SerialName("capacity_case") val capacityCase: Int? = null,
    @SerialName("temperature_type") val temperatureType: String? = null,
    val images: List<String> = emptyList()
)

@Serializable
data class ProxyShipmentCustomerDto(
    @Serializable(with = StringLikeSerializer::class)
    val code: String,
    val name: String
)

@Serializable
data class ProxyShipmentShortageDetailDto(
    @SerialName("order_qty") val orderQty: Int,
    @SerialName("planned_qty") val plannedQty: Int,
    @SerialName("picked_qty") val pickedQty: Int,
    @SerialName("shortage_qty") val shortageQty: Int,
    @SerialName("qty_type_at_order") val qtyTypeAtOrder: String
)

@Serializable
data class ProxyShipmentCandidateLocationDto(
    @SerialName("location_id") val locationId: Int,
    val code: String,
    @SerialName("available_qty") val availableQty: Int
)

@Serializable
data class ProxyShipmentWarehouseRequest(
    @SerialName("warehouse_id") val warehouseId: Int
)

@Serializable
data class ProxyShipmentUpdateRequest(
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("picked_qty") val pickedQty: Int
)

@Serializable
data class ProxyShipmentCompleteRequest(
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("picked_qty") val pickedQty: Int? = null
)

object StringLikeSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringLike", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        return if (decoder is JsonDecoder) {
            decoder.decodeJsonElement().toFlexibleString().orEmpty()
        } else {
            decoder.decodeString()
        }
    }
}

private fun JsonElement.toFlexibleString(): String? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> content
    else -> toString()
}

fun ProxyShipmentListResponse.toDomain(): ProxyShipmentListPayload = ProxyShipmentListPayload(
    items = items.map { it.toDomain() },
    summary = ProxyShipmentSummary(
        totalCount = summary.totalCount,
        byDeliveryCourse = summary.byDeliveryCourse.map {
            ProxyShipmentCourseSummary(
                id = it.id,
                code = it.code,
                name = it.name,
                count = it.count
            )
        }
    ),
    businessDate = meta.businessDate
)

fun ProxyShipmentAllocationDto.toDomain(): ProxyShipmentAllocation = ProxyShipmentAllocation(
    allocationId = allocationId,
    shortageId = shortageId,
    shipmentDate = shipmentDate,
    status = ProxyShipmentStatus.fromString(status),
    pickupWarehouse = pickupWarehouse.toDomain(),
    destinationWarehouse = destinationWarehouse.toDomain(),
    deliveryCourse = deliveryCourse?.toDomain(),
    item = item.toDomain(),
    assignQty = assignQty,
    assignQtyType = ProxyShipmentQuantityType.fromString(assignQtyType),
    pickedQty = pickedQty,
    remainingQty = remainingQty,
    customer = customer?.toDomain(),
    slipNumber = slipNumber,
    isEditable = isEditable
)

fun ProxyShipmentAllocationDto.toDetailDomain(): ProxyShipmentDetail = ProxyShipmentDetail(
    allocation = toDomain(),
    shortageDetail = shortageDetail?.toDomain(),
    candidateLocations = candidateLocations.map { it.toDomain() }
)

fun ProxyShipmentAllocationDto.toCompletionDomain(): ProxyShipmentCompletionResult =
    ProxyShipmentCompletionResult(
        allocation = toDomain(),
        stockTransferQueueId = stockTransferQueueId
    )

private fun ProxyShipmentWarehouseDto.toDomain(): ProxyShipmentWarehouse =
    ProxyShipmentWarehouse(
        id = id,
        code = code,
        name = name
    )

private fun ProxyShipmentDeliveryCourseDto.toDomain(): ProxyShipmentDeliveryCourse =
    ProxyShipmentDeliveryCourse(
        id = id,
        code = code,
        name = name
    )

private fun ProxyShipmentItemDto.toDomain(): ProxyShipmentItem =
    ProxyShipmentItem(
        id = id,
        code = code,
        name = name,
        janCodes = janCodes,
        volume = volume,
        capacityCase = capacityCase,
        temperatureType = temperatureType,
        images = images
    )

private fun ProxyShipmentCustomerDto.toDomain(): ProxyShipmentCustomer =
    ProxyShipmentCustomer(
        code = code,
        name = name
    )

private fun ProxyShipmentShortageDetailDto.toDomain(): ProxyShipmentShortageDetail =
    ProxyShipmentShortageDetail(
        orderQty = orderQty,
        plannedQty = plannedQty,
        pickedQty = pickedQty,
        shortageQty = shortageQty,
        qtyTypeAtOrder = qtyTypeAtOrder
    )

private fun ProxyShipmentCandidateLocationDto.toDomain(): ProxyShipmentCandidateLocation =
    ProxyShipmentCandidateLocation(
        locationId = locationId,
        code = code,
        availableQty = availableQty
    )
