package biz.smt_life.android.feature.outbound.picking

import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem

/**
 * UI State for Picking History screen (2.5.3 - 出庫処理＞履歴).
 *
 * Display modes:
 * - Editable mode: at least one PICKING item exists, show delete & confirm buttons
 * - Read-only mode: all items COMPLETED/SHORTAGE, hide buttons, list is read-only
 */
data class PickingHistoryState(
    val task: PickingTask? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isConfirming: Boolean = false,
    val errorMessage: String? = null,
    val showConfirmDialog: Boolean = false,
    val itemToDelete: PickingTaskItem? = null,
    val warehouseName: String = ""
) {
    /**
     * Items to show in history (all registered items: PICKING, COMPLETED, SHORTAGE).
     */
    val historyItems: List<PickingTaskItem>
        get() = task?.items?.filter {
            it.status != biz.smt_life.android.core.domain.model.ItemStatus.PENDING
        } ?: emptyList()

    /**
     * History items grouped by itemId for display.
     * Shows total case/piece quantities per product.
     */
    val groupedHistoryItems: List<GroupedHistoryItem>
        get() = historyItems
            .groupBy { it.itemId }
            .map { (itemId, items) ->
                val representative = items.first()
                val totalCasePlanned = items
                    .filter { it.plannedQtyType == biz.smt_life.android.core.domain.model.QuantityType.CASE }
                    .sumOf { it.plannedQty }
                val totalCasePicked = items
                    .filter { it.plannedQtyType == biz.smt_life.android.core.domain.model.QuantityType.CASE }
                    .sumOf { it.pickedQty }
                val totalPiecePlanned = items
                    .filter { it.plannedQtyType == biz.smt_life.android.core.domain.model.QuantityType.PIECE }
                    .sumOf { it.plannedQty }
                val totalPiecePicked = items
                    .filter { it.plannedQtyType == biz.smt_life.android.core.domain.model.QuantityType.PIECE }
                    .sumOf { it.pickedQty }
                val allItemIds = task?.items?.map { it.itemId }?.distinct() ?: emptyList()
                GroupedHistoryItem(
                    itemId = itemId,
                    itemName = representative.itemName,
                    locationCode = representative.locationCode,
                    janCode = representative.janCode,
                    totalCasePlanned = totalCasePlanned,
                    totalCasePicked = totalCasePicked,
                    totalPiecePlanned = totalPiecePlanned,
                    totalPiecePicked = totalPiecePicked,
                    customerCount = items.map { it.customerName }.distinct().size,
                    workNumber = (allItemIds.indexOf(itemId).takeIf { it >= 0 } ?: 0) + 1
                )
            }
            .sortedBy { it.workNumber }

    /**
     * Editable mode: at least one PICKING item exists.
     * In this mode, user can delete individual items and confirm all.
     */
    val isEditableMode: Boolean
        get() = task != null && task.hasPickingItems

    /**
     * Read-only mode: all items are COMPLETED or SHORTAGE.
     * In this mode, hide delete/confirm buttons.
     */
    val isReadOnlyMode: Boolean
        get() = task != null && task.isFullyProcessed

    /**
     * Whether the confirm-all button should be enabled.
     */
    val canConfirmAll: Boolean
        get() = isEditableMode && historyItems.isNotEmpty() && !isConfirming && !isDeleting

    /**
     * Total number of distinct products (by itemId) across all task items.
     * Matches OutboundPickingState.totalGroupCount for consistency.
     */
    val totalGroupCount: Int
        get() = task?.items?.map { it.itemId }?.distinct()?.size ?: 0

    /**
     * Number of distinct products that have been registered (PICKING status).
     * Matches the input screen's grouped product count.
     */
    val registeredGroupCount: Int
        get() = groupedHistoryItems.size
}

/**
 * History items grouped by product (itemId) with total quantities.
 */
data class GroupedHistoryItem(
    val itemId: Int,
    val itemName: String,
    val locationCode: String?,
    val janCode: String?,
    val totalCasePlanned: Double,
    val totalCasePicked: Double,
    val totalPiecePlanned: Double,
    val totalPiecePicked: Double,
    val customerCount: Int,
    val workNumber: Int = 0
)
