package biz.smt_life.android.feature.outbound.proxyshipment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.repository.ProxyShipmentRepository
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProxyShipmentResultViewModel @Inject constructor(
    private val proxyShipmentRepository: ProxyShipmentRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProxyShipmentResultState())
    val state: StateFlow<ProxyShipmentResultState> = _state.asStateFlow()

    private var currentAllocationId: Int? = null

    fun initialize(allocationId: Int) {
        if (currentAllocationId == allocationId && state.value.completion != null) return
        currentAllocationId = allocationId

        val warehouseId = tokenManager.getDefaultWarehouseId()
        if (warehouseId <= 0) {
            _state.update {
                it.copy(isLoading = false, errorMessage = "倉庫情報が見つかりません")
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val cached = proxyShipmentRepository.completionFlow(allocationId).first()
            if (cached != null) {
                _state.update { it.copy(isLoading = false, completion = cached) }
                return@launch
            }

            proxyShipmentRepository.completeAllocation(
                allocationId = allocationId,
                warehouseId = warehouseId,
                pickedQty = null
            ).onSuccess { completion ->
                _state.update {
                    it.copy(isLoading = false, completion = completion)
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapErrorMessage(throwable)
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun mapErrorMessage(throwable: Throwable): String = when (throwable) {
        is NetworkException -> throwable.message ?: "通信エラーが発生しました"
        else -> throwable.message ?: "完了結果の取得に失敗しました"
    }
}
