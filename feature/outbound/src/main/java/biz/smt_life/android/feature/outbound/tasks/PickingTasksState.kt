package biz.smt_life.android.feature.outbound.tasks

import biz.smt_life.android.core.domain.model.PickingTask

/**
 * UI state for Picking Tasks screen per spec 2.5.1 出荷処理.
 * Shows only "My tasks" (私の担当) - tasks assigned to current picker.
 *
 * Note: warehouseId and pickerId are managed by TokenManager (session data),
 * not stored in UI state to avoid duplication and sync issues.
 */
data class PickingTasksState(
    val tasksState: TaskListState = TaskListState.Loading,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedTask: PickingTask? = null, // Task selected for navigation to picking screen
    val warehouseName: String = "",
    val selectedTab: TaskTab = TaskTab.PENDING
)

enum class TaskTab { PENDING, ACTIVE, COMPLETED, SUPPORT }

/**
 * State for the task list.
 */
sealed interface TaskListState {
    data object Loading : TaskListState
    data object Empty : TaskListState
    data class Success(
        val pendingTasks: List<PickingTask> = emptyList(),
        val activeTasks: List<PickingTask> = emptyList(),
        val completedTasks: List<PickingTask> = emptyList()
    ) : TaskListState
    data class Error(val message: String) : TaskListState
}
