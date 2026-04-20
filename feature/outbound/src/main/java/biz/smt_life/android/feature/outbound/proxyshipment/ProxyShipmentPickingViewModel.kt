package biz.smt_life.android.feature.outbound.proxyshipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.ProxyShipmentAllocation
import biz.smt_life.android.core.domain.model.ProxyShipmentCompletionResult
import biz.smt_life.android.core.domain.model.ProxyShipmentStatus
import biz.smt_life.android.core.domain.repository.ProxyShipmentRepository
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProxyShipmentPickingViewModel @Inject constructor(
    private val proxyShipmentRepository: ProxyShipmentRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProxyShipmentPickingState())
    val state: StateFlow<ProxyShipmentPickingState> = _state.asStateFlow()

    private var currentAllocationId: Int? = null
    private var currentShipmentDate: String? = null
    private var currentCourseKey: String? = null

    fun initialize(shipmentDate: String, courseKey: String) {
        if (
            currentShipmentDate == shipmentDate &&
            currentCourseKey == courseKey &&
            state.value.detail != null
        ) {
            return
        }

        currentShipmentDate = shipmentDate
        currentCourseKey = courseKey
        currentAllocationId = null
        val warehouseId = tokenManager.getDefaultWarehouseId()
        if (warehouseId <= 0) {
            _state.update {
                it.copy(isLoading = false, errorMessage = "倉庫情報が見つかりません")
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    warehouseId = warehouseId,
                    shipmentDate = shipmentDate,
                    courseKey = courseKey,
                    groupAllocations = emptyList(),
                    initialAllocationCount = 0,
                    currentAllocationIndex = 0,
                    detail = null,
                    pickedQtyInput = "",
                    janScanFeedback = null
                )
            }

            ensureGroupAllocations(warehouseId, shipmentDate, courseKey)
                .onSuccess { allocations ->
                    if (allocations.isEmpty()) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "該当する配送コースの横持ち出荷が見つかりません"
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                groupAllocations = allocations,
                                initialAllocationCount = allocations.size,
                                currentAllocationIndex = 0
                            )
                        }
                        loadAllocationAt(0)
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapErrorMessage(throwable)
                        )
                    }
                }
        }
    }

    fun moveToPrev() {
        val previousIndex = state.value.currentAllocationIndex - 1
        if (previousIndex < 0) return
        loadAllocationAt(previousIndex)
    }

    fun moveToNext() {
        val nextIndex = state.value.currentAllocationIndex + 1
        if (nextIndex > state.value.groupAllocations.lastIndex) return
        loadAllocationAt(nextIndex)
    }

    fun onPickedQtyInputChange(value: String) {
        if (value.isNotEmpty() && value.any { !it.isDigit() }) return
        _state.update { it.copy(pickedQtyInput = value) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun showScanner() {
        _state.update { it.copy(showJanScannerDialog = true) }
    }

    fun dismissScanner() {
        _state.update { it.copy(showJanScannerDialog = false) }
    }

    fun showImages() {
        _state.update { it.copy(showImageDialog = true) }
    }

    fun dismissImages() {
        _state.update { it.copy(showImageDialog = false) }
    }

    fun onJanScanResult(code: String, isMatch: Boolean) {
        _state.update {
            it.copy(
                showJanScannerDialog = false,
                janScanFeedback = ProxyShipmentJanScanFeedback(
                    scannedCode = code,
                    isMatch = isMatch
                )
            )
        }
    }

    fun update(onSuccess: (() -> Unit)? = null) {
        val allocationId = currentAllocationId ?: return
        val warehouseId = state.value.warehouseId
        val pickedQty = state.value.parsedPickedQty

        if (warehouseId <= 0 || pickedQty == null || state.value.quantityErrorMessage != null) {
            _state.update { it.copy(errorMessage = it.quantityErrorMessage ?: "数量を確認してください") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            proxyShipmentRepository.updateAllocation(allocationId, warehouseId, pickedQty)
                .onSuccess { allocation ->
                    upsertGroupAllocation(allocation)
                    _state.update { current ->
                        current.copy(
                            isSubmitting = false,
                            detail = current.detail?.copy(allocation = allocation),
                            pickedQtyInput = allocation.pickedQty.toString()
                        )
                    }
                    onSuccess?.invoke()
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = mapErrorMessage(throwable)
                        )
                    }
                }
        }
    }

    fun complete(onFinished: (ProxyShipmentCompletionResult) -> Unit) {
        val allocationId = currentAllocationId ?: return
        val warehouseId = state.value.warehouseId
        val pickedQty = state.value.parsedPickedQty

        if (warehouseId <= 0 || pickedQty == null || state.value.quantityErrorMessage != null) {
            _state.update { it.copy(errorMessage = it.quantityErrorMessage ?: "数量を確認してください") }
            return
        }

        viewModelScope.launch {
            val currentIndex = state.value.currentAllocationIndex
            _state.update { it.copy(isSubmitting = true) }
            proxyShipmentRepository.completeAllocation(allocationId, warehouseId, pickedQty)
                .onSuccess { completion ->
                    val shipmentDate = state.value.shipmentDate
                    val courseKey = state.value.courseKey
                    val remaining = proxyShipmentRepository.allocationsFlow.value
                        .findProxyShipmentGroup(shipmentDate, courseKey)
                        .ifEmpty {
                            state.value.groupAllocations.filterNot { it.allocationId == allocationId }
                        }

                    if (remaining.isEmpty()) {
                        _state.update { current ->
                            current.copy(
                                isSubmitting = false,
                                detail = current.detail?.copy(allocation = completion.allocation),
                                pickedQtyInput = completion.allocation.pickedQty.toString()
                            )
                        }
                        onFinished(completion)
                    } else {
                        val nextIndex = currentIndex.coerceAtMost(remaining.lastIndex)
                        _state.update { current ->
                            current.copy(
                                isSubmitting = false,
                                groupAllocations = remaining,
                                currentAllocationIndex = nextIndex,
                                detail = null,
                                pickedQtyInput = "",
                                janScanFeedback = null
                            )
                        }
                        loadAllocationAt(nextIndex)
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = mapErrorMessage(throwable)
                        )
                    }
                }
        }
    }

    private suspend fun ensureGroupAllocations(
        warehouseId: Int,
        shipmentDate: String,
        courseKey: String
    ): Result<List<ProxyShipmentAllocation>> {
        val cached = proxyShipmentRepository.allocationsFlow.value
            .findProxyShipmentGroup(shipmentDate, courseKey)
        if (cached.isNotEmpty()) {
            return Result.success(cached)
        }

        return proxyShipmentRepository.getAllocations(
            warehouseId = warehouseId,
            shipmentDate = shipmentDate
        ).map {
            proxyShipmentRepository.allocationsFlow.value.findProxyShipmentGroup(shipmentDate, courseKey)
        }
    }

    private fun loadAllocationAt(index: Int) {
        val warehouseId = state.value.warehouseId
        val targetAllocation = state.value.groupAllocations.getOrNull(index) ?: return
        currentAllocationId = targetAllocation.allocationId

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    currentAllocationIndex = index,
                    janScanFeedback = null
                )
            }

            val activeAllocation = if (targetAllocation.status == ProxyShipmentStatus.RESERVED) {
                proxyShipmentRepository.startAllocation(targetAllocation.allocationId, warehouseId)
                    .onSuccess(::upsertGroupAllocation)
                    .getOrElse { throwable ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = mapErrorMessage(throwable)
                            )
                        }
                        return@launch
                    }
            } else {
                targetAllocation
            }

            proxyShipmentRepository.getAllocationDetail(activeAllocation.allocationId, warehouseId)
                .onSuccess { detail ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            detail = detail,
                            pickedQtyInput = detail.allocation.pickedQty.toString()
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapErrorMessage(throwable)
                        )
                    }
                }
        }
    }

    private fun upsertGroupAllocation(allocation: ProxyShipmentAllocation) {
        _state.update { current ->
            val updatedAllocations = current.groupAllocations
                .map { existing ->
                    if (existing.allocationId == allocation.allocationId) allocation else existing
                }
                .findProxyShipmentGroup(current.shipmentDate, current.courseKey)

            val newIndex = updatedAllocations.indexOfFirst { it.allocationId == allocation.allocationId }
                .coerceAtLeast(0)

            current.copy(
                groupAllocations = updatedAllocations,
                currentAllocationIndex = newIndex
            )
        }
    }

    private fun mapErrorMessage(throwable: Throwable): String = when (throwable) {
        is NetworkException -> throwable.message ?: "通信エラーが発生しました"
        else -> throwable.message ?: "横持ち出荷の操作に失敗しました"
    }
}
