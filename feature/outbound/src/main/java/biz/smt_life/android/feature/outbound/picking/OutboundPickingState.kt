package biz.smt_life.android.feature.outbound.picking

import biz.smt_life.android.core.domain.model.PickingTask

/**
 * 同一商品（itemId）でグルーピングされたピッキングアイテム。
 * 1つの GroupedPickingItem = 1画面で表示される単位。
 */
data class GroupedPickingItem(
    val itemId: Int,
    val itemName: String,
    val janCode: String?,
    val volume: String?,
    val capacityCase: Int?,
    val locationCode: String?,
    val images: List<String>,
    val walkingOrder: Int,
    val customerEntries: List<CustomerEntry>
)

/**
 * 得意先ごとのエントリ。API 登録時の単位。
 * 同一得意先でケースとバラの受注が混在する場合、1つの CustomerEntry にまとめる。
 */
data class CustomerEntry(
    val caseEntry: CustomerEntryDetail?,
    val pieceEntry: CustomerEntryDetail?,
    val customerName: String,
    val customerCode: String,
    val slipNumbers: List<Int>
)

data class JanScanResult(
    val scannedCode: String,
    val isMatch: Boolean
)

data class CustomerEntryDetail(
    val pickingItemResultId: Int,
    val plannedQty: Double,
    val pickedQtyInput: String
)

/**
 * UI State for Outbound Picking (2.5.2 - Data Input Screen).
 * Items are grouped by itemId for batch picking.
 *
 * Important:
 * - `originalTask` holds the full task with all items (for computing counters)
 * - `groupedItems` contains PENDING items grouped by itemId
 * - `currentGroupIndex` tracks the current group being picked
 */
data class OutboundPickingState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val originalTask: PickingTask? = null,
    val groupedItems: List<GroupedPickingItem> = emptyList(),
    val currentGroupIndex: Int = 0,
    val totalCaseInput: String = "",
    val totalPieceInput: String = "",
    val quantityErrorMessage: String? = null,
    val isUpdating: Boolean = false,
    val showCompletionDialog: Boolean = false,
    val isCompleting: Boolean = false,
    val showImageDialog: Boolean = false,
    val showJanScannerDialog: Boolean = false,
    val isJanScannerInCamera: Boolean = false,
    val janScanResults: Map<Int, JanScanResult> = emptyMap(),
    val warehouseId: Int = 0,
    val warehouseName: String = ""
) {
    val currentGroup: GroupedPickingItem?
        get() = groupedItems.getOrNull(currentGroupIndex)

    /**
     * Total group count = pending groups + registered groups.
     * Computed by grouping ALL items by itemId.
     */
    val totalGroupCount: Int
        get() = originalTask?.items
            ?.map { it.itemId }?.distinct()?.size ?: 0

    /**
     * Registered group count = total groups - remaining pending groups.
     */
    val registeredGroupCount: Int
        get() = originalTask?.items
            ?.groupBy { it.itemId }
            ?.count { (_, items) -> items.all { it.status != biz.smt_life.android.core.domain.model.ItemStatus.PENDING } } ?: 0

    val canMovePrev: Boolean
        get() = currentGroupIndex > 0

    val canMoveNext: Boolean
        get() = currentGroupIndex < (groupedItems.size - 1)

    val canRegister: Boolean
        get() = !isUpdating && currentGroup != null

    val hasQuantityError: Boolean
        get() {
            val tc = totalCaseInput.toDoubleOrNull() ?: 0.0
            if (tc > totalCasePlanned) return true
            val tp = totalPieceInput.toDoubleOrNull() ?: 0.0
            if (tp > totalPiecePlanned) return true
            
            val group = currentGroup ?: return false
            for (entry in group.customerEntries) {
                val cQty = entry.caseEntry?.pickedQtyInput?.toDoubleOrNull() ?: 0.0
                if (cQty > (entry.caseEntry?.plannedQty ?: 0.0)) return true
                val pQty = entry.pieceEntry?.pickedQtyInput?.toDoubleOrNull() ?: 0.0
                if (pQty > (entry.pieceEntry?.plannedQty ?: 0.0)) return true
            }
            return false
        }

    /**
     * True if all items in the current group have been registered (status is not PENDING).
     */
    val isCurrentGroupRegistered: Boolean
        get() {
            val group = currentGroup ?: return false
            val items = originalTask?.items ?: return false
            val resultIds = group.customerEntries.flatMap { entry ->
                listOfNotNull(entry.caseEntry?.pickingItemResultId, entry.pieceEntry?.pickingItemResultId)
            }
            if (resultIds.isEmpty()) return false
            return resultIds.all { id ->
                items.find { it.id == id }?.status != biz.smt_life.android.core.domain.model.ItemStatus.PENDING
            }
        }

    val hasImages: Boolean
        get() = currentGroup?.images?.isNotEmpty() == true

    val currentJanScanResult: JanScanResult?
        get() = currentGroup?.itemId?.let { janScanResults[it] }

    val totalCasePlanned: Double
        get() = currentGroup?.customerEntries
            ?.mapNotNull { it.caseEntry }
            ?.sumOf { it.plannedQty } ?: 0.0

    val totalPiecePlanned: Double
        get() = currentGroup?.customerEntries
            ?.mapNotNull { it.pieceEntry }
            ?.sumOf { it.plannedQty } ?: 0.0
}
