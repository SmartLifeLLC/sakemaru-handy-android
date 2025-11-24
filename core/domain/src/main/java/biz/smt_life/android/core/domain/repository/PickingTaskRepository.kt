package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.PickingTask

/**
 * Repository interface for Picking Tasks.
 * Provides access to picking task data from the backend.
 */
interface PickingTaskRepository {
    /**
     * Fetch picking tasks for "My Area" tab (filtered by picker).
     *
     * @param warehouseId Required warehouse ID
     * @param pickerId Picker ID for filtering
     * @return Result containing list of picking tasks or error
     */
    suspend fun getMyAreaTasks(warehouseId: Int, pickerId: Int): Result<List<PickingTask>>

    /**
     * Fetch all picking tasks for "All Courses" tab (no picker filter).
     *
     * @param warehouseId Required warehouse ID
     * @return Result containing list of picking tasks or error
     */
    suspend fun getAllTasks(warehouseId: Int): Result<List<PickingTask>>
}
