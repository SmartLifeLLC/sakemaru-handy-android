package biz.smt_life.android.feature.outbound.proxyshipment

import biz.smt_life.android.core.domain.model.ProxyShipmentAllocation
import biz.smt_life.android.core.domain.model.ProxyShipmentDetail
import biz.smt_life.android.core.domain.model.ProxyShipmentStatus

data class ProxyShipmentPickingState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val warehouseId: Int = 0,
    val shipmentDate: String = "",
    val courseKey: String = "",
    val groupAllocations: List<ProxyShipmentAllocation> = emptyList(),
    val initialAllocationCount: Int = 0,
    val currentAllocationIndex: Int = 0,
    val detail: ProxyShipmentDetail? = null,
    val pickedQtyInput: String = "",
    val showJanScannerDialog: Boolean = false,
    val showImageDialog: Boolean = false,
    val janScanFeedback: ProxyShipmentJanScanFeedback? = null
) {
    val allocation
        get() = detail?.allocation ?: groupAllocations.getOrNull(currentAllocationIndex)

    val totalAllocationCount: Int
        get() = if (initialAllocationCount > 0) initialAllocationCount else groupAllocations.size

    val registeredAllocationCount: Int
        get() = (totalAllocationCount - groupAllocations.count { it.status == ProxyShipmentStatus.RESERVED })
            .coerceAtLeast(0)

    val canMovePrev: Boolean
        get() = currentAllocationIndex > 0

    val canMoveNext: Boolean
        get() = currentAllocationIndex < groupAllocations.lastIndex

    val parsedPickedQty: Int?
        get() = pickedQtyInput.toIntOrNull()

    val quantityErrorMessage: String?
        get() {
            val allocation = allocation ?: return null
            val pickedQty = parsedPickedQty ?: return if (pickedQtyInput.isBlank()) null else "数値を入力してください"
            return if (pickedQty < 0 || pickedQty > allocation.assignQty) {
                "0 以上 ${allocation.assignQty} 以下で入力してください"
            } else {
                null
            }
        }

    val hasUnsavedChanges: Boolean
        get() = allocation?.pickedQty != parsedPickedQty

    val canUpdate: Boolean
        get() = !isLoading &&
            !isSubmitting &&
            parsedPickedQty != null &&
            quantityErrorMessage == null &&
            allocation?.isEditable == true
}

data class ProxyShipmentJanScanFeedback(
    val scannedCode: String,
    val isMatch: Boolean
)
