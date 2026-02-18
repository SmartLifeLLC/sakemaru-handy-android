package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.IncomingScheduleProductResponse
import biz.smt_life.android.core.network.model.IncomingWorkItemResponse
import biz.smt_life.android.core.network.model.LocationResponse
import biz.smt_life.android.core.network.model.ScheduleDetailResponse
import biz.smt_life.android.core.network.model.StartWorkRequest
import biz.smt_life.android.core.network.model.UpdateWorkRequest
import biz.smt_life.android.core.network.model.WarehouseResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Incoming (入庫) API endpoints.
 * Security: Requires X-API-Key and Authorization Bearer token.
 */
interface IncomingApi {

    @GET("/api/master/warehouses")
    suspend fun getWarehouses(): ApiEnvelope<List<WarehouseResponse>>

    @GET("/api/incoming/schedules")
    suspend fun getSchedules(
        @Query("warehouse_id") warehouseId: Int,
        @Query("search") search: String? = null
    ): ApiEnvelope<List<IncomingScheduleProductResponse>>

    @GET("/api/incoming/schedules/{id}")
    suspend fun getScheduleDetail(
        @Path("id") id: Int
    ): ApiEnvelope<ScheduleDetailResponse>

    @GET("/api/incoming/work-items")
    suspend fun getWorkItems(
        @Query("warehouse_id") warehouseId: Int,
        @Query("picker_id") pickerId: Int? = null,
        @Query("status") status: String? = null,
        @Query("from_date") fromDate: String? = null,
        @Query("to_date") toDate: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiEnvelope<List<IncomingWorkItemResponse>>

    @POST("/api/incoming/work-items")
    suspend fun startWork(
        @Body request: StartWorkRequest
    ): ApiEnvelope<IncomingWorkItemResponse>

    @PUT("/api/incoming/work-items/{id}")
    suspend fun updateWork(
        @Path("id") id: Int,
        @Body request: UpdateWorkRequest
    ): ApiEnvelope<IncomingWorkItemResponse>

    @DELETE("/api/incoming/work-items/{id}")
    suspend fun cancelWork(
        @Path("id") id: Int
    ): ApiEnvelope<Unit?>

    @POST("/api/incoming/work-items/{id}/complete")
    suspend fun completeWork(
        @Path("id") id: Int
    ): ApiEnvelope<Unit?>

    @GET("/api/incoming/locations")
    suspend fun getLocations(
        @Query("warehouse_id") warehouseId: Int,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiEnvelope<List<LocationResponse>>
}
