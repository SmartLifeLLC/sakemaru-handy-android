package biz.smt_life.android.core.network.repository

import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingQuantityType
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWarehouseSummary
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.IncomingWorkStatus
import biz.smt_life.android.core.domain.model.Location
import biz.smt_life.android.core.domain.model.StartWorkData
import biz.smt_life.android.core.domain.model.UpdateWorkItemData
import biz.smt_life.android.core.domain.model.WorkItemSchedule
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.network.ErrorMapper
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.network.api.IncomingApi
import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.IncomingProductResponse
import biz.smt_life.android.core.network.model.IncomingScheduleResponse
import biz.smt_life.android.core.network.model.IncomingWarehouseSummaryResponse
import biz.smt_life.android.core.network.model.IncomingWorkItemResponse
import biz.smt_life.android.core.network.model.LocationResponse
import biz.smt_life.android.core.network.model.StartWorkRequest
import biz.smt_life.android.core.network.model.UpdateWorkItemRequest
import biz.smt_life.android.core.network.model.WarehouseResponse
import biz.smt_life.android.core.network.model.WorkItemScheduleResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingRepositoryImpl @Inject constructor(
    private val incomingApi: IncomingApi,
    private val errorMapper: ErrorMapper
) : IncomingRepository {

    private var warehousesCache: List<IncomingWarehouse>? = null
    private var warehousesCacheTime: Long = 0L

    private companion object {
        const val WAREHOUSES_CACHE_TTL_MS = 5 * 60 * 1000L
    }

    override suspend fun getWarehouses(): Result<List<IncomingWarehouse>> {
        val now = System.currentTimeMillis()
        warehousesCache?.takeIf { now - warehousesCacheTime < WAREHOUSES_CACHE_TTL_MS }?.let {
            return Result.success(it)
        }

        return try {
            val response = incomingApi.getWarehouses()
            if (response.isSuccess && response.result?.data != null) {
                val warehouses = response.result.data.map { it.toDomainModel() }
                warehousesCache = warehouses
                warehousesCacheTime = now
                Result.success(warehouses)
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "倉庫一覧の取得に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun getSchedules(
        warehouseId: Int,
        search: String?
    ): Result<List<IncomingProduct>> {
        return try {
            val response = incomingApi.getSchedules(warehouseId = warehouseId, search = search)
            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.map { it.toDomainModel() })
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "入荷予定の取得に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun getScheduleDetail(id: Int): Result<IncomingProduct> {
        return try {
            val response = incomingApi.getScheduleDetail(id)
            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.toDomainModel())
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "入荷予定詳細の取得に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun getWorkItems(
        warehouseId: Int,
        pickerId: Int?,
        status: String?,
        fromDate: String?,
        toDate: String?,
        limit: Int?
    ): Result<List<IncomingWorkItem>> {
        return try {
            val response = incomingApi.getWorkItems(
                warehouseId = warehouseId,
                pickerId = pickerId,
                status = status,
                fromDate = fromDate,
                toDate = toDate,
                limit = limit
            )
            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.map { it.toDomainModel() })
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "作業履歴の取得に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun getWorkingScheduleIds(
        warehouseId: Int,
        pickerId: Int
    ): Result<Set<Int>> {
        return try {
            val response = incomingApi.getWorkItems(
                warehouseId = warehouseId,
                pickerId = pickerId,
                status = "WORKING"
            )
            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.map { it.incomingScheduleId }.toSet())
            } else {
                Result.success(emptySet())
            }
        } catch (_: Exception) {
            Result.success(emptySet())
        }
    }

    override suspend fun startWork(data: StartWorkData): Result<IncomingWorkItem> {
        return try {
            val response = incomingApi.startWork(
                StartWorkRequest(
                    incomingScheduleId = data.incomingScheduleId,
                    pickerId = data.pickerId,
                    warehouseId = data.warehouseId
                )
            )

            if ((response.isSuccess || response.code == "ALREADY_WORKING") && response.result?.data != null) {
                Result.success(response.result.data.toDomainModel())
            } else {
                Result.failure(
                    NetworkException.ValidationError(
                        extractErrorMessage(response.result, "作業開始に失敗しました")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun updateWorkItem(id: Int, data: UpdateWorkItemData): Result<IncomingWorkItem> {
        return try {
            val response = incomingApi.updateWorkItem(
                id = id,
                request = UpdateWorkItemRequest(
                    workQuantity = data.workQuantity,
                    workArrivalDate = data.workArrivalDate,
                    workExpirationDate = data.workExpirationDate,
                    locationId = data.locationId
                )
            )

            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.toDomainModel())
            } else {
                Result.failure(
                    NetworkException.ValidationError(
                        extractErrorMessage(response.result, "作業データの更新に失敗しました")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun completeWorkItem(id: Int): Result<Unit> {
        return try {
            val response = incomingApi.completeWorkItem(id)
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(
                    NetworkException.ValidationError(
                        extractErrorMessage(response.result, "入荷確定に失敗しました")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun cancelWorkItem(id: Int): Result<Unit> {
        return try {
            val response = incomingApi.cancelWorkItem(id)
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(
                    NetworkException.ValidationError(
                        extractErrorMessage(response.result, "作業のキャンセルに失敗しました")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun searchLocations(
        warehouseId: Int,
        search: String?,
        limit: Int?
    ): Result<List<Location>> {
        return try {
            val response = incomingApi.searchLocations(
                warehouseId = warehouseId,
                search = search,
                limit = limit
            )
            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.map { it.toDomainModel() })
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "ロケーション検索に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    private fun extractErrorMessage(result: ApiEnvelope.ResultBlock<*>?, fallbackMessage: String): String {
        if (result == null) return fallbackMessage

        val detailedErrors = result.errors
            ?.values
            ?.flatten()
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n")

        return when {
            !result.errorMessage.isNullOrBlank() && !detailedErrors.isNullOrBlank() ->
                "${result.errorMessage}\n$detailedErrors"
            !result.errorMessage.isNullOrBlank() ->
                result.errorMessage
            !result.message.isNullOrBlank() && !detailedErrors.isNullOrBlank() ->
                "${result.message}\n$detailedErrors"
            !result.message.isNullOrBlank() ->
                result.message
            !detailedErrors.isNullOrBlank() ->
                detailedErrors
            else ->
                fallbackMessage
        }
    }

    private fun WarehouseResponse.toDomainModel() = IncomingWarehouse(
        id = id,
        code = code,
        name = name,
        kanaName = kanaName,
        outOfStockOption = outOfStockOption
    )

    private fun LocationResponse.toDomainModel() = Location(
        id = id,
        code1 = code1,
        code2 = code2,
        code3 = code3,
        name = name,
        displayName = displayName
    )

    private fun IncomingWarehouseSummaryResponse.toDomainModel() = IncomingWarehouseSummary(
        warehouseId = warehouseId,
        warehouseCode = warehouseCode,
        warehouseName = warehouseName,
        expectedQuantity = expectedQuantity,
        receivedQuantity = receivedQuantity,
        remainingQuantity = remainingQuantity
    )

    private fun IncomingScheduleResponse.toDomainModel() = IncomingSchedule(
        id = id,
        warehouseId = warehouseId,
        warehouseName = warehouseName,
        expectedQuantity = expectedQuantity,
        receivedQuantity = receivedQuantity,
        remainingQuantity = remainingQuantity,
        quantityType = IncomingQuantityType.fromString(quantityType),
        expectedArrivalDate = expectedArrivalDate,
        expirationDate = expirationDate,
        status = IncomingScheduleStatus.fromString(status),
        location = location?.toDomainModel()
    )

    private fun IncomingProductResponse.toDomainModel() = IncomingProduct(
        itemId = itemId,
        itemCode = itemCode,
        itemName = itemName,
        searchCode = searchCode,
        janCodes = janCodes,
        volume = volume,
        volumeUnit = volumeUnit,
        capacityCase = capacityCase,
        temperatureType = temperatureType,
        images = images,
        defaultLocation = defaultLocation?.toDomainModel(),
        totalExpectedQuantity = totalExpectedQuantity,
        totalReceivedQuantity = totalReceivedQuantity,
        totalRemainingQuantity = totalRemainingQuantity,
        warehouses = warehouses.map { it.toDomainModel() },
        schedules = schedules.map { it.toDomainModel() }
    )

    private fun WorkItemScheduleResponse.toDomainModel() = WorkItemSchedule(
        id = id,
        itemId = itemId,
        itemCode = itemCode,
        itemName = itemName,
        janCodes = janCodes,
        warehouseId = warehouseId,
        warehouseName = warehouseName,
        expectedQuantity = expectedQuantity,
        receivedQuantity = receivedQuantity,
        remainingQuantity = remainingQuantity,
        quantityType = IncomingQuantityType.fromString(quantityType),
        expectedArrivalDate = expectedArrivalDate,
        status = IncomingScheduleStatus.fromString(status)
    )

    private fun IncomingWorkItemResponse.toDomainModel() = IncomingWorkItem(
        id = id,
        incomingScheduleId = incomingScheduleId,
        pickerId = pickerId,
        warehouseId = warehouseId,
        locationId = locationId,
        location = location?.toDomainModel(),
        workQuantity = workQuantity,
        workArrivalDate = workArrivalDate,
        workExpirationDate = workExpirationDate,
        status = IncomingWorkStatus.fromString(status),
        startedAt = startedAt,
        schedule = schedule?.toDomainModel()
    )
}
