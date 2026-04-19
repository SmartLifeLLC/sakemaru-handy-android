package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.ProxyShipmentAllocation
import biz.smt_life.android.core.domain.model.ProxyShipmentCompletionResult
import biz.smt_life.android.core.domain.model.ProxyShipmentDetail
import biz.smt_life.android.core.domain.model.ProxyShipmentListPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ProxyShipmentRepository {
    val allocationsFlow: StateFlow<List<ProxyShipmentAllocation>>

    fun allocationFlow(allocationId: Int): Flow<ProxyShipmentAllocation?>

    fun completionFlow(allocationId: Int): Flow<ProxyShipmentCompletionResult?>

    suspend fun getAllocations(
        warehouseId: Int,
        shipmentDate: String? = null,
        deliveryCourseId: Int? = null
    ): Result<ProxyShipmentListPayload>

    suspend fun getAllocationDetail(
        allocationId: Int,
        warehouseId: Int
    ): Result<ProxyShipmentDetail>

    suspend fun startAllocation(
        allocationId: Int,
        warehouseId: Int
    ): Result<ProxyShipmentAllocation>

    suspend fun updateAllocation(
        allocationId: Int,
        warehouseId: Int,
        pickedQty: Int
    ): Result<ProxyShipmentAllocation>

    suspend fun completeAllocation(
        allocationId: Int,
        warehouseId: Int,
        pickedQty: Int? = null
    ): Result<ProxyShipmentCompletionResult>
}
