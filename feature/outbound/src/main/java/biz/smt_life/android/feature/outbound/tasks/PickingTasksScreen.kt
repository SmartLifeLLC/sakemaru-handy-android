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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.PickingTask

/**
 * Picking Tasks screen per spec 2.5.1 出庫処理 > ピッキングリスト選択.
 *
 * Features:
 * - Two tabs: My Area (担当エリア) and All Courses (全コース)
 * - List of tasks with progress (e.g., "5/10")
 * - Status chip based on completion
 * - Pull-to-refresh for each tab
 * - Empty and error states
 * - Tap to navigate to picking detail
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickingTasksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPickingDetail: (courseCode: String) -> Unit,
    viewModel: PickingTasksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(selectedTabIndex = state.activeTab.ordinal) {
                Tab(
                    selected = state.activeTab == PickingTab.MY_AREA,
                    onClick = { viewModel.onTabSelected(PickingTab.MY_AREA) },
                    text = { Text("私の担当") }
                )
                Tab(
                    selected = state.activeTab == PickingTab.ALL_COURSES,
                    onClick = { viewModel.onTabSelected(PickingTab.ALL_COURSES) },
                    text = { Text("全体") }
                )
            }

            // Tab content
            when (state.currentTabState) {
                is TaskListState.Loading -> LoadingContent()
                is TaskListState.Empty -> EmptyContent()
                is TaskListState.Error -> ErrorContent(
                    message = (state.currentTabState as TaskListState.Error).message,
                    onRetry = { viewModel.refresh() }
                )
                is TaskListState.Success -> {
                    val tasks = (state.currentTabState as TaskListState.Success).tasks
                    TaskListContent(
                        tasks = tasks,
                        isRefreshing = false,
                        onRefresh = { viewModel.refresh() },
                        onTaskClick = { task ->
                            // Navigate using courseCode to maintain compatibility with existing flow
                            onNavigateToPickingDetail(task.courseCode)
                        }
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
    onTaskClick: (PickingTask) -> Unit
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PickingTaskCard(
    task: PickingTask,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
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
                text = "エリア: ${task.pickingAreaName}",
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
