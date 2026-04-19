package biz.smt_life.android.feature.outbound.proxyshipment

import biz.smt_life.android.core.domain.model.ProxyShipmentAllocation
import biz.smt_life.android.core.domain.model.ProxyShipmentStatus

internal const val PROXY_SHIPMENT_NO_COURSE_KEY = "none"

internal data class ProxyShipmentCourseGroup(
    val shipmentDate: String,
    val courseKey: String,
    val deliveryCourseId: Int?,
    val deliveryCourseCode: String,
    val deliveryCourseName: String,
    val allocations: List<ProxyShipmentAllocation>
) {
    val status: ProxyShipmentStatus
        get() = if (allocations.any { it.status == ProxyShipmentStatus.PICKING }) {
            ProxyShipmentStatus.PICKING
        } else {
            ProxyShipmentStatus.RESERVED
        }

    val totalCount: Int
        get() = allocations.size

    val reservedCount: Int
        get() = allocations.count { it.status == ProxyShipmentStatus.RESERVED }

    val registeredCount: Int
        get() = totalCount - reservedCount

    val totalAssignQty: Int
        get() = allocations.sumOf { it.assignQty }

    val totalPickedQty: Int
        get() = allocations.sumOf { it.pickedQty }

    val destinationSummary: String
        get() = allocations.map { it.destinationWarehouse.name }
            .distinct()
            .joinToString(separator = " / ")

    val customerCount: Int
        get() = allocations.mapNotNull { it.customer?.code }
            .distinct()
            .size
}

internal fun List<ProxyShipmentAllocation>.toProxyShipmentCourseGroups(): List<ProxyShipmentCourseGroup> {
    return this
        .filter { it.status == ProxyShipmentStatus.RESERVED || it.status == ProxyShipmentStatus.PICKING }
        .groupBy { allocation ->
            allocation.shipmentDate to allocation.proxyShipmentCourseKey()
        }
        .map { entry ->
            val (key, allocations) = entry
            val (shipmentDate, courseKey) = key
            val sortedAllocations = allocations.sortedProxyShipmentGroupAllocations()
            val representative = sortedAllocations.first()
            ProxyShipmentCourseGroup(
                shipmentDate = shipmentDate,
                courseKey = courseKey,
                deliveryCourseId = representative.deliveryCourse?.id,
                deliveryCourseCode = representative.deliveryCourse?.code.orEmpty(),
                deliveryCourseName = representative.deliveryCourse?.name ?: "配送コース未設定",
                allocations = sortedAllocations
            )
        }
        .sortedWith(
            compareBy<ProxyShipmentCourseGroup>(
                { it.shipmentDate },
                { it.deliveryCourseCode.ifBlank { it.deliveryCourseName } },
                { it.deliveryCourseName }
            )
        )
}

internal fun List<ProxyShipmentAllocation>.findProxyShipmentGroup(
    shipmentDate: String,
    courseKey: String
): List<ProxyShipmentAllocation> {
    return filter {
        it.shipmentDate == shipmentDate && it.proxyShipmentCourseKey() == courseKey
    }.sortedProxyShipmentGroupAllocations()
}

internal fun ProxyShipmentAllocation.proxyShipmentCourseKey(): String {
    return deliveryCourse?.id?.toString() ?: PROXY_SHIPMENT_NO_COURSE_KEY
}

private fun List<ProxyShipmentAllocation>.sortedProxyShipmentGroupAllocations(): List<ProxyShipmentAllocation> {
    return sortedWith(
        compareBy<ProxyShipmentAllocation>(
            { allocation ->
                when (allocation.status) {
                    ProxyShipmentStatus.PICKING -> 0
                    ProxyShipmentStatus.RESERVED -> 1
                    ProxyShipmentStatus.FULFILLED -> 2
                    ProxyShipmentStatus.SHORTAGE -> 3
                }
            },
            { it.slipNumber ?: Int.MAX_VALUE },
            { it.customer?.name.orEmpty() },
            { it.item.code },
            { it.allocationId }
        )
    )
}
