package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.StockDisposalItem
import biz.smt_life.android.core.domain.model.StockDisposalQueueResult

interface StockDisposalRepository {

    suspend fun searchItems(
        keyword: String,
        warehouseCode: String,
        stockAllocationCode: String? = null,
        limit: Int? = null
    ): Result<List<StockDisposalItem>>

    suspend fun createDisposal(
        request: StockDisposalCreateRequest
    ): Result<List<StockDisposalQueueResult>>
}

data class StockDisposalCreateRequest(
    val stockDisposals: List<StockDisposalEntry>
)

data class StockDisposalEntry(
    val requestId: String,
    val processDate: String,
    val disposalDate: String,
    val warehouseCode: String,
    val reason: String,
    val slipNumber: String?,
    val note: String?,
    val details: List<StockDisposalDetail>
)

data class StockDisposalDetail(
    val itemCode: String,
    val stockAllocationCode: String?,
    val quantity: Int,
    val quantityType: String,
    val note: String?
)
