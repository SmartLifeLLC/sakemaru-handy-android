package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.ProxyShipmentAllocationDto
import biz.smt_life.android.core.network.model.ProxyShipmentCompleteRequest
import biz.smt_life.android.core.network.model.ProxyShipmentListResponse
import biz.smt_life.android.core.network.model.ProxyShipmentUpdateRequest
import biz.smt_life.android.core.network.model.ProxyShipmentWarehouseRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProxyShipmentApi {
    @GET("/api/proxy-shipments")
    suspend fun getProxyShipments(
        @Query("warehouse_id") warehouseId: Int,
        @Query("shipment_date") shipmentDate: String? = null,
        @Query("delivery_course_id") deliveryCourseId: Int? = null
    ): ApiEnvelope<ProxyShipmentListResponse>

    @GET("/api/proxy-shipments/{id}")
    suspend fun getProxyShipmentDetail(
        @Path("id") allocationId: Int,
        @Query("warehouse_id") warehouseId: Int
    ): ApiEnvelope<ProxyShipmentAllocationDto>

    @POST("/api/proxy-shipments/{id}/start")
    suspend fun startProxyShipment(
        @Path("id") allocationId: Int,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: ProxyShipmentWarehouseRequest
    ): ApiEnvelope<ProxyShipmentAllocationDto>

    @POST("/api/proxy-shipments/{id}/update")
    suspend fun updateProxyShipment(
        @Path("id") allocationId: Int,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: ProxyShipmentUpdateRequest
    ): ApiEnvelope<ProxyShipmentAllocationDto>

    @POST("/api/proxy-shipments/{id}/complete")
    suspend fun completeProxyShipment(
        @Path("id") allocationId: Int,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: ProxyShipmentCompleteRequest
    ): ApiEnvelope<ProxyShipmentAllocationDto>
}
