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
        val grouped = groupItemsInternal(task.items)

        val isSameTask = _state.value.originalTask?.taskId == task.taskId

        val startIndex = if (isSameTask && _state.value.groupedItems.isNotEmpty()) {
            _state.value.currentGroupIndex.coerceIn(0, maxOf(0, grouped.size - 1))
        } else if (editItemId != null) {
            grouped.indexOfFirst { it.itemId == editItemId }.coerceAtLeast(0)
        } else {
            grouped.indexOfFirst { group -> 
                task.items.any { it.itemId == group.itemId && it.status == ItemStatus.PENDING }
            }.coerceAtLeast(0)
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
                    totalCaseInput = if (isSameTask) it.totalCaseInput else formatTotal(targetGroup, QuantityType.CASE),
                    totalPieceInput = if (isSameTask) it.totalPieceInput else formatTotal(targetGroup, QuantityType.PIECE),
                    isLoading = false,
                    warehouseId = warehouseId
                )
            }
        }
        loadWarehouseName(warehouseId)
    }

    // ===== Grouping Logic =====

    private fun groupItemsInternal(
        items: List<PickingTaskItem>
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
                            val initialQty = if (it.status != ItemStatus.PENDING) it.pickedQty else it.plannedQty
                            CustomerEntryDetail(
                                pickingItemResultId = it.id,
                                plannedQty = it.plannedQty,
                                pickedQtyInput = String.format("%.0f", initialQty)
                            )
                        },
                        pieceEntry = pieceItem?.let {
                            val initialQty = if (it.status != ItemStatus.PENDING) it.pickedQty else it.plannedQty
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
            QuantityType.CASE -> group.customerEntries.mapNotNull { it.caseEntry }.sumOf { it.pickedQtyInput.toDoubleOrNull() ?: 0.0 }
            QuantityType.PIECE -> group.customerEntries.mapNotNull { it.pieceEntry }.sumOf { it.pickedQtyInput.toDoubleOrNull() ?: 0.0 }
        }
        return String.format("%.0f", total)
    }

    // ===== Total Input Handlers (auto-distribute) =====

    fun onTotalCaseInputChange(value: String) {
        if (value.isNotEmpty() && !value.matches(Regex("^\\d*\\.?\\d*$"))) return
        val totalInput = value.toDoubleOrNull() ?: 0.0

        _state.update { it.copy(totalCaseInput = value, quantityErrorMessage = null) }
        val group = _state.value.currentGroup ?: return

        var remaining = totalInput
        val updatedEntries = group.customerEntries.map { entry ->
            if (entry.caseEntry != null) {
                val allocated = minOf(remaining, entry.caseEntry.plannedQty)
                remaining -= allocated
                entry.copy(caseEntry = entry.caseEntry.copy(
                    pickedQtyInput = if (value.isEmpty()) "" else String.format("%.0f", allocated)
                ))
            } else entry
        }
        updateCurrentGroupEntries(updatedEntries)
    }

    fun onTotalPieceInputChange(value: String) {
        if (value.isNotEmpty() && !value.matches(Regex("^\\d*\\.?\\d*$"))) return
        val totalInput = value.toDoubleOrNull() ?: 0.0

        _state.update { it.copy(totalPieceInput = value, quantityErrorMessage = null) }
        val group = _state.value.currentGroup ?: return

        var remaining = totalInput
        val updatedEntries = group.customerEntries.map { entry ->
            if (entry.pieceEntry != null) {
                val allocated = minOf(remaining, entry.pieceEntry.plannedQty)
                remaining -= allocated
                entry.copy(pieceEntry = entry.pieceEntry.copy(
                    pickedQtyInput = if (value.isEmpty()) "" else String.format("%.0f", allocated)
                ))
            } else entry
        }
        updateCurrentGroupEntries(updatedEntries)
    }

    // ===== Individual Input Handlers =====

    fun onCustomerCaseQtyChange(customerIndex: Int, value: String) {
        if (value.isNotEmpty() && !value.matches(Regex("^\\d*\\.?\\d*$"))) return
        _state.update { it.copy(quantityErrorMessage = null) }
        val group = _state.value.currentGroup ?: return
        val entry = group.customerEntries.getOrNull(customerIndex) ?: return
        val caseEntry = entry.caseEntry ?: return

        // Removed hard block `if (inputQty > caseEntry.plannedQty) return` to let error show

        val updatedEntries = group.customerEntries.toMutableList()
        updatedEntries[customerIndex] = entry.copy(
            caseEntry = caseEntry.copy(pickedQtyInput = value)
        )
        updateCurrentGroupEntries(updatedEntries)
        recalculateTotalCase()
    }

    fun onCustomerPieceQtyChange(customerIndex: Int, value: String) {
        if (value.isNotEmpty() && !value.matches(Regex("^\\d*\\.?\\d*$"))) return
        _state.update { it.copy(quantityErrorMessage = null) }
        val group = _state.value.currentGroup ?: return
        val entry = group.customerEntries.getOrNull(customerIndex) ?: return
        val pieceEntry = entry.pieceEntry ?: return

        // Removed hard block

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
        if (_state.value.hasQuantityError) {
            _state.update { it.copy(quantityErrorMessage = "入力数字エラー") }
            return
        }
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
        val grouped = groupItemsInternal(refreshedTask.items)
        val currentIndex = _state.value.currentGroupIndex

        if (refreshedTask.isFullyProcessed) {
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
            // Sequential movement: move to the next index first.
            // If already at the last index, jump back to the first PENDING item.
            val targetIndex = if (currentIndex < (grouped.size - 1)) {
                currentIndex + 1
            } else {
                grouped.indexOfFirst { group -> 
                    refreshedTask.items.any { it.itemId == group.itemId && it.status == ItemStatus.PENDING }
                }.coerceAtLeast(0)
            }
            
            val targetGroup = grouped[targetIndex]
            _state.update {
                it.copy(
                    groupedItems = grouped,
                    currentGroupIndex = targetIndex,
                    totalCaseInput = formatTotal(targetGroup, QuantityType.CASE),
                    totalPieceInput = formatTotal(targetGroup, QuantityType.PIECE),
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

    fun showJanScannerDialog(isInCamera: Boolean = false) {
        _state.update { it.copy(showJanScannerDialog = true, isJanScannerInCamera = isInCamera) }
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
