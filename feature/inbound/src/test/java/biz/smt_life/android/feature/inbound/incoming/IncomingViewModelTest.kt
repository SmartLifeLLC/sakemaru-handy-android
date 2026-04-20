package biz.smt_life.android.feature.inbound.incoming

import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.IncomingWorkStatus
import biz.smt_life.android.core.domain.model.Location
import biz.smt_life.android.core.domain.model.UpdateWorkItemData
import biz.smt_life.android.core.domain.model.WorkItemSchedule
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.ui.TokenManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncomingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: IncomingRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var viewModel: IncomingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        tokenManager = mockk()

        every { tokenManager.getPickerId() } returns 7
        every { tokenManager.getPickerName() } returns "Picker"
        every { tokenManager.getDefaultWarehouseId() } returns 10

        viewModel = IncomingViewModel(repository, tokenManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `history item without schedule is rejected`() {
        val workItem = IncomingWorkItem(
            id = 1,
            incomingScheduleId = 100,
            pickerId = 7,
            warehouseId = 10,
            workQuantity = 3,
            workArrivalDate = "2026-04-20",
            status = IncomingWorkStatus.COMPLETED,
            schedule = null
        )

        val result = viewModel.selectHistoryItem(workItem)

        assertFalse(result)
        assertEquals("このスケジュールは編集できません", viewModel.state.value.errorMessage)
    }

    @Test
    fun `selectHistoryItem restores product context and target warehouse`() {
        val workItem = historyWorkItem(
            workWarehouseId = 10,
            targetWarehouseId = 20
        )

        val result = viewModel.selectHistoryItem(workItem)

        assertTrue(result)
        assertTrue(viewModel.state.value.isFromHistory)
        assertEquals(20, viewModel.state.value.selectedSchedule?.warehouseId)
        assertEquals("仮想倉庫", viewModel.state.value.selectedSchedule?.warehouseName)
        assertEquals(501, viewModel.state.value.selectedProduct?.itemId)
        assertEquals("商品A", viewModel.state.value.selectedProduct?.itemName)
        assertEquals(9001, viewModel.state.value.currentWorkItem?.id)
    }

    @Test
    fun `submitEntry from history updates and skips completion`() = runTest {
        val warehouse = IncomingWarehouse(id = 10, code = "W10", name = "作業倉庫")
        val refreshedProduct = IncomingProduct(
            itemId = 501,
            itemCode = "ITEM-501",
            itemName = "商品A",
            janCodes = listOf("490000000001"),
            totalExpectedQuantity = 20,
            totalReceivedQuantity = 7,
            totalRemainingQuantity = 13,
            schedules = listOf(
                IncomingSchedule(
                    id = 100,
                    warehouseId = 20,
                    warehouseName = "仮想倉庫",
                    expectedQuantity = 20,
                    receivedQuantity = 7,
                    remainingQuantity = 13,
                    status = IncomingScheduleStatus.PARTIAL
                )
            )
        )
        val updatedWorkItem = historyWorkItem(
            workWarehouseId = 10,
            targetWarehouseId = 20
        )

        coEvery { repository.getSchedules(10, null) } returns Result.success(listOf(refreshedProduct))
        coEvery { repository.getWorkingScheduleIds(10, 7) } returns Result.success(emptySet())
        coEvery {
            repository.updateWorkItem(
                9001,
                any()
            )
        } returns Result.success(updatedWorkItem)
        coEvery {
            repository.getWorkItems(
                warehouseId = 10,
                pickerId = 7,
                status = "all",
                fromDate = any(),
                toDate = any(),
                limit = 100
            )
        } returns Result.success(listOf(updatedWorkItem))

        viewModel.selectWarehouse(warehouse)
        advanceUntilIdle()
        assertTrue(viewModel.selectHistoryItem(updatedWorkItem))

        var navigated = false
        viewModel.submitEntry { navigated = true }
        advanceUntilIdle()

        val expectedArrivalDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        coVerify(exactly = 1) {
            repository.updateWorkItem(
                9001,
                match<UpdateWorkItemData> {
                    it.workQuantity == 5 &&
                        it.workArrivalDate == expectedArrivalDate &&
                        it.workExpirationDate == "2026-05-01" &&
                        it.locationId == 301
                }
            )
        }
        coVerify(exactly = 0) { repository.startWork(any()) }
        coVerify(exactly = 0) { repository.completeWorkItem(any()) }
        coVerify(exactly = 1) {
            repository.getWorkItems(
                warehouseId = 10,
                pickerId = 7,
                status = "all",
                fromDate = any(),
                toDate = any(),
                limit = 100
            )
        }
        assertTrue(navigated)
        assertEquals("商品A", viewModel.state.value.selectedProduct?.itemName)
        assertFalse(viewModel.state.value.isFromHistory)
        assertEquals("", viewModel.state.value.searchQuery)
    }

    @Test
    fun `initializeDefaultWarehouse selects configured warehouse and loads products`() = runTest {
        val warehouse = IncomingWarehouse(id = 10, code = "W10", name = "作業倉庫")
        coEvery { repository.getWarehouses() } returns Result.success(listOf(warehouse))
        coEvery { repository.getSchedules(10, null) } returns Result.success(emptyList())
        coEvery { repository.getWorkingScheduleIds(10, 7) } returns Result.success(emptySet())

        viewModel.initializeDefaultWarehouse()
        advanceUntilIdle()

        assertEquals(10, viewModel.state.value.selectedWarehouse?.id)
        assertEquals("作業倉庫", viewModel.state.value.selectedWarehouse?.name)
        coVerify(exactly = 1) { repository.getWarehouses() }
        coVerify(exactly = 1) { repository.getSchedules(10, null) }
    }

    private fun historyWorkItem(
        workWarehouseId: Int,
        targetWarehouseId: Int
    ) = IncomingWorkItem(
        id = 9001,
        incomingScheduleId = 100,
        pickerId = 7,
        warehouseId = workWarehouseId,
        locationId = 301,
        location = Location(id = 301, displayName = "A-01-01"),
        workQuantity = 5,
        workArrivalDate = "2026-04-20",
        workExpirationDate = "2026-05-01",
        status = IncomingWorkStatus.COMPLETED,
        schedule = WorkItemSchedule(
            id = 100,
            itemId = 501,
            itemCode = "ITEM-501",
            itemName = "商品A",
            janCodes = listOf("490000000001"),
            warehouseId = targetWarehouseId,
            warehouseName = "仮想倉庫",
            expectedQuantity = 20,
            receivedQuantity = 5,
            remainingQuantity = 15,
            status = IncomingScheduleStatus.PARTIAL
        )
    )
}
