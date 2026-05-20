package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.StockDisposalCreateResponse
import biz.smt_life.android.core.network.model.StockDisposalSearchResponse
import biz.smt_life.android.core.network.model.StockDisposalRequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface StockDisposalApi {

    @GET("/api/stock-disposals/items/search")
    suspend fun searchItems(
        @Query("keyword") keyword: String,
        @Query("warehouse_code") warehouseCode: String,
        @Query("stock_allocation_code") stockAllocationCode: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiEnvelope<StockDisposalSearchResponse>

    @POST("/api/stock-disposals")
    suspend fun createDisposal(
        @Body request: StockDisposalRequestBody
    ): ApiEnvelope<StockDisposalCreateResponse>
}
