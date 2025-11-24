package biz.smt_life.android.core.network.repository

import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType
import biz.smt_life.android.core.domain.repository.PickingTaskRepository
import biz.smt_life.android.core.network.ErrorMapper
import biz.smt_life.android.core.network.api.PickingApi
import biz.smt_life.android.core.network.model.PickingTaskResponse
import javax.inject.Inject

/**
 * Implementation of PickingTaskRepository.
 * Maps API responses to domain models and handles errors.
 */
class PickingTaskRepositoryImpl @Inject constructor(
    private val pickingApi: PickingApi,
    private val errorMapper: ErrorMapper
) : PickingTaskRepository {

    override suspend fun getMyAreaTasks(warehouseId: Int, pickerId: Int): Result<List<PickingTask>> {
        return try {
            val response = pickingApi.getPickingTasks(
                warehouseId = warehouseId,
                pickerId = pickerId
            )

            if (response.isSuccess && response.result?.data != null) {
                val tasks = response.result.data.map { it.toDomainModel() }
                Result.success(tasks)
            } else {
                val errorMessage = response.result?.errorMessage ?: "Failed to fetch tasks"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun getAllTasks(warehouseId: Int): Result<List<PickingTask>> {
        return try {
            val response = pickingApi.getPickingTasks(
                warehouseId = warehouseId,
                pickerId = null // No picker filter for "All Courses"
            )

            if (response.isSuccess && response.result?.data != null) {
                val tasks = response.result.data.map { it.toDomainModel() }
                Result.success(tasks)
            } else {
                val errorMessage = response.result?.errorMessage ?: "Failed to fetch tasks"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    /**
     * Maps API response to domain model.
     */
    private fun PickingTaskResponse.toDomainModel(): PickingTask {
        val items = pickingList.map { item ->
            PickingTaskItem(
                id = item.wmsPickingItemResultId,
                itemId = item.itemId,
                itemName = item.itemName,
                plannedQtyType = QuantityType.fromString(item.plannedQtyType),
                plannedQty = item.plannedQty.toDoubleOrNull() ?: 0.0,
                pickedQty = item.pickedQty.toDoubleOrNull() ?: 0.0,
                slipNumber = item.slipNumber
            )
        }

        val totalItems = items.size
        val completedItems = items.count { it.isCompleted }

        return PickingTask(
            taskId = wave.wmsPickingTaskId,
            waveId = wave.wmsWaveId,
            courseName = course.name,
            courseCode = course.code,
            pickingAreaName = pickingArea.name,
            pickingAreaCode = pickingArea.code,
            items = items,
            totalItems = totalItems,
            completedItems = completedItems,
            progressText = "$completedItems/$totalItems"
        )
    }
}
