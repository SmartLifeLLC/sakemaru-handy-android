package biz.smt_life.android.feature.outbound.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.PickingTask
import kotlinx.coroutines.launch

/**
 * Picking Tasks screen per spec 2.5.1 出庫処理 > ピッキングリスト選択.
 *
 * Features:
 * - Shows only "My tasks" (私の担当) - tasks assigned to current picker
 * - List of tasks with progress (e.g., "5/10")
 * - Status chip based on completion
 * - Status-based navigation:
 *   - PENDING items → Data Input screen
 *   - Only PICKING items → History screen (editable)
 *   - All COMPLETED/SHORTAGE → History screen (read-only)
 * - Pull-to-refresh
 * - Empty and error states
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickingTasksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDataInput: (taskId: Int) -> Unit,
    onNavigateToHistory: (taskId: Int) -> Unit,
    viewModel: PickingTasksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var isStartingTask by remember { mutableStateOf(false) }

    // Show error messages
    LaunchedEffect(Unit) {
        // Handle errors via snackbar if needed
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("出庫処理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // Content
            when (state.tasksState) {
                is TaskListState.Loading -> LoadingContent()
                is TaskListState.Empty -> EmptyContent()
                is TaskListState.Error -> ErrorContent(
                    message = (state.tasksState as TaskListState.Error).message,
                    onRetry = { viewModel.refresh() }
                )
                is TaskListState.Success -> {
                    val tasks = (state.tasksState as TaskListState.Success).tasks
                    TaskListContent(
                        tasks = tasks,
                        isRefreshing = false,
                        onRefresh = { viewModel.refresh() },
                        onTaskClick = { task ->
                            // Status-based navigation (per spec 2.5.1)
                            isStartingTask = true
                            viewModel.selectTask(
                                task = task,
                                onNavigateToDataInput = { selectedTask ->
                                    isStartingTask = false
                                    onNavigateToDataInput(selectedTask.taskId)
                                },
                                onNavigateToHistory = { selectedTask ->
                                    isStartingTask = false
                                    onNavigateToHistory(selectedTask.taskId)
                                },
                                onError = { errorMessage ->
                                    isStartingTask = false
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        snackbarHostState.showSnackbar(
                                            message = errorMessage,
                                            duration = androidx.compose.material3.SnackbarDuration.Short
                                        )
                                    }
                                }
                            )
                        },
                        isStartingTask = isStartingTask
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "該当データがありません",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("再試行")
            }
        }
    }
}

@Composable
private fun TaskListContent(
    tasks: List<PickingTask>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTaskClick: (PickingTask) -> Unit,
    isStartingTask: Boolean = false
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(tasks, key = { it.taskId }) { task ->
                PickingTaskCard(
                    task = task,
                    onClick = { onTaskClick(task) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    enabled = !isStartingTask
                )
            }
        }
    }
}

@Composable
private fun PickingTaskCard(
    task: PickingTask,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Course name and progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "(${task.progressText})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Course code
            Text(
                text = "コード: ${task.courseCode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Picking area
            Text(
                text = "フロア: ${task.pickingAreaName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status chip
            StatusChip(task = task)
        }
    }
}

@Composable
private fun StatusChip(task: PickingTask) {
    val (text, containerColor) = when {
        task.isCompleted -> "完了" to MaterialTheme.colorScheme.primaryContainer
        task.isInProgress -> "進行中" to MaterialTheme.colorScheme.secondaryContainer
        else -> "未着手" to MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ========== Preview Section ==========

@Preview(
    name = "Picking Task Card - Pending",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewPickingTaskCardPending() {
    MaterialTheme {
        PickingTaskCard(
            task = PickingTask(
                taskId = 1,
                courseCode = "A001",
                courseName = "Aコース（午前便）",
                pickingAreaName = "1F 冷凍エリア",
                waveId = 111,
                pickingAreaCode = "pickingAreaCode",
                items = List(10) { index ->
                    biz.smt_life.android.core.domain.model.PickingTaskItem(
                        id = index,
                        itemId = index + 100,
                        itemName = "商品 $index",
                        slipNumber = 202312150 + index,
                        volume = "500ml",
                        capacityCase = 24,
                        janCode = null,
                        plannedQty = 10.0,
                        plannedQtyType = biz.smt_life.android.core.domain.model.QuantityType.CASE,
                        pickedQty = 0.0,
                        status = biz.smt_life.android.core.domain.model.ItemStatus.PENDING,
                        packaging = "packaging",
                        temperatureType = "temperatureType",
                        walkingOrder = 12234,
                        images = emptyList()
                    )
                }
            ),
            onClick = {},
            enabled = true
        )
    }
}

@Preview(
    name = "Picking Task Card - In Progress",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewPickingTaskCardInProgress() {
    MaterialTheme {
        PickingTaskCard(
            task = PickingTask(
                taskId = 2,
                courseCode = "B002",
                courseName = "Bコース（午後便）",
                pickingAreaName = "2F 常温エリア",
                waveId = 111,
                pickingAreaCode = "pickingAreaCode",
                items = List(10) { index ->
                    biz.smt_life.android.core.domain.model.PickingTaskItem(
                        id = index,
                        itemId = index + 200,
                        itemName = "商品 $index",
                        slipNumber = 2023121500 + index,
                        volume = "350ml",
                        capacityCase = 24,
                        janCode = null,
                        plannedQty = 10.0,
                        plannedQtyType = biz.smt_life.android.core.domain.model.QuantityType.CASE,
                        pickedQty = if (index < 5) 10.0 else 0.0,
                        status = if (index < 5) biz.smt_life.android.core.domain.model.ItemStatus.PICKING
                                 else biz.smt_life.android.core.domain.model.ItemStatus.PENDING,
                        packaging = "packaging",
                        temperatureType = "temperatureType",
                        walkingOrder = 12234,
                        images = emptyList()
                    )
                }
            ),
            onClick = {},
            enabled = true
        )
    }
}

@Preview(
    name = "Picking Task Card - Completed",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewPickingTaskCardCompleted() {
    MaterialTheme {
        PickingTaskCard(
            task = PickingTask(
                taskId = 3,
                courseCode = "C003",
                courseName = "Cコース（深夜便）",
                pickingAreaName = "3F 冷蔵エリア",
                waveId = 111,
                pickingAreaCode = "pickingAreaCode",
                items = List(10) { index ->
                    biz.smt_life.android.core.domain.model.PickingTaskItem(
                        id = index,
                        itemId = index + 300,
                        itemName = "商品 $index",
                        slipNumber = 2023121500 + index,
                        volume = "1000ml",
                        capacityCase = 12,
                        janCode = null,
                        plannedQty = 10.0,
                        plannedQtyType = biz.smt_life.android.core.domain.model.QuantityType.CASE,
                        pickedQty = 10.0,
                        status = biz.smt_life.android.core.domain.model.ItemStatus.COMPLETED,
                        packaging = "packaging",
                        temperatureType = "temperatureType",
                        walkingOrder = 12234,
                        images = emptyList()
                    )
                }
            ),
            onClick = {},
            enabled = true
        )
    }
}

@Preview(
    name = "Loading Content",
    showBackground = true,
    widthDp = 400,
    heightDp = 600
)
@Composable
private fun PreviewLoadingContent() {
    MaterialTheme {
        LoadingContent()
    }
}

@Preview(
    name = "Empty Content",
    showBackground = true,
    widthDp = 400,
    heightDp = 600
)
@Composable
private fun PreviewEmptyContent() {
    MaterialTheme {
        EmptyContent()
    }
}

@Preview(
    name = "Error Content",
    showBackground = true,
    widthDp = 400,
    heightDp = 600
)
@Composable
private fun PreviewErrorContent() {
    MaterialTheme {
        ErrorContent(
            message = "ネットワークエラーが発生しました",
            onRetry = {}
        )
    }
}

@Preview(
    name = "Status Chip - All States",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewStatusChips() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pending
            StatusChip(
                task = PickingTask(
                    taskId = 1,
                    courseCode = "A001",
                    courseName = "未着手コース",
                    pickingAreaName = "1F",
                    waveId = 111,
                    pickingAreaCode = "pickingAreaCode",
                    items = List(10) { biz.smt_life.android.core.domain.model.PickingTaskItem(
                        id = it, itemId = it, itemName = "", slipNumber = 0, volume = null,
                        capacityCase = null, janCode = null, plannedQty = 10.0,
                        plannedQtyType = biz.smt_life.android.core.domain.model.QuantityType.CASE,
                        pickedQty = 0.0, status = biz.smt_life.android.core.domain.model.ItemStatus.PENDING,
                        packaging = "packaging",
                        temperatureType = "temperatureType",
                        walkingOrder = 12234,
                        images = emptyList()
                    ) }
                )
            )
            // In Progress
            StatusChip(
                task = PickingTask(
                    taskId = 2,
                    courseCode = "B002",
                    courseName = "進行中コース",
                    pickingAreaName = "2F",
                    waveId = 111,
                    pickingAreaCode = "pickingAreaCode",
                    items = List(10) { index -> biz.smt_life.android.core.domain.model.PickingTaskItem(
                        id = index, itemId = index, itemName = "", slipNumber = 0, volume = null,
                        capacityCase = null, janCode = null, plannedQty = 10.0,
                        plannedQtyType = biz.smt_life.android.core.domain.model.QuantityType.CASE,
                        pickedQty = if (index < 5) 10.0 else 0.0,
                        status = if (index < 5) biz.smt_life.android.core.domain.model.ItemStatus.PICKING
                                 else biz.smt_life.android.core.domain.model.ItemStatus.PENDING,
                        packaging = "packaging",
                        temperatureType = "temperatureType",
                        walkingOrder = 12234,
                        images = emptyList()
                    ) }
                )
            )
            // Completed
            StatusChip(
                task = PickingTask(
                    taskId = 3,
                    courseCode = "C003",
                    courseName = "完了コース",
                    pickingAreaName = "3F",
                    waveId = 11,
                    pickingAreaCode = "pickingAreaCode",
                    items = List(10) { biz.smt_life.android.core.domain.model.PickingTaskItem(
                        id = it, itemId = it, itemName = "", slipNumber = 0, volume = null,
                        capacityCase = null, janCode = null, plannedQty = 10.0,
                        plannedQtyType = biz.smt_life.android.core.domain.model.QuantityType.CASE,
                        pickedQty = 10.0, status = biz.smt_life.android.core.domain.model.ItemStatus.COMPLETED,
                        packaging = "packaging",
                        temperatureType = "temperatureType",
                        walkingOrder = 12234,
                        images = emptyList()
                    ) }
                )
            )
        }
    }
}
