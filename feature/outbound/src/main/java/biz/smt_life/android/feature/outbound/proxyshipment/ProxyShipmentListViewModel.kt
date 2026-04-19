package biz.smt_life.android.feature.outbound.proxyshipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.repository.IncomingRepository
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
class ProxyShipmentListViewModel @Inject constructor(
    private val proxyShipmentRepository: ProxyShipmentRepository,
    private val incomingRepository: IncomingRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProxyShipmentListState())
    val state: StateFlow<ProxyShipmentListState> = _state.asStateFlow()

    init {
        observeAllocations()
        initialize()
    }

    private fun observeAllocations() {
        viewModelScope.launch {
            proxyShipmentRepository.allocationsFlow.collect { allocations ->
                _state.update { current ->
                    if (current.hasLoadedOnce) {
                        current.copy(items = allocations)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun initialize() {
        val warehouseId = tokenManager.getDefaultWarehouseId()
        if (warehouseId <= 0) {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "倉庫情報が見つかりません。再ログインしてください。"
                )
            }
            return
        }

        _state.update { it.copy(warehouseId = warehouseId) }
        loadWarehouseName(warehouseId)
        loadAllocations(initialLoad = true)
    }

    private fun loadWarehouseName(warehouseId: Int) {
        viewModelScope.launch {
            incomingRepository.getWarehouses()
                .onSuccess { warehouses ->
                    val warehouseName = warehouses.firstOrNull { it.id == warehouseId }?.name.orEmpty()
                    _state.update { it.copy(warehouseName = warehouseName) }
                }
        }
    }

    fun refresh() {
        loadAllocations(initialLoad = false, isRefresh = true)
    }

    fun selectTab(tab: ProxyShipmentTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun selectCourse(courseId: Int?) {
        _state.update { it.copy(selectedCourseId = courseId) }
        loadAllocations(initialLoad = false)
    }

    fun selectDate(apiDate: String) {
        _state.update { it.copy(selectedDateApi = apiDate) }
        loadAllocations(initialLoad = false)
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    internal fun openGroup(
        group: ProxyShipmentCourseGroup,
        onNavigateToPicking: (String, String) -> Unit
    ) {
        onNavigateToPicking(group.shipmentDate, group.courseKey)
    }

    private fun loadAllocations(initialLoad: Boolean, isRefresh: Boolean = false) {
        val current = _state.value
        val warehouseId = current.warehouseId
        if (warehouseId <= 0) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = initialLoad && !it.hasLoadedOnce,
                    isRefreshing = isRefresh
                )
            }

            proxyShipmentRepository.getAllocations(
                warehouseId = warehouseId,
                shipmentDate = current.selectedDateApi.takeIf { !initialLoad || current.hasLoadedOnce },
                deliveryCourseId = current.selectedCourseId
            ).onSuccess { payload ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        hasLoadedOnce = true,
                        items = payload.items,
                        courseOptions = payload.summary.byDeliveryCourse,
                        totalCount = payload.summary.totalCount,
                        businessDateApi = payload.businessDate,
                        selectedDateApi = if (initialLoad && !it.hasLoadedOnce) {
                            payload.businessDate
                        } else {
                            current.selectedDateApi ?: payload.businessDate
                        }
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = mapErrorMessage(throwable)
                    )
                }
            }
        }
    }

    private fun mapErrorMessage(throwable: Throwable): String = when (throwable) {
        is NetworkException -> throwable.message ?: "通信エラーが発生しました"
        else -> throwable.message ?: "横持ち出荷の取得に失敗しました"
    }
}
