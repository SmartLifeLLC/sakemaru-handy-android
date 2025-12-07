package biz.smt_life.android.feature.outbound.tasks

import biz.smt_life.android.core.domain.model.PickingTask

/**
 * UI state for Picking Tasks screen per spec 2.5.1 出庫処理.
 * Shows only "My tasks" (私の担当) - tasks assigned to current picker.
 */
data class PickingTasksState(
    val tasksState: TaskListState = TaskListState.Loading,
    val warehouseId: Int? = null,
    val pickerId: Int? = null,
    val errorMessage: String? = null,
    val selectedTask: PickingTask? = null // Task selected for navigation to picking screen
)

/**
 * State for the task list.
 */
sealed interface TaskListState {
    data object Loading : TaskListState
    data object Empty : TaskListState
    data class Success(val tasks: List<PickingTask>) : TaskListState
    data class Error(val message: String) : TaskListState
}
