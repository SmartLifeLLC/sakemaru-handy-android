package biz.smt_life.android.feature.stockdisposal

import biz.smt_life.android.core.domain.model.StockDisposalItem
import biz.smt_life.android.core.domain.model.StockDisposalReason

data class StockDisposalUiState(
    val warehouseName: String = "",
    val warehouseCode: String = "",
    val searchKeyword: String = "",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val items: List<StockDisposalItem> = emptyList(),
    val selectedItem: StockDisposalItem? = null,
    val error: String? = null,
    val caseQuantity: String = "",
    val pieceQuantity: String = "",
    val reason: StockDisposalReason? = null,
    val note: String = "",
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitError: String? = null
) {
    val totalPieces: Int
        get() {
            val cases = caseQuantity.toIntOrNull() ?: 0
            val pieces = pieceQuantity.toIntOrNull() ?: 0
            val capacityCase = selectedItem?.capacityCase ?: 0
            return cases * capacityCase + pieces
        }

    val canSubmit: Boolean
        get() {
            val cases = caseQuantity.toIntOrNull() ?: 0
            val pieces = pieceQuantity.toIntOrNull() ?: 0
            return (cases != 0 || pieces != 0) && reason != null && !isSubmitting
        }
}
