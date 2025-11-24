package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.PickingTaskResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Picking Tasks API per handy_swagger_20251109.json.
 *
 * Endpoint: GET /api/picking/tasks
 * Security: Requires X-API-Key and Authorization Bearer token
 *
 * Query parameters:
 * - warehouse_id (required): Filter tasks by warehouse
 * - picker_id (optional): Filter tasks by specific picker (for "My Area" tab)
 * - picking_area_id (optional): Filter tasks by specific area
 */
interface PickingApi {
    @GET("/api/picking/tasks")
    suspend fun getPickingTasks(
        @Query("warehouse_id") warehouseId: Int,
        @Query("picker_id") pickerId: Int? = null,
        @Query("picking_area_id") pickingAreaId: Int? = null
    ): ApiEnvelope<List<PickingTaskResponse>>
}
