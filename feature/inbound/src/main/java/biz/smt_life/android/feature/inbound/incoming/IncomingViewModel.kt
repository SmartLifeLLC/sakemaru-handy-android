package biz.smt_life.android.feature.inbound.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.Location
import biz.smt_life.android.core.domain.model.StartWorkData
import biz.smt_life.android.core.domain.model.UpdateWorkItemData
import biz.smt_life.android.core.domain.model.WorkItemSchedule
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        val pickerId = tokenManager.getPickerId()
        val pickerName = tokenManager.getPickerName()
        _state.update {
            it.copy(
                pickerId = pickerId,
                pickerName = pickerName
            )
        }
    }

    fun loadWarehouses() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingWarehouses = true, errorMessage = null) }
            repository.getWarehouses()
                .onSuccess { warehouses ->
                    _state.update {
                        it.copy(
                            isLoadingWarehouses = false,
                            warehouses = warehouses
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingWarehouses = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
        }
    }

    fun initializeDefaultWarehouse(force: Boolean = false) {
        val currentState = _state.value
        if (!force && (currentState.selectedWarehouse != null || currentState.isLoadingWarehouses)) {
            return
        }

        val defaultWarehouseId = tokenManager.getDefaultWarehouseId()
        if (defaultWarehouseId <= 0) {
            _state.update {
                it.copy(
                    errorMessage = "作業倉庫が設定されていません。倉庫設定を確認してください。"
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoadingWarehouses = true,
                    errorMessage = null
                )
            }

            repository.getWarehouses()
                .onSuccess { warehouses ->
                    val defaultWarehouse = warehouses.firstOrNull { warehouse ->
                        warehouse.id == defaultWarehouseId
                    }

                    if (defaultWarehouse == null) {
                        _state.update {
                            it.copy(
                                isLoadingWarehouses = false,
                                warehouses = warehouses,
                                errorMessage = "設定済みの作業倉庫が見つかりません。倉庫設定を確認してください。"
                            )
                        }
                        return@onSuccess
                    }

                    _state.update {
                        it.copy(
                            isLoadingWarehouses = false,
                            warehouses = warehouses
                        )
                    }
                    selectWarehouse(defaultWarehouse)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingWarehouses = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
        }
    }

    fun selectWarehouse(warehouse: IncomingWarehouse) {
        _state.update {
            it.copy(
                selectedWarehouse = warehouse,
                products = emptyList(),
                searchQuery = "",
                selectedProductIndex = 0,
                workingScheduleIds = emptySet(),
                isLoadingProducts = false,
                isSearching = false,
                selectedProduct = null,
                selectedScheduleIndex = 0,
                selectedSchedule = null,
                currentWorkItem = null,
                isFromHistory = false,
                inputQuantity = "",
                inputExpirationDate = "",
                inputLocationSearch = "",
                inputLocationId = null,
                inputLocation = null,
                locationSuggestions = emptyList(),
                isLoadingLocations = false,
                historyItems = emptyList(),
                selectedHistoryIndex = 0,
                isLoadingHistory = false,
                successMessage = null,
                errorMessage = null
            )
        }
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            refreshProducts(
                search = null,
                isInitialLoad = true
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            refreshProducts(
                search = query,
                isSearch = true
            )
        }
    }

    fun onProductBarcodeScan(barcode: String) {
        _state.update { it.copy(searchQuery = barcode) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            refreshProducts(
                search = barcode,
                isSearch = true
            )
        }
    }

    fun moveProductSelectionUp() {
        _state.update {
            val newIndex = (it.selectedProductIndex - 1).coerceAtLeast(0)
            it.copy(selectedProductIndex = newIndex)
        }
    }

    fun moveProductSelectionDown() {
        _state.update {
            val maxIndex = (it.products.size - 1).coerceAtLeast(0)
            val newIndex = (it.selectedProductIndex + 1).coerceAtMost(maxIndex)
            it.copy(selectedProductIndex = newIndex)
        }
    }

    fun selectCurrentProduct(): IncomingProduct? {
        val products = _state.value.products
        val index = _state.value.selectedProductIndex
        if (index !in products.indices) return null

        val product = products[index]
        _state.update {
            it.copy(
                selectedProduct = product,
                selectedScheduleIndex = 0
            )
        }
        return product
    }

    fun selectProduct(product: IncomingProduct) {
        _state.update {
            it.copy(
                selectedProduct = product,
                selectedScheduleIndex = 0
            )
        }
    }

    fun moveScheduleSelectionUp() {
        _state.update {
            val newIndex = (it.selectedScheduleIndex - 1).coerceAtLeast(0)
            it.copy(selectedScheduleIndex = newIndex)
        }
    }

    fun moveScheduleSelectionDown() {
        _state.update {
            val schedules = it.selectedProduct?.schedules.orEmpty()
            val maxIndex = (schedules.size - 1).coerceAtLeast(0)
            val newIndex = (it.selectedScheduleIndex + 1).coerceAtMost(maxIndex)
            it.copy(selectedScheduleIndex = newIndex)
        }
    }

    fun selectCurrentSchedule(): IncomingSchedule? {
        val schedules = _state.value.selectedProduct?.schedules.orEmpty()
        val index = _state.value.selectedScheduleIndex
        if (index !in schedules.indices) return null

        val schedule = schedules[index]
        prepareInputForSchedule(schedule, isFromHistory = false)
        return schedule
    }

    fun selectSchedule(schedule: IncomingSchedule) {
        prepareInputForSchedule(schedule, isFromHistory = false)
    }

    private fun prepareInputForSchedule(
        schedule: IncomingSchedule,
        isFromHistory: Boolean,
        workItem: IncomingWorkItem? = null
    ) {
        val workLocation = workItem?.location
        val scheduleLocation = schedule.location
        val inputLocationSearch = when {
            workLocation?.displayName != null -> workLocation.displayName.orEmpty()
            workLocation != null -> workLocation.fullDisplayName
            scheduleLocation?.displayName != null -> scheduleLocation.displayName.orEmpty()
            scheduleLocation != null -> scheduleLocation.fullDisplayName
            else -> ""
        }
        val scheduleIndex = _state.value.selectedProduct?.schedules
            .orEmpty()
            .indexOfFirst { candidate -> candidate.id == schedule.id }

        _state.update {
            it.copy(
                selectedScheduleIndex = if (scheduleIndex >= 0) scheduleIndex else it.selectedScheduleIndex,
                selectedSchedule = schedule,
                currentWorkItem = workItem,
                isFromHistory = isFromHistory,
                inputQuantity = workItem?.workQuantity?.toString() ?: schedule.remainingQuantity.toString(),
                inputExpirationDate = workItem?.workExpirationDate ?: schedule.expirationDate.orEmpty(),
                inputLocationSearch = inputLocationSearch,
                inputLocationId = workItem?.locationId ?: schedule.location?.id,
                inputLocation = workItem?.location ?: schedule.location,
                locationSuggestions = emptyList()
            )
        }
    }

    fun onQuantityChange(value: String) {
        val filtered = value.filter { it.isDigit() }
        _state.update { it.copy(inputQuantity = filtered) }
    }

    fun onExpirationDateChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(8)
        val formatted = buildString {
            digits.forEachIndexed { index, char ->
                if (index == 4 || index == 6) append('-')
                append(char)
            }
        }
        _state.update { it.copy(inputExpirationDate = formatted) }
    }

    fun onLocationSearchChange(value: String) {
        _state.update {
            it.copy(
                inputLocationSearch = value,
                inputLocationId = null,
                inputLocation = null
            )
        }
        locationSearchJob?.cancel()

        if (value.isBlank()) {
            _state.update { it.copy(locationSuggestions = emptyList(), isLoadingLocations = false) }
            return
        }

        locationSearchJob = viewModelScope.launch {
            delay(300)
            val warehouseId = _state.value.selectedWarehouse?.id ?: return@launch
            _state.update { it.copy(isLoadingLocations = true) }
            repository.searchLocations(warehouseId, value, 20)
                .onSuccess { locations ->
                    _state.update {
                        it.copy(
                            isLoadingLocations = false,
                            locationSuggestions = locations
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingLocations = false) }
                }
        }
    }

    fun onLocationBarcodeScan(barcode: String) {
        onLocationSearchChange(barcode)
    }

    fun selectLocation(location: Location) {
        _state.update {
            it.copy(
                inputLocationId = location.id,
                inputLocation = location,
                inputLocationSearch = location.displayName ?: location.fullDisplayName,
                locationSuggestions = emptyList()
            )
        }
    }

    fun setQuantityToExpected() {
        val schedule = _state.value.selectedSchedule ?: return
        _state.update { it.copy(inputQuantity = schedule.remainingQuantity.toString()) }
    }

    fun canSubmit(): Boolean {
        val state = _state.value
        val schedule = state.selectedSchedule ?: return false
        val quantity = state.inputQuantity.toIntOrNull() ?: return false
        return quantity > 0 && quantity <= schedule.remainingQuantity
    }

    fun submitEntry(onSuccess: () -> Unit) {
        val state = _state.value
        val schedule = state.selectedSchedule ?: return
        val warehouseId = state.selectedWarehouse?.id ?: return
        val pickerId = state.pickerId ?: return
        val quantity = state.inputQuantity.toIntOrNull() ?: return
        val expirationDate = state.inputExpirationDate.ifBlank { null }
        val locationId = state.inputLocationId

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                val workItem = if (state.currentWorkItem != null) {
                    state.currentWorkItem
                } else {
                    val startResult = repository.startWork(
                        StartWorkData(
                            incomingScheduleId = schedule.id,
                            pickerId = pickerId,
                            warehouseId = warehouseId
                        )
                    )
                    if (startResult.isFailure) {
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = mapErrorMessage(startResult.exceptionOrNull()!!)
                            )
                        }
                        return@launch
                    }
                    startResult.getOrThrow()
                }

                val updateResult = repository.updateWorkItem(
                    id = workItem.id,
                    data = UpdateWorkItemData(
                        workQuantity = quantity,
                        workArrivalDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        workExpirationDate = expirationDate,
                        locationId = locationId
                    )
                )

                if (updateResult.isFailure) {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = mapErrorMessage(updateResult.exceptionOrNull()!!)
                        )
                    }
                    return@launch
                }

                if (!state.isFromHistory) {
                    val completeResult = repository.completeWorkItem(workItem.id)
                    if (completeResult.isFailure) {
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = mapErrorMessage(completeResult.exceptionOrNull()!!)
                            )
                        }
                        return@launch
                    }
                }

                _state.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = if (state.isFromHistory) "更新しました" else "入庫を確定しました",
                        currentWorkItem = null
                    )
                }

                if (state.isFromHistory) {
                    _state.update { it.copy(searchQuery = "") }
                    loadHistory()
                    refreshProducts(search = null)
                } else {
                    refreshProducts(search = _state.value.searchQuery)
                }
                delay(1500)
                _state.update {
                    it.copy(
                        selectedSchedule = null,
                        currentWorkItem = null,
                        isFromHistory = false,
                        inputQuantity = "",
                        inputExpirationDate = "",
                        inputLocationSearch = "",
                        inputLocationId = null,
                        inputLocation = null,
                        locationSuggestions = emptyList(),
                        successMessage = null
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = mapErrorMessage(e)
                    )
                }
            }
        }
    }

    fun loadHistory() {
        val warehouseId = _state.value.selectedWarehouse?.id ?: return
        val pickerId = _state.value.pickerId
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        viewModelScope.launch {
            _state.update { it.copy(isLoadingHistory = true, errorMessage = null) }
            repository.getWorkItems(
                warehouseId = warehouseId,
                pickerId = pickerId,
                status = "all",
                fromDate = today,
                toDate = today,
                limit = 100
            )
                .onSuccess { items ->
                    _state.update {
                        it.copy(
                            isLoadingHistory = false,
                            historyItems = items,
                            selectedHistoryIndex = 0
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingHistory = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
        }
    }

    fun moveHistorySelectionUp() {
        _state.update {
            val newIndex = (it.selectedHistoryIndex - 1).coerceAtLeast(0)
            it.copy(selectedHistoryIndex = newIndex)
        }
    }

    fun moveHistorySelectionDown() {
        _state.update {
            val maxIndex = (it.historyItems.size - 1).coerceAtLeast(0)
            val newIndex = (it.selectedHistoryIndex + 1).coerceAtMost(maxIndex)
            it.copy(selectedHistoryIndex = newIndex)
        }
    }

    fun selectHistoryItem(workItem: IncomingWorkItem): Boolean {
        if (!workItem.canEditFromHistory) {
            _state.update { it.copy(errorMessage = "このスケジュールは編集できません") }
            return false
        }

        val scheduleSummary = workItem.schedule ?: run {
            _state.update { it.copy(errorMessage = "このスケジュールは編集できません") }
            return false
        }

        val schedule = IncomingSchedule(
            id = workItem.incomingScheduleId,
            warehouseId = scheduleSummary.warehouseId ?: workItem.warehouseId,
            warehouseName = scheduleSummary.warehouseName,
            expectedQuantity = scheduleSummary.expectedQuantity,
            receivedQuantity = scheduleSummary.receivedQuantity,
            remainingQuantity = scheduleSummary.remainingQuantity,
            expectedArrivalDate = scheduleSummary.expectedArrivalDate,
            expirationDate = workItem.workExpirationDate,
            status = scheduleSummary.status,
            location = workItem.location
        )

        val contextProduct = _state.value.products.firstOrNull { it.itemId == scheduleSummary.itemId }
            ?: createHistoryContextProduct(scheduleSummary, schedule)
        val productIndex = _state.value.products.indexOfFirst { it.itemId == contextProduct.itemId }
        val resolvedSchedule = contextProduct.schedules.firstOrNull { it.id == schedule.id } ?: schedule
        val scheduleIndex = contextProduct.schedules.indexOfFirst { it.id == resolvedSchedule.id }

        _state.update {
            it.copy(
                selectedProduct = contextProduct,
                selectedProductIndex = if (productIndex >= 0) productIndex else 0,
                selectedScheduleIndex = if (scheduleIndex >= 0) scheduleIndex else 0,
                errorMessage = null
            )
        }

        prepareInputForSchedule(resolvedSchedule, isFromHistory = true, workItem = workItem)
        return true
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _state.update { it.copy(successMessage = null) }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun resetToWarehouseSelection() {
        _state.update {
            IncomingState(
                pickerId = it.pickerId,
                pickerName = it.pickerName,
                warehouses = it.warehouses
            )
        }
    }

    fun resetToProductList() {
        _state.update {
            it.copy(
                selectedProduct = null,
                selectedSchedule = null,
                currentWorkItem = null,
                isFromHistory = false,
                inputQuantity = "",
                inputExpirationDate = "",
                inputLocationSearch = "",
                inputLocationId = null,
                inputLocation = null,
                locationSuggestions = emptyList(),
                successMessage = null,
                errorMessage = null
            )
        }
    }

    private suspend fun refreshProducts(
        search: String? = null,
        isInitialLoad: Boolean = false,
        isSearch: Boolean = false
    ) {
        val warehouseId = _state.value.selectedWarehouse?.id ?: return
        val pickerId = _state.value.pickerId ?: return
        val normalizedSearch = search?.ifBlank { null }

        if (isInitialLoad) {
            _state.update { it.copy(isLoadingProducts = true, errorMessage = null) }
        } else if (isSearch) {
            _state.update { it.copy(isSearching = true, errorMessage = null) }
        }

        val productsResult: Result<List<IncomingProduct>>
        val workingIdsResult: Result<Set<Int>>

        coroutineScope {
            val productsDeferred = async { repository.getSchedules(warehouseId, normalizedSearch) }
            val workingIdsDeferred = async { repository.getWorkingScheduleIds(warehouseId, pickerId) }
            productsResult = productsDeferred.await()
            workingIdsResult = workingIdsDeferred.await()
        }

        productsResult
            .onSuccess { products ->
                val current = _state.value
                val workingIds = workingIdsResult.getOrDefault(emptySet())
                val preservedSelectedProduct = current.selectedProduct?.itemId?.let { selectedItemId ->
                    products.firstOrNull { it.itemId == selectedItemId }
                }
                val nextSelectedProductIndex = current.selectedProductIndex.coerceIn(
                    minimumValue = 0,
                    maximumValue = (products.size - 1).coerceAtLeast(0)
                )

                _state.update {
                    it.copy(
                        isLoadingProducts = false,
                        isSearching = false,
                        products = products,
                        workingScheduleIds = workingIds,
                        selectedProduct = preservedSelectedProduct,
                        selectedProductIndex = if (products.isEmpty()) 0 else nextSelectedProductIndex
                    )
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        isLoadingProducts = false,
                        isSearching = false,
                        errorMessage = mapErrorMessage(error)
                    )
                }
            }
    }

    private fun createHistoryContextProduct(
        scheduleSummary: WorkItemSchedule,
        schedule: IncomingSchedule
    ) = IncomingProduct(
        itemId = scheduleSummary.itemId,
        itemCode = scheduleSummary.itemCode.orEmpty(),
        itemName = scheduleSummary.itemName.orEmpty(),
        janCodes = scheduleSummary.janCodes,
        totalExpectedQuantity = scheduleSummary.expectedQuantity,
        totalReceivedQuantity = scheduleSummary.receivedQuantity,
        totalRemainingQuantity = scheduleSummary.remainingQuantity,
        schedules = listOf(schedule)
    )

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is NetworkException.Unauthorized -> "認証エラー。再ログインしてください。"
            is NetworkException.Forbidden -> "アクセス権限がありません。"
            is NetworkException.NotFound -> "データが見つかりません。"
            is NetworkException.NetworkError -> "ネットワークエラー。接続を確認してください。"
            is NetworkException.ServerError -> "サーバーエラー。しばらくしてから再度お試しください。"
            is NetworkException.ValidationError -> error.message ?: "入力エラーです。"
            else -> error.message ?: "エラーが発生しました。"
        }
    }
}
