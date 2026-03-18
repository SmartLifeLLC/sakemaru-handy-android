package biz.smt_life.android.feature.outbound.picking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.ItemStatus
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.domain.repository.PickingTaskRepository
import biz.smt_life.android.core.network.NetworkException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Outbound Picking (2.5.2 - Data Input Screen).
 * Groups items by itemId for batch picking with total/individual quantity input.
 */
@HiltViewModel
class OutboundPickingViewModel @Inject constructor(
    private val pickingTaskRepository: PickingTaskRepository,
    private val incomingRepository: IncomingRepository,
    private val tokenManager: biz.smt_life.android.core.ui.TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(OutboundPickingState())
    val state: StateFlow<OutboundPickingState> = _state.asStateFlow()

    /**
     * Initialize the screen with a picking task.
     * Normal flow: groups PENDING items only.
     * Edit flow (from history): includes PICKING items for the specified editItemId,
     * so user can edit a previously registered item.
     * @param editItemId If specified, include PICKING items for this itemId and jump to it
     */
    fun initialize(task: PickingTask, editItemId: Int? = null) {
        val warehouseId = tokenManager.getDefaultWarehouseId()
        val grouped = if (editItemId != null) {
            groupEditableItems(task.items, editItemId)
        } else {
            groupPendingItems(task.items)
        }

        val startIndex = if (editItemId != null) {
            grouped.indexOfFirst { it.itemId == editItemId }.coerceAtLeast(0)
        } else {
            0
        }

        _state.update {
            if (grouped.isEmpty()) {
                it.copy(
                    originalTask = task,
                    groupedItems = emptyList(),
                    currentGroupIndex = 0,
                    totalCaseInput = "",
                    totalPieceInput = "",
                    isLoading = false,
                    warehouseId = warehouseId
                )
            } else {
                val targetGroup = grouped[startIndex]
                it.copy(
                    originalTask = task,
                    groupedItems = grouped,
                    currentGroupIndex = startIndex,
                    totalCaseInput = formatTotal(targetGroup, QuantityType.CASE),
                    totalPieceInput = formatTotal(targetGroup, QuantityType.PIECE),
                    isLoading = false,
                    warehouseId = warehouseId
                )
            }
        }
        loadWarehouseName(warehouseId)
    }

    // ===== Grouping Logic =====

    private fun groupPendingItems(items: List<PickingTaskItem>): List<GroupedPickingItem> {
        val pendingItems = items.filter { it.status == ItemStatus.PENDING }
        return groupItemsInternal(pendingItems, usePickedQty = false)
    }

    /**
     * Group PENDING items + PICKING items for the specified editItemId.
     * PICKING items use pickedQty as initial input value.
     */
    private fun groupEditableItems(items: List<PickingTaskItem>, editItemId: Int): List<GroupedPickingItem> {
        val editableItems = items.filter {
            it.status == ItemStatus.PENDING || (it.status == ItemStatus.PICKING && it.itemId == editItemId)
        }
        return groupItemsInternal(editableItems, usePickedQty = true)
    }

    private fun groupItemsInternal(
        items: List<PickingTaskItem>,
        usePickedQty: Boolean
    ): List<GroupedPickingItem> {
        return items
            .groupBy { it.itemId }
            .map { (itemId, itemGroup) ->
                val representative = itemGroup.first()
                val customerGroups = itemGroup.groupBy { it.customerName ?: "" }
                val customerEntries = customerGroups.map { (customerName, customerItems) ->
                    val caseItem = customerItems.find { it.plannedQtyType == QuantityType.CASE }
                    val pieceItem = customerItems.find { it.plannedQtyType == QuantityType.PIECE }
                    CustomerEntry(
                        customerName = customerName,
                        customerCode = customerItems.first().customerCode ?: "",
                        caseEntry = caseItem?.let {
                            val initialQty = if (usePickedQty && it.status == ItemStatus.PICKING) it.pickedQty else it.plannedQty
                            CustomerEntryDetail(
                                pickingItemResultId = it.id,
                                plannedQty = it.plannedQty,
                                pickedQtyInput = String.format("%.0f", initialQty)
                            )
                        },
                        pieceEntry = pieceItem?.let {
                            val initialQty = if (usePickedQty && it.status == ItemStatus.PICKING) it.pickedQty else it.plannedQty
                            CustomerEntryDetail(
                                pickingItemResultId = it.id,
                                plannedQty = it.plannedQty,
                                pickedQtyInput = String.format("%.0f", initialQty)
                            )
                        },
                        slipNumbers = customerItems.map { it.slipNumber }
                    )
                }
                GroupedPickingItem(
                    itemId = itemId,
                    itemName = representative.itemName,
                    janCode = representative.janCode,
                    volume = representative.volume,
                    capacityCase = representative.capacityCase,
                    locationCode = representative.locationCode,
                    images = representative.images,
                    walkingOrder = itemGroup.minOf { it.walkingOrder },
                    customerEntries = customerEntries
                )
            }
            .sortedBy { it.walkingOrder }
    }

    private fun formatTotal(group: GroupedPickingItem, type: QuantityType): String {
        val total = when (type) {
            QuantityType.CASE -> group.customerEntries.mapNotNull { it.caseEntry }.sumOf { it.plannedQty }
            QuantityType.PIECE -> group.customerEntries.mapNotNull { it.pieceEntry }.sumOf { it.plannedQty }
        }
        return if (total > 0) String.format("%.0f", total) else ""
    }

    // ===== Total Input Handlers (auto-distribute) =====

    fun onTotalCaseInputChange(value: String) {
        if (value.isNotEmpty() && !value.matches(Regex("^\\d*\\.?\\d*$"))) return
        _state.update { it.copy(totalCaseInput = value) }

        val totalInput = value.toDoubleOrNull() ?: return
        val group = _state.value.currentGroup ?: return
        val maxTotal = _state.value.totalCasePlanned
        val clamped = totalInput.coerceIn(0.0, maxTotal)

        var remaining = clamped
        val updatedEntries = group.customerEntries.map { entry ->
            if (entry.caseEntry != null) {
                val allocated = minOf(remaining, entry.caseEntry.plannedQty)
                remaining -= allocated
                entry.copy(caseEntry = entry.caseEntry.copy(
                    pickedQtyInput = String.format("%.0f", allocated)
                ))
            } else entry
        }
        updateCurrentGroupEntries(updatedEntries)
    }

    fun onTotalPieceInputChange(value: String) {
        if (value.isNotEmpty() && !value.matches(Regex("^\\d*\\.?\\d*$"))) return
        _state.update { it.copy(totalPieceInput = value) }

        val totalInput = value.toDoubleOrNull() ?: return
        val group = _state.value.currentGroup ?: return
        val maxTotal = _state.value.totalPiecePlanned
        val clamped = totalInput.coerceIn(0.0, maxTotal)

        var remaining = clamped
        val updatedEntries = group.customerEntries.map { entry ->
            if (entry.pieceEntry != null) {
                val allocated = minOf(remaining, entry.pieceEntry.plannedQty)
                remaining -= allocated
                entry.copy(pieceEntry = entry.pieceEntry.copy(
                    pickedQtyInput = String.format("%.0f", allocated)
                ))
            } else entry
        }
        updateCurrentGroupEntries(updatedEntries)
    }

    // ===== Individual Input Handlers =====

    fun onCustomerCaseQtyChange(customerIndex: Int, value: String) {
        if (value.isNotEmpty() && !value.matches(Regex("^\\d*\\.?\\d*$"))) return
        val group = _state.value.currentGroup ?: return
        val entry = group.customerEntries.getOrNull(customerIndex) ?: return
        val caseEntry = entry.caseEntry ?: return

        val updatedEntries = group.customerEntries.toMutableList()
        updatedEntries[customerIndex] = entry.copy(
            caseEntry = caseEntry.copy(pickedQtyInput = value)
        )
        updateCurrentGroupEntries(updatedEntries)
        recalculateTotalCase()
    }

    fun onCustomerPieceQtyChange(customerIndex: Int, value: String) {
        if (value.isNotEmpty() && !value.matches(Regex("^\\d*\\.?\\d*$"))) return
        val group = _state.value.currentGroup ?: return
        val entry = group.customerEntries.getOrNull(customerIndex) ?: return
        val pieceEntry = entry.pieceEntry ?: return

        val updatedEntries = group.customerEntries.toMutableList()
        updatedEntries[customerIndex] = entry.copy(
            pieceEntry = pieceEntry.copy(pickedQtyInput = value)
        )
        updateCurrentGroupEntries(updatedEntries)
        recalculateTotalPiece()
    }

    private fun updateCurrentGroupEntries(updatedEntries: List<CustomerEntry>) {
        val currentIndex = _state.value.currentGroupIndex
        val groups = _state.value.groupedItems.toMutableList()
        val currentGroup = groups.getOrNull(currentIndex) ?: return
        groups[currentIndex] = currentGroup.copy(customerEntries = updatedEntries)
        _state.update { it.copy(groupedItems = groups) }
    }

    private fun recalculateTotalCase() {
        val group = _state.value.currentGroup ?: return
        val total = group.customerEntries
            .mapNotNull { it.caseEntry }
            .sumOf { it.pickedQtyInput.toDoubleOrNull() ?: 0.0 }
        _state.update { it.copy(totalCaseInput = String.format("%.0f", total)) }
    }

    private fun recalculateTotalPiece() {
        val group = _state.value.currentGroup ?: return
        val total = group.customerEntries
            .mapNotNull { it.pieceEntry }
            .sumOf { it.pickedQtyInput.toDoubleOrNull() ?: 0.0 }
        _state.update { it.copy(totalPieceInput = String.format("%.0f", total)) }
    }

    // ===== Registration =====

    fun registerGroupedItem() {
        val group = _state.value.currentGroup ?: return
        val originalTask = _state.value.originalTask ?: return

        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, errorMessage = null) }

            try {
                for (entry in group.customerEntries) {
                    entry.caseEntry?.let { caseDetail ->
                        val qty = caseDetail.pickedQtyInput.toDoubleOrNull() ?: 0.0
                        pickingTaskRepository.updatePickingItem(
                            resultId = caseDetail.pickingItemResultId,
                            pickedQty = qty,
                            pickedQtyType = QuantityType.CASE.name
                        ).getOrThrow()
                    }
                    entry.pieceEntry?.let { pieceDetail ->
                        val qty = pieceDetail.pickedQtyInput.toDoubleOrNull() ?: 0.0
                        pickingTaskRepository.updatePickingItem(
                            resultId = pieceDetail.pickingItemResultId,
                            pickedQty = qty,
                            pickedQtyType = QuantityType.PIECE.name
                        ).getOrThrow()
                    }
                }
                refreshTaskFromServer(originalTask.taskId)
            } catch (e: Exception) {
                _state.update { it.copy(isUpdating = false, errorMessage = mapErrorMessage(e)) }
            }
        }
    }

    // ===== Navigation =====

    fun moveToPrevGroup() {
        if (!_state.value.canMovePrev) return
        val newIndex = _state.value.currentGroupIndex - 1
        _state.update { it.copy(currentGroupIndex = newIndex) }
        loadGroupTotals(newIndex)
    }

    fun moveToNextGroup() {
        if (!_state.value.canMoveNext) return
        val newIndex = _state.value.currentGroupIndex + 1
        _state.update { it.copy(currentGroupIndex = newIndex) }
        loadGroupTotals(newIndex)
    }

    private fun loadGroupTotals(index: Int) {
        val group = _state.value.groupedItems.getOrNull(index) ?: return
        _state.update {
            it.copy(
                totalCaseInput = formatTotal(group, QuantityType.CASE),
                totalPieceInput = formatTotal(group, QuantityType.PIECE)
            )
        }
    }

    private fun moveToNextGroupOrComplete() {
        val refreshedTask = _state.value.originalTask ?: return
        val grouped = groupPendingItems(refreshedTask.items)

        if (grouped.isEmpty()) {
            _state.update {
                it.copy(
                    groupedItems = emptyList(),
                    currentGroupIndex = 0,
                    totalCaseInput = "",
                    totalPieceInput = "",
                    isUpdating = false
                )
            }
        } else {
            val firstGroup = grouped.first()
            _state.update {
                it.copy(
                    groupedItems = grouped,
                    currentGroupIndex = 0,
                    totalCaseInput = formatTotal(firstGroup, QuantityType.CASE),
                    totalPieceInput = formatTotal(firstGroup, QuantityType.PIECE),
                    isUpdating = false
                )
            }
        }
    }

    // ===== Refresh & Complete =====

    private suspend fun refreshTaskFromServer(taskId: Int) {
        val warehouseId = _state.value.warehouseId
        val pickerId = tokenManager.getPickerId()

        pickingTaskRepository.refreshTask(taskId, warehouseId, pickerId)
            .onSuccess { refreshedTask ->
                _state.update { it.copy(originalTask = refreshedTask) }
                moveToNextGroupOrComplete()
            }
            .onFailure { error ->
                _state.update { it.copy(isUpdating = false, errorMessage = mapErrorMessage(error)) }
            }
    }

    fun completeTask(onSuccess: () -> Unit) {
        val taskId = _state.value.originalTask?.taskId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isCompleting = true, errorMessage = null) }

            pickingTaskRepository.completeTask(taskId)
                .onSuccess {
                    _state.update { it.copy(isCompleting = false, showCompletionDialog = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _state.update { it.copy(isCompleting = false, errorMessage = mapErrorMessage(error)) }
                }
        }
    }

    // ===== Utility =====

    private fun loadWarehouseName(warehouseId: Int) {
        viewModelScope.launch {
            incomingRepository.getWarehouses()
                .onSuccess { warehouses ->
                    val name = warehouses.firstOrNull { it.id == warehouseId }?.name ?: ""
                    _state.update { it.copy(warehouseName = name) }
                }
        }
    }

    fun showImageDialog() {
        _state.update { it.copy(showImageDialog = true) }
    }

    fun dismissImageDialog() {
        _state.update { it.copy(showImageDialog = false) }
    }

    fun showJanScannerDialog() {
        _state.update { it.copy(showJanScannerDialog = true) }
    }

    fun dismissJanScannerDialog() {
        _state.update { it.copy(showJanScannerDialog = false) }
    }

    fun onJanScanResult(scannedCode: String, isMatch: Boolean) {
        val itemId = _state.value.currentGroup?.itemId ?: return
        _state.update {
            it.copy(janScanResults = it.janScanResults + (itemId to JanScanResult(scannedCode, isMatch)))
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is NetworkException.Unauthorized -> "認証エラー。再ログインしてください。"
            is NetworkException.NotFound -> "データが見つかりません。"
            is NetworkException.Conflict -> "データが競合しています。再度お試しください。"
            is NetworkException.ValidationError -> error.message ?: "入力エラーです。"
            is NetworkException.NetworkError -> "ネットワークエラー。接続を確認してください。"
            is NetworkException.ServerError -> "サーバーエラーが発生しました。"
            is NetworkException.Unknown -> "エラーが発生しました。"
            else -> error.message ?: "予期しないエラーが発生しました。"
        }
    }
}
