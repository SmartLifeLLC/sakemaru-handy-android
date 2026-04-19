package biz.smt_life.android.core.network.repository

import biz.smt_life.android.core.domain.model.ProxyShipmentAllocation
import biz.smt_life.android.core.domain.model.ProxyShipmentCompletionResult
import biz.smt_life.android.core.domain.model.ProxyShipmentDetail
import biz.smt_life.android.core.domain.model.ProxyShipmentListPayload
import biz.smt_life.android.core.domain.repository.ProxyShipmentRepository
import biz.smt_life.android.core.network.ErrorMapper
import biz.smt_life.android.core.network.IdempotencyKeyGenerator
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.network.api.ProxyShipmentApi
import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.ProxyShipmentCompleteRequest
import biz.smt_life.android.core.network.model.ProxyShipmentUpdateRequest
import biz.smt_life.android.core.network.model.ProxyShipmentWarehouseRequest
import biz.smt_life.android.core.network.model.toCompletionDomain
import biz.smt_life.android.core.network.model.toDetailDomain
import biz.smt_life.android.core.network.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyShipmentRepositoryImpl @Inject constructor(
    private val proxyShipmentApi: ProxyShipmentApi,
    private val errorMapper: ErrorMapper
) : ProxyShipmentRepository {

    private val _allocationsFlow = MutableStateFlow<List<ProxyShipmentAllocation>>(emptyList())
    override val allocationsFlow: StateFlow<List<ProxyShipmentAllocation>> = _allocationsFlow.asStateFlow()

    private val _completionResults =
        MutableStateFlow<Map<Int, ProxyShipmentCompletionResult>>(emptyMap())

    override fun allocationFlow(allocationId: Int): Flow<ProxyShipmentAllocation?> {
        return allocationsFlow.map { allocations ->
            allocations.firstOrNull { it.allocationId == allocationId }
        }
    }

    override fun completionFlow(allocationId: Int): Flow<ProxyShipmentCompletionResult?> {
        return _completionResults.map { results -> results[allocationId] }
    }

    override suspend fun getAllocations(
        warehouseId: Int,
        shipmentDate: String?,
        deliveryCourseId: Int?
    ): Result<ProxyShipmentListPayload> {
        return try {
            val response = proxyShipmentApi.getProxyShipments(
                warehouseId = warehouseId,
                shipmentDate = shipmentDate,
                deliveryCourseId = deliveryCourseId
            )

            if (response.isSuccess && response.result?.data != null) {
                val payload = response.result.data.toDomain()
                _allocationsFlow.value = payload.items
                Result.success(payload)
            } else {
                Result.failure(
                    NetworkException.ValidationError(
                        extractErrorMessage(response.result, "横持ち出荷一覧の取得に失敗しました")
                    )
                )
            }
        } catch (throwable: Throwable) {
            Result.failure(errorMapper.mapException(throwable))
        }
    }

    override suspend fun getAllocationDetail(
        allocationId: Int,
        warehouseId: Int
    ): Result<ProxyShipmentDetail> {
        return try {
            val response = proxyShipmentApi.getProxyShipmentDetail(
                allocationId = allocationId,
                warehouseId = warehouseId
            )

            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.toDetailDomain())
            } else {
                Result.failure(
                    NetworkException.ValidationError(
                        extractErrorMessage(response.result, "横持ち出荷詳細の取得に失敗しました")
                    )
                )
            }
        } catch (throwable: Throwable) {
            Result.failure(errorMapper.mapException(throwable))
        }
    }

    override suspend fun startAllocation(
        allocationId: Int,
        warehouseId: Int
    ): Result<ProxyShipmentAllocation> {
        return try {
            val response = proxyShipmentApi.startProxyShipment(
                allocationId = allocationId,
                idempotencyKey = IdempotencyKeyGenerator.generate(),
                request = ProxyShipmentWarehouseRequest(warehouseId)
            )

            if (response.isSuccess && response.result?.data != null) {
                val allocation = response.result.data.toDomain()
                upsertAllocation(allocation)
                Result.success(allocation)
            } else {
                Result.failure(
                    NetworkException.ValidationError(
                        extractErrorMessage(response.result, "横持ち出荷の開始に失敗しました")
                    )
                )
            }
        } catch (throwable: Throwable) {
            Result.failure(errorMapper.mapException(throwable))
        }
    }

    override suspend fun updateAllocation(
        allocationId: Int,
        warehouseId: Int,
        pickedQty: Int
    ): Result<ProxyShipmentAllocation> {
        return try {
            val response = proxyShipmentApi.updateProxyShipment(
                allocationId = allocationId,
                idempotencyKey = IdempotencyKeyGenerator.generate(),
                request = ProxyShipmentUpdateRequest(
                    warehouseId = warehouseId,
                    pickedQty = pickedQty
                )
            )

            if (response.isSuccess && response.result?.data != null) {
                val allocation = response.result.data.toDomain()
                upsertAllocation(allocation)
                Result.success(allocation)
            } else {
                Result.failure(
                    NetworkException.ValidationError(
                        extractErrorMessage(response.result, "ピック数の更新に失敗しました")
                    )
                )
            }
        } catch (throwable: Throwable) {
            Result.failure(errorMapper.mapException(throwable))
        }
    }

    override suspend fun completeAllocation(
        allocationId: Int,
        warehouseId: Int,
        pickedQty: Int?
    ): Result<ProxyShipmentCompletionResult> {
        return try {
            val response = proxyShipmentApi.completeProxyShipment(
                allocationId = allocationId,
                idempotencyKey = IdempotencyKeyGenerator.generate(),
                request = ProxyShipmentCompleteRequest(
                    warehouseId = warehouseId,
                    pickedQty = pickedQty
                )
            )

            if (response.isSuccess && response.result?.data != null) {
                val completion = response.result.data.toCompletionDomain()
                _completionResults.update { current ->
                    current + (completion.allocation.allocationId to completion)
                }
                _allocationsFlow.update { current ->
                    current.filterNot { it.allocationId == allocationId }
                }
                Result.success(completion)
            } else {
                Result.failure(
                    NetworkException.ValidationError(
                        extractErrorMessage(response.result, "横持ち出荷の完了に失敗しました")
                    )
                )
            }
        } catch (throwable: Throwable) {
            Result.failure(errorMapper.mapException(throwable))
        }
    }

    private fun upsertAllocation(allocation: ProxyShipmentAllocation) {
        _allocationsFlow.update { current ->
            val replaced = current.map { existing ->
                if (existing.allocationId == allocation.allocationId) allocation else existing
            }
            if (replaced.any { it.allocationId == allocation.allocationId }) {
                replaced
            } else {
                listOf(allocation) + current
            }
        }
    }

    private fun <T> extractErrorMessage(
        result: ApiEnvelope.ResultBlock<T>?,
        fallbackMessage: String
    ): String {
        val primaryMessage = result?.errorMessage
        val detailedErrors = result?.errors
            ?.values
            ?.flatten()
            ?.joinToString(separator = "\n")

        return when {
            !primaryMessage.isNullOrBlank() && !detailedErrors.isNullOrBlank() ->
                "$primaryMessage\n$detailedErrors"
            !primaryMessage.isNullOrBlank() ->
                primaryMessage
            !detailedErrors.isNullOrBlank() ->
                detailedErrors
            else ->
                fallbackMessage
        }
    }
}
