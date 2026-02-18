package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.Location

/**
 * Repository interface for Incoming (入庫) feature.
 * Provides access to incoming schedules, work items, and locations.
 */
interface IncomingRepository {

    /**
     * Fetch warehouse master list.
     */
    suspend fun getWarehouses(): Result<List<IncomingWarehouse>>

    /**
     * Fetch incoming schedules (product list with schedules).
     * @param warehouseId Required warehouse ID
     * @param search Optional search keyword (item code, JAN code, item name)
     */
    suspend fun getSchedules(warehouseId: Int, search: String? = null): Result<List<IncomingProduct>>

    /**
     * Fetch work items (history).
     * @param warehouseId Required warehouse ID
     * @param pickerId Optional picker ID filter
     * @param status Optional status filter (WORKING, COMPLETED, CANCELLED, all)
     * @param fromDate Optional start date filter (YYYY-MM-DD)
     */
    suspend fun getWorkItems(
        warehouseId: Int,
        pickerId: Int? = null,
        status: String? = null,
        fromDate: String? = null
    ): Result<List<IncomingWorkItem>>

    /**
     * Start a new work item (入荷作業開始).
     */
    suspend fun startWork(
        scheduleId: Int,
        pickerId: Int,
        warehouseId: Int
    ): Result<IncomingWorkItem>

    /**
     * Update a work item (数量・日付・ロケーション更新).
     */
    suspend fun updateWork(
        id: Int,
        quantity: Int,
        arrivalDate: String,
        expirationDate: String? = null,
        locationId: Int? = null
    ): Result<IncomingWorkItem>

    /**
     * Complete a work item (入庫確定).
     */
    suspend fun completeWork(id: Int): Result<Unit>

    /**
     * Cancel a work item (作業キャンセル).
     */
    suspend fun cancelWork(id: Int): Result<Unit>

    /**
     * Search locations for a warehouse.
     * @param warehouseId Required warehouse ID
     * @param search Optional search keyword
     */
    suspend fun getLocations(warehouseId: Int, search: String? = null): Result<List<Location>>
}
