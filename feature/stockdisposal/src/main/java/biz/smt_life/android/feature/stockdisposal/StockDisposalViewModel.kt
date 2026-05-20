package biz.smt_life.android.feature.stockdisposal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.StockDisposalItem
import biz.smt_life.android.core.domain.model.StockDisposalReason
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.domain.repository.StockDisposalCreateRequest
import biz.smt_life.android.core.domain.repository.StockDisposalDetail
import biz.smt_life.android.core.domain.repository.StockDisposalEntry
import biz.smt_life.android.core.domain.repository.StockDisposalRepository
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StockDisposalViewModel @Inject constructor(
    private val stockDisposalRepository: StockDisposalRepository,
    private val incomingRepository: IncomingRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockDisposalUiState())
    val uiState: StateFlow<StockDisposalUiState> = _uiState.asStateFlow()

    private var warehouseCode: String? = null

    init {
        resolveWarehouseCode()
    }

    private fun resolveWarehouseCode() {
        viewModelScope.launch {
            val warehouseId = tokenManager.getDefaultWarehouseId()
            if (warehouseId > 0) {
                incomingRepository.getWarehouses().onSuccess { warehouses ->
                    val warehouse = warehouses.find { it.id == warehouseId }
                    warehouseCode = warehouse?.code
                    _uiState.update {
                        it.copy(
                            warehouseName = warehouse?.name.orEmpty(),
                            warehouseCode = warehouse?.code.orEmpty()
                        )
                    }
                }
            }
        }
    }

    fun onSearchKeywordChange(keyword: String) {
        _uiState.update {
            it.copy(
                searchKeyword = keyword,
                hasSearched = false,
                items = emptyList(),
                error = null
            )
        }
    }

    fun onBarcodeScan(code: String) {
        onSearchKeywordChange(code)
    }

    fun onSearch() {
        onSearch(null)
    }

    private fun onSearch(overrideKeyword: String? = null) {
        val keyword = (overrideKeyword ?: _uiState.value.searchKeyword).trim()
        if (keyword.isEmpty()) return

        val code = warehouseCode
        if (code.isNullOrEmpty()) {
            _uiState.update { it.copy(error = "倉庫が設定されていません") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    hasSearched = true,
                    error = null,
                    items = emptyList()
                )
            }
            stockDisposalRepository.searchItems(
                keyword = keyword,
                warehouseCode = code
            ).onSuccess { items ->
                _uiState.update { it.copy(isSearching = false, items = items) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSearching = false, error = e.message ?: "検索に失敗しました") }
            }
        }
    }

    fun onItemSelected(item: StockDisposalItem) {
        _uiState.update {
            it.copy(
                selectedItem = item,
                caseQuantity = "",
                pieceQuantity = "",
                reason = null,
                note = "",
                submitSuccess = false,
                submitError = null
            )
        }
    }

    fun onBackToSearch() {
        _uiState.update {
            it.copy(
                selectedItem = null,
                caseQuantity = "",
                pieceQuantity = "",
                reason = null,
                note = "",
                submitSuccess = false,
                submitError = null
            )
        }
    }

    fun onCaseQuantityChange(value: String) {
        if (value.isSignedIntegerInput()) {
            _uiState.update { it.copy(caseQuantity = value) }
        }
    }

    fun onPieceQuantityChange(value: String) {
        if (value.isSignedIntegerInput()) {
            _uiState.update { it.copy(pieceQuantity = value) }
        }
    }

    fun onReasonSelected(reason: StockDisposalReason) {
        _uiState.update { it.copy(reason = reason) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onDismissSubmitResult() {
        _uiState.update { it.copy(submitSuccess = false, submitError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        val item = state.selectedItem ?: return
        val reason = state.reason ?: return
        val code = warehouseCode ?: return

        val cases = state.caseQuantity.toIntOrNull() ?: 0
        val pieces = state.pieceQuantity.toIntOrNull() ?: 0
        if (cases == 0 && pieces == 0) return

        val details = mutableListOf<StockDisposalDetail>()
        if (cases != 0) {
            details.add(
                StockDisposalDetail(
                    itemCode = item.itemCode,
                    stockAllocationCode = null,
                    quantity = cases,
                    quantityType = "CASE",
                    note = state.note.ifBlank { null }
                )
            )
        }
        if (pieces != 0) {
            details.add(
                StockDisposalDetail(
                    itemCode = item.itemCode,
                    stockAllocationCode = null,
                    quantity = pieces,
                    quantityType = "PIECE",
                    note = state.note.ifBlank { null }
                )
            )
        }

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val requestId = "android-disposal-${UUID.randomUUID()}"

        val request = StockDisposalCreateRequest(
            stockDisposals = listOf(
                StockDisposalEntry(
                    requestId = requestId,
                    processDate = today,
                    disposalDate = today,
                    warehouseCode = code,
                    reason = reason.value,
                    slipNumber = null,
                    note = state.note.ifBlank { null },
                    details = details
                )
            )
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            stockDisposalRepository.createDisposal(request)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitSuccess = true,
                            selectedItem = null,
                            caseQuantity = "",
                            pieceQuantity = "",
                            reason = null,
                            note = ""
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = e.message ?: "登録に失敗しました"
                        )
                    }
                }
        }
    }

    private fun String.isSignedIntegerInput(): Boolean {
        return isEmpty() || this == "-" || toIntOrNull() != null
    }
}
