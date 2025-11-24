package biz.smt_life.android.feature.outbound.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.repository.PickingTaskRepository
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Picking Tasks screen per spec 2.5.1 出庫処理.
 * Manages two tabs: My Area (picker-filtered) and All Courses (warehouse-wide).
 *
 * Responsibilities:
 * - Load tasks per tab from repository
 * - Handle tab switching with lazy loading
 * - Pull-to-refresh for each tab
 * - Cache latest successful data in memory
 * - Handle 401/403 by exposing session error
 */
@HiltViewModel
class PickingTasksViewModel @Inject constructor(
    private val repository: PickingTaskRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(PickingTasksState())
    val state: StateFlow<PickingTasksState> = _state.asStateFlow()

    init {
        initializeSessionData()
    }

    /**
     * Initialize warehouse ID and picker ID from session.
     * If missing, set error state.
     */
    private fun initializeSessionData() {
        val warehouseId = tokenManager.getDefaultWarehouseId()
        val pickerId = tokenManager.getPickerId()

        if (warehouseId <= 0) {
            _state.update {
                it.copy(
                    errorMessage = "倉庫情報が見つかりません。再ログインしてください。",
                    myAreaState = TaskListState.Error("倉庫情報が見つかりません"),
                    allCoursesState = TaskListState.Error("倉庫情報が見つかりません")
                )
            }
            return
        }

        _state.update { it.copy(warehouseId = warehouseId, pickerId = pickerId) }

        // Load My Area tasks by default
        loadMyAreaTasks()
    }

    /**
     * Switch between tabs.
     * Loads data for the newly active tab if not already loaded.
     */
    fun onTabSelected(tab: PickingTab) {
        _state.update { it.copy(activeTab = tab) }

        // Lazy load: fetch data for tab if not already loaded
        when (tab) {
            PickingTab.MY_AREA -> {
                if (_state.value.myAreaState is TaskListState.Loading) {
                    loadMyAreaTasks()
                }
            }
            PickingTab.ALL_COURSES -> {
                if (_state.value.allCoursesState is TaskListState.Loading) {
                    loadAllCoursesTasks()
                }
            }
        }
    }

    /**
     * Refresh tasks for the currently active tab.
     */
    fun refresh() {
        when (_state.value.activeTab) {
            PickingTab.MY_AREA -> loadMyAreaTasks()
            PickingTab.ALL_COURSES -> loadAllCoursesTasks()
        }
    }

    /**
     * Load tasks for "My Area" tab (filtered by picker).
     */
    fun loadMyAreaTasks() {
        val warehouseId = _state.value.warehouseId ?: return
        val pickerId = _state.value.pickerId

        if (pickerId == null || pickerId <= 0) {
            _state.update {
                it.copy(myAreaState = TaskListState.Error("ピッカー情報が見つかりません"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(myAreaState = TaskListState.Loading) }

            repository.getMyAreaTasks(warehouseId, pickerId)
                .onSuccess { tasks ->
                    val newState = if (tasks.isEmpty()) {
                        TaskListState.Empty
                    } else {
                        TaskListState.Success(tasks)
                    }
                    _state.update { it.copy(myAreaState = newState) }
                }
                .onFailure { error ->
                    val message = mapErrorMessage(error)
                    _state.update { it.copy(myAreaState = TaskListState.Error(message)) }
                }
        }
    }

    /**
     * Load tasks for "All Courses" tab (no picker filter).
     */
    fun loadAllCoursesTasks() {
        val warehouseId = _state.value.warehouseId ?: return

        viewModelScope.launch {
            _state.update { it.copy(allCoursesState = TaskListState.Loading) }

            repository.getAllTasks(warehouseId)
                .onSuccess { tasks ->
                    val newState = if (tasks.isEmpty()) {
                        TaskListState.Empty
                    } else {
                        TaskListState.Success(tasks)
                    }
                    _state.update { it.copy(allCoursesState = newState) }
                }
                .onFailure { error ->
                    val message = mapErrorMessage(error)
                    _state.update { it.copy(allCoursesState = TaskListState.Error(message)) }
                }
        }
    }

    /**
     * Navigate to task detail.
     * Pass along task ID, warehouse ID, and other navigation params.
     */
    fun onTaskClicked(taskId: Int) {
        // Navigation event will be handled by the screen
        // Exposing event via SharedFlow if needed
    }

    /**
     * Map exception to user-friendly Japanese error message.
     */
    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is NetworkException.Unauthorized -> "認証エラー。再ログインしてください。"
            is NetworkException.Forbidden -> "アクセス権限がありません。"
            is NetworkException.NotFound -> "データが見つかりません。"
            is NetworkException.NetworkError -> "ネットワークエラー。接続を確認してください。"
            is NetworkException.ServerError -> "サーバーエラー。しばらくしてから再度お試しください。"
            else -> error.message ?: "エラーが発生しました。"
        }
    }
}
