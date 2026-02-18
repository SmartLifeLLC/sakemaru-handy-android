package biz.smt_life.android.feature.inbound.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.Location
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class IncomingViewModel @Inject constructor(
    private val repository: IncomingRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(IncomingState())
    val state: StateFlow<IncomingState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var locationSearchJob: Job? = null

    init {
        _state.update {
            it.copy(
                pickerId = tokenManager.getPickerId(),
                pickerName = tokenManager.getPickerName()
            )
        }
    }

    // ─── P10: Warehouse ───

    fun loadWarehouses() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingWarehouses = true, errorMessage = null) }
            repository.getWarehouses()
                .onSuccess { warehouses ->
                    _state.update { it.copy(warehouses = warehouses, isLoadingWarehouses = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingWarehouses = false, errorMessage = mapError(e)) }
                }
        }
    }

    fun selectWarehouse(warehouse: biz.smt_life.android.core.domain.model.IncomingWarehouse) {
        _state.update { it.copy(selectedWarehouse = warehouse) }
    }

    // ─── P11: Product List ───

    fun loadProducts() {
        val warehouseId = _state.value.selectedWarehouse?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, errorMessage = null) }
            repository.getSchedules(warehouseId)
                .onSuccess { products ->
                    _state.update { it.copy(products = products, isSearching = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSearching = false, errorMessage = mapError(e)) }
                }
            // Load working schedule IDs
            loadWorkingScheduleIds()
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            searchProducts(query)
        }
    }

    private suspend fun searchProducts(query: String) {
        val warehouseId = _state.value.selectedWarehouse?.id ?: return
        _state.update { it.copy(isSearching = true) }
        repository.getSchedules(warehouseId, query.ifBlank { null })
            .onSuccess { products ->
                _state.update { it.copy(products = products, isSearching = false) }
            }
            .onFailure { e ->
                _state.update { it.copy(isSearching = false, errorMessage = mapError(e)) }
            }
    }

    private suspend fun loadWorkingScheduleIds() {
        val warehouseId = _state.value.selectedWarehouse?.id ?: return
        val pickerId = _state.value.pickerId ?: return
        repository.getWorkItems(warehouseId, pickerId, "WORKING")
            .onSuccess { workItems ->
                _state.update { it.copy(workingScheduleIds = workItems.map { wi -> wi.incomingScheduleId }.toSet()) }
            }
    }

    fun selectProduct(product: IncomingProduct) {
        _state.update { it.copy(selectedProduct = product) }
    }

    // ─── P12: Schedule List ───

    fun selectSchedule(schedule: IncomingSchedule) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        _state.update {
            it.copy(
                selectedSchedule = schedule,
                isFromHistory = false,
                currentWorkItem = null,
                inputQuantity = schedule.remainingQuantity.toString(),
                inputExpirationDate = "",
                inputLocationSearch = "",
                inputLocationId = null,
                inputLocation = null,
                locationSuggestions = emptyList()
            )
        }
    }

    // ─── P13: Input ───

    fun onQuantityChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _state.update { it.copy(inputQuantity = value) }
        }
    }

    fun onExpirationDateChange(value: String) {
        _state.update { it.copy(inputExpirationDate = value) }
    }

    fun onLocationSearchChange(query: String) {
        _state.update { it.copy(inputLocationSearch = query, inputLocationId = null, inputLocation = null) }
        locationSearchJob?.cancel()
        locationSearchJob = viewModelScope.launch {
            delay(300) // debounce
            searchLocations(query)
        }
    }

    private suspend fun searchLocations(query: String) {
        val warehouseId = _state.value.selectedWarehouse?.id ?: return
        if (query.isBlank()) {
            _state.update { it.copy(locationSuggestions = emptyList()) }
            return
        }
        _state.update { it.copy(isLoadingLocations = true) }
        repository.getLocations(warehouseId, query)
            .onSuccess { locations ->
                _state.update { it.copy(locationSuggestions = locations, isLoadingLocations = false) }
            }
            .onFailure {
                _state.update { it.copy(isLoadingLocations = false) }
            }
    }

    fun selectLocation(location: Location) {
        _state.update {
            it.copy(
                inputLocationId = location.id,
                inputLocation = location,
                inputLocationSearch = location.displayName,
                locationSuggestions = emptyList()
            )
        }
    }

    fun submitIncoming() {
        val scheduleId = _state.value.selectedSchedule?.id ?: return
        val pickerId = _state.value.pickerId ?: return
        val warehouseId = _state.value.selectedWarehouse?.id ?: return
        val quantity = _state.value.inputQuantity.toIntOrNull() ?: return
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val expirationDate = _state.value.inputExpirationDate.ifBlank { null }
        val locationId = _state.value.inputLocationId

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }

            if (_state.value.isFromHistory && _state.value.currentWorkItem != null) {
                // Edit flow: update existing work item
                val workItemId = _state.value.currentWorkItem!!.id
                repository.updateWork(workItemId, quantity, today, expirationDate, locationId)
                    .onSuccess {
                        _state.update { it.copy(isSubmitting = false, successMessage = "更新しました") }
                        // Refresh products for updated quantities
                        refreshAfterSubmit()
                    }
                    .onFailure { e ->
                        _state.update { it.copy(isSubmitting = false, errorMessage = mapError(e)) }
                    }
            } else {
                // New flow: start → update → complete
                repository.startWork(scheduleId, pickerId, warehouseId)
                    .onSuccess { workItem ->
                        repository.updateWork(workItem.id, quantity, today, expirationDate, locationId)
                            .onSuccess {
                                repository.completeWork(workItem.id)
                                    .onSuccess {
                                        _state.update { it.copy(isSubmitting = false, successMessage = "入庫を確定しました") }
                                        refreshAfterSubmit()
                                    }
                                    .onFailure { e ->
                                        _state.update { it.copy(isSubmitting = false, errorMessage = mapError(e)) }
                                    }
                            }
                            .onFailure { e ->
                                _state.update { it.copy(isSubmitting = false, errorMessage = mapError(e)) }
                            }
                    }
                    .onFailure { e ->
                        _state.update { it.copy(isSubmitting = false, errorMessage = mapError(e)) }
                    }
            }
        }
    }

    private suspend fun refreshAfterSubmit() {
        delay(1500) // Show success message
        _state.update { it.copy(successMessage = null) }
        // Refresh product list with updated quantities
        val warehouseId = _state.value.selectedWarehouse?.id ?: return
        repository.getSchedules(warehouseId, _state.value.searchQuery.ifBlank { null })
            .onSuccess { products ->
                _state.update { s ->
                    // Update selectedProduct with refreshed data
                    val updatedProduct = products.find { it.itemId == s.selectedProduct?.itemId }
                    s.copy(products = products, selectedProduct = updatedProduct)
                }
            }
    }

    // ─── P14: History ───

    fun loadHistory() {
        val warehouseId = _state.value.selectedWarehouse?.id ?: return
        val pickerId = _state.value.pickerId
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            _state.update { it.copy(isLoadingHistory = true, errorMessage = null) }
            repository.getWorkItems(warehouseId, pickerId, "all", today)
                .onSuccess { items ->
                    _state.update { it.copy(historyItems = items, isLoadingHistory = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingHistory = false, errorMessage = mapError(e)) }
                }
        }
    }

    fun selectHistoryItem(workItem: IncomingWorkItem) {
        _state.update {
            it.copy(
                currentWorkItem = workItem,
                isFromHistory = true,
                selectedSchedule = null, // Will use workItem's schedule info
                inputQuantity = workItem.workQuantity.toString(),
                inputExpirationDate = workItem.workExpirationDate ?: "",
                inputLocationSearch = workItem.location?.displayName ?: "",
                inputLocationId = workItem.locationId,
                inputLocation = workItem.location
            )
        }
    }

    // ─── Common ───

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _state.update { it.copy(successMessage = null) }
    }

    private fun mapError(e: Throwable): String = when (e) {
        is NetworkException.Unauthorized -> "認証エラー。再ログインしてください。"
        is NetworkException.Forbidden -> "アクセス権限がありません。"
        is NetworkException.NotFound -> "データが見つかりません。"
        is NetworkException.ValidationError -> e.message ?: "入力エラーです。"
        is NetworkException.NetworkError -> "ネットワークエラー。接続を確認してください。"
        is NetworkException.ServerError -> "サーバーエラー。しばらくしてから再度お試しください。"
        else -> e.message ?: "エラーが発生しました。"
    }
}
