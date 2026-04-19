package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.IncomingProductResponse
import biz.smt_life.android.core.network.model.IncomingWorkItemResponse
import biz.smt_life.android.core.network.model.LocationResponse
import biz.smt_life.android.core.network.model.StartWorkRequest
import biz.smt_life.android.core.network.model.UpdateWorkItemRequest
import biz.smt_life.android.core.network.model.WarehouseResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface IncomingApi {
    @GET("/api/master/warehouses")
    suspend fun getWarehouses(): ApiEnvelope<List<WarehouseResponse>>

    @GET("/api/incoming/schedules")
    suspend fun getSchedules(
        @Query("warehouse_id") warehouseId: Int,
        @Query("search") search: String? = null
    ): ApiEnvelope<List<IncomingProductResponse>>

    @GET("/api/incoming/schedules/{id}")
    suspend fun getScheduleDetail(
        @Path("id") id: Int
    ): ApiEnvelope<IncomingProductResponse>

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
    suspend fun updateWorkItem(
        @Path("id") id: Int,
        @Body request: UpdateWorkItemRequest
    ): ApiEnvelope<IncomingWorkItemResponse>

    @POST("/api/incoming/work-items/{id}/complete")
    suspend fun completeWorkItem(
        @Path("id") id: Int
    ): ApiEnvelope<Unit?>

    @DELETE("/api/incoming/work-items/{id}")
    suspend fun cancelWorkItem(
        @Path("id") id: Int
    ): ApiEnvelope<Unit?>

    @GET("/api/incoming/locations")
    suspend fun searchLocations(
        @Query("warehouse_id") warehouseId: Int,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiEnvelope<List<LocationResponse>>
}
