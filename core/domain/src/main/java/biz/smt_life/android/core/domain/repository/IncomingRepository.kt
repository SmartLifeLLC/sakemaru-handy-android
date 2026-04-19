package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.Location
import biz.smt_life.android.core.domain.model.StartWorkData
import biz.smt_life.android.core.domain.model.UpdateWorkItemData

interface IncomingRepository {
    suspend fun getWarehouses(): Result<List<IncomingWarehouse>>

    suspend fun getSchedules(
        warehouseId: Int,
        search: String? = null
    ): Result<List<IncomingProduct>>

    suspend fun getScheduleDetail(id: Int): Result<IncomingProduct>

    suspend fun getWorkItems(
        warehouseId: Int,
        pickerId: Int? = null,
        status: String? = null,
        fromDate: String? = null,
        toDate: String? = null,
        limit: Int? = null
    ): Result<List<IncomingWorkItem>>

    suspend fun getWorkingScheduleIds(
        warehouseId: Int,
        pickerId: Int
    ): Result<Set<Int>>

    suspend fun startWork(data: StartWorkData): Result<IncomingWorkItem>

    suspend fun updateWorkItem(id: Int, data: UpdateWorkItemData): Result<IncomingWorkItem>

    suspend fun completeWorkItem(id: Int): Result<Unit>

    suspend fun cancelWorkItem(id: Int): Result<Unit>

    suspend fun searchLocations(
        warehouseId: Int,
        search: String? = null,
        limit: Int? = null
    ): Result<List<Location>>
}
