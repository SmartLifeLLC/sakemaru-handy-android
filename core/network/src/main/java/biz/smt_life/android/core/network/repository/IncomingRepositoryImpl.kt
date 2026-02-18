package biz.smt_life.android.core.network.repository

import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWarehouseSummary
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.IncomingWorkStatus
import biz.smt_life.android.core.domain.model.Location
import biz.smt_life.android.core.domain.model.WorkItemSchedule
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.network.ErrorMapper
import biz.smt_life.android.core.network.api.IncomingApi
import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.IncomingScheduleProductResponse
import biz.smt_life.android.core.network.model.IncomingWorkItemResponse
import biz.smt_life.android.core.network.model.LocationResponse
import biz.smt_life.android.core.network.model.StartWorkRequest
import biz.smt_life.android.core.network.model.UpdateWorkRequest
import biz.smt_life.android.core.network.model.WarehouseResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingRepositoryImpl @Inject constructor(
    private val incomingApi: IncomingApi,
    private val errorMapper: ErrorMapper
) : IncomingRepository {

    override suspend fun getWarehouses(): Result<List<IncomingWarehouse>> {
        return try {
            val response = incomingApi.getWarehouses()
            if (response.isSuccess && response.result?.data != null) {
                val warehouses = response.result.data.map { it.toDomain() }
                Result.success(warehouses)
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "倉庫一覧の取得に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun getSchedules(warehouseId: Int, search: String?): Result<List<IncomingProduct>> {
        return try {
            val response = incomingApi.getSchedules(warehouseId, search)
            if (response.isSuccess && response.result?.data != null) {
                val products = response.result.data.map { it.toDomain() }
                Result.success(products)
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "入庫予定の取得に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun getWorkItems(
        warehouseId: Int,
        pickerId: Int?,
        status: String?,
        fromDate: String?
    ): Result<List<IncomingWorkItem>> {
        return try {
            val response = incomingApi.getWorkItems(
                warehouseId = warehouseId,
                pickerId = pickerId,
                status = status,
                fromDate = fromDate
            )
            if (response.isSuccess && response.result?.data != null) {
                val workItems = response.result.data.map { it.toDomain() }
                Result.success(workItems)
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "作業データの取得に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun startWork(
        scheduleId: Int,
        pickerId: Int,
        warehouseId: Int
    ): Result<IncomingWorkItem> {
        return try {
            val request = StartWorkRequest(
                incomingScheduleId = scheduleId,
                pickerId = pickerId,
                warehouseId = warehouseId
            )
            val response = incomingApi.startWork(request)
            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.toDomain())
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "作業の開始に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun updateWork(
        id: Int,
        quantity: Int,
        arrivalDate: String,
        expirationDate: String?,
        locationId: Int?
    ): Result<IncomingWorkItem> {
        return try {
            val request = UpdateWorkRequest(
                workQuantity = quantity,
                workArrivalDate = arrivalDate,
                workExpirationDate = expirationDate,
                locationId = locationId
            )
            val response = incomingApi.updateWork(id, request)
            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.toDomain())
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "作業データの更新に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun completeWork(id: Int): Result<Unit> {
        return try {
            val response = incomingApi.completeWork(id)
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "入庫確定に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun cancelWork(id: Int): Result<Unit> {
        return try {
            val response = incomingApi.cancelWork(id)
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "作業のキャンセルに失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    override suspend fun getLocations(warehouseId: Int, search: String?): Result<List<Location>> {
        return try {
            val response = incomingApi.getLocations(warehouseId, search)
            if (response.isSuccess && response.result?.data != null) {
                val locations = response.result.data.map { it.toDomain() }
                Result.success(locations)
            } else {
                Result.failure(Exception(extractErrorMessage(response.result, "ロケーションの取得に失敗しました")))
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }

    // ─── Mappers ───

    private fun WarehouseResponse.toDomain() = IncomingWarehouse(
        id = id,
        code = code,
        name = name,
        kanaName = kanaName,
        outOfStockOption = outOfStockOption
    )

    private fun IncomingScheduleProductResponse.toDomain() = IncomingProduct(
        itemId = itemId,
        itemCode = itemCode,
        itemName = itemName,
        searchCode = searchCode,
        janCodes = janCodes,
        volume = volume,
        temperatureType = temperatureType,
        images = images,
        totalExpectedQuantity = totalExpectedQuantity,
        totalReceivedQuantity = totalReceivedQuantity,
        totalRemainingQuantity = totalRemainingQuantity,
        warehouses = warehouses.map { wh ->
            IncomingWarehouseSummary(
                warehouseId = wh.warehouseId,
                warehouseCode = wh.warehouseCode,
                warehouseName = wh.warehouseName,
                expectedQuantity = wh.expectedQuantity,
                receivedQuantity = wh.receivedQuantity,
                remainingQuantity = wh.remainingQuantity
            )
        },
        schedules = schedules.map { sc ->
            IncomingSchedule(
                id = sc.id,
                warehouseId = sc.warehouseId,
                warehouseName = sc.warehouseName,
                expectedQuantity = sc.expectedQuantity,
                receivedQuantity = sc.receivedQuantity,
                remainingQuantity = sc.remainingQuantity,
                quantityType = sc.quantityType,
                expectedArrivalDate = sc.expectedArrivalDate,
                status = IncomingScheduleStatus.fromString(sc.status)
            )
        }
    )

    private fun IncomingWorkItemResponse.toDomain() = IncomingWorkItem(
        id = id,
        incomingScheduleId = incomingScheduleId,
        pickerId = pickerId,
        warehouseId = warehouseId,
        locationId = locationId,
        location = location?.toDomain(),
        workQuantity = workQuantity,
        workArrivalDate = workArrivalDate,
        workExpirationDate = workExpirationDate,
        status = IncomingWorkStatus.fromString(status),
        startedAt = startedAt,
        schedule = schedule?.let { sc ->
            WorkItemSchedule(
                id = sc.id,
                itemId = sc.itemId,
                itemCode = sc.itemCode,
                itemName = sc.itemName,
                warehouseId = sc.warehouseId,
                warehouseName = sc.warehouseName,
                expectedQuantity = sc.expectedQuantity,
                receivedQuantity = sc.receivedQuantity,
                remainingQuantity = sc.remainingQuantity,
                quantityType = sc.quantityType
            )
        }
    )

    private fun LocationResponse.toDomain() = Location(
        id = id,
        code1 = code1,
        code2 = code2,
        code3 = code3,
        name = name,
        displayName = displayName
    )

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
