package biz.smt_life.android.core.network.repository

import biz.smt_life.android.core.domain.model.StockDisposalItem
import biz.smt_life.android.core.domain.model.StockDisposalQueueResult
import biz.smt_life.android.core.domain.model.StockSummary
import biz.smt_life.android.core.domain.repository.StockDisposalCreateRequest
import biz.smt_life.android.core.domain.repository.StockDisposalRepository
import biz.smt_life.android.core.network.ErrorMapper
import biz.smt_life.android.core.network.api.StockDisposalApi
import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.StockDisposalDetailDto
import biz.smt_life.android.core.network.model.StockDisposalEntryDto
import biz.smt_life.android.core.network.model.StockDisposalItemDto
import biz.smt_life.android.core.network.model.StockDisposalQueueDto
import biz.smt_life.android.core.network.model.StockDisposalRequestBody
import biz.smt_life.android.core.network.model.StockSummaryDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockDisposalRepositoryImpl @Inject constructor(
    private val api: StockDisposalApi,
    private val errorMapper: ErrorMapper
) : StockDisposalRepository {

    override suspend fun searchItems(
        keyword: String,
        warehouseCode: String,
        stockAllocationCode: String?,
        limit: Int?
    ): Result<List<StockDisposalItem>> {
        return try {
            val response = api.searchItems(
                keyword = keyword,
                warehouseCode = warehouseCode,
                stockAllocationCode = stockAllocationCode,
                limit = limit
            )
            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.items.map { it.toDomainModel() })
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "商品検索に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun createDisposal(
        request: StockDisposalCreateRequest
    ): Result<List<StockDisposalQueueResult>> {
        return try {
            val body = StockDisposalRequestBody(
                stockDisposals = request.stockDisposals.map { entry ->
                    StockDisposalEntryDto(
                        requestId = entry.requestId,
                        processDate = entry.processDate,
                        disposalDate = entry.disposalDate,
                        warehouseCode = entry.warehouseCode,
                        reason = entry.reason,
                        slipNumber = entry.slipNumber,
                        note = entry.note,
                        details = entry.details.map { detail ->
                            StockDisposalDetailDto(
                                itemCode = detail.itemCode,
                                stockAllocationCode = detail.stockAllocationCode,
                                quantity = detail.quantity,
                                quantityType = detail.quantityType,
                                note = detail.note
                            )
                        }
                    )
                }
            )
            val response = api.createDisposal(body)
            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.stockDisposalQueues.map { it.toDomainModel() })
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "在庫調節登録に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    private fun <T> extractErrorMessage(
        result: ApiEnvelope.ResultBlock<T>?,
        fallback: String
    ): String {
        if (result == null) return fallback
        val primary = result.errorMessage ?: result.message
        val detailed = result.errors?.values?.flatten()?.joinToString("\n")
        return when {
            !primary.isNullOrBlank() && !detailed.isNullOrBlank() -> "$primary\n$detailed"
            !primary.isNullOrBlank() -> primary
            !detailed.isNullOrBlank() -> detailed
            else -> fallback
        }
    }

    private fun StockDisposalItemDto.toDomainModel() = StockDisposalItem(
        itemId = itemId,
        itemCode = itemCode,
        itemName = itemName,
        displayName = displayName,
        packaging = packaging,
        displayPackaging = displayPackaging,
        nameWithPackaging = nameWithPackaging,
        orderJanCode = orderJanCode,
        capacityCase = capacityCase,
        capacityCarton = capacityCarton,
        matchedField = matchedField,
        matchedValue = matchedValue,
        stock = stock.toDomainModel()
    )

    private fun StockSummaryDto.toDomainModel() = StockSummary(
        actualQuantity = actualQuantity,
        theoreticalQuantity = theoreticalQuantity
    )

    private fun StockDisposalQueueDto.toDomainModel() = StockDisposalQueueResult(
        requestId = requestId,
        queueId = queueId,
        status = status,
        duplicated = duplicated
    )
}
