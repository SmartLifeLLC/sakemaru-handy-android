package biz.smt_life.android.feature.outbound.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.PickingTask
import kotlinx.coroutines.launch

// ─── Color definitions ───────────────────────────────────────────────────────

private val BackgroundCream = Color.White
private val HeaderRed = Color(0xFFC0392B)
private val HeaderOrange = Color(0xFFE67E22)
private val TextGray = Color(0xFF555555)

private data class CourseCardColors(
    val background: Color,
    val border: Color,
    val titleColor: Color,
    val backgroundPressed: Color,
    val borderPressed: Color
)

private fun courseCardColors(task: PickingTask): CourseCardColors = when {
    task.isFullyProcessed -> CourseCardColors(  // 完了
        background       = Color(0xFFF5F5F5),
        border           = Color(0xFFBDBDBD),
        titleColor       = Color(0xFF757575),
        backgroundPressed = Color(0xFFEEEEEE),
        borderPressed    = Color(0xFF9E9E9E)
    )
    task.registeredCount > 0 -> CourseCardColors(  // 作業中（PICKING/COMPLETED/SHORTAGE件あり）
        background       = Color(0xFFE8F5E9),
        border           = Color(0xFF4CAF50),
        titleColor       = Color(0xFF2E7D32),
        backgroundPressed = Color(0xFFC8E6C9),
        borderPressed    = Color(0xFF388E3C)
    )
    else -> CourseCardColors(  // 未着手
        background       = Color(0xFFFFFDE7),
        border           = Color(0xFFF9A825),
        titleColor       = Color(0xFFE67E22),
        backgroundPressed = Color(0xFFFFF9C4),
        borderPressed    = Color(0xFFF57F17)
    )
}

// ─── Screen ──────────────────────────────────────────────────────────────────

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
    val warehouseName = state.warehouseName

    // Show error messages
    LaunchedEffect(Unit) {
        // Handle errors via snackbar if needed
    }

    Scaffold(
        containerColor = BackgroundCream,
        topBar = {
            Column {
                TopAppBar(
                    modifier = Modifier.height(60.dp),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFDFBF2)
                    ),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る",
                                tint = HeaderRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalShipping,
                                contentDescription = null,
                                tint = HeaderOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "配送コース選択",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderRed
                            )
                            if (warehouseName.isNotEmpty()) {
                                Text(
                                    text = "｜${warehouseName}",
                                    fontSize = 14.sp,
                                    color = HeaderOrange
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(thickness = 2.dp, color = Color(0xFFF9A825))
            }
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.weight(1f)) {
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
}

// ─── Content composables ──────────────────────────────────────────────────────

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

@OptIn(ExperimentalMaterial3Api::class)
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
        Column(modifier = Modifier.fillMaxSize()) {
            // 案内文言
            Text(
                text = "配送コースを選択してください",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 16.dp, bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.taskId }) { task ->
                    PickingTaskCard(
                        task = task,
                        onClick = { onTaskClick(task) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isStartingTask
                    )
                }
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
    val colors = courseCardColors(task)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isPressed) colors.backgroundPressed else colors.background
        ),
        border = BorderStroke(
            2.dp,
            if (isPressed) colors.borderPressed else colors.border
        ),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // [1行] 🚚 アイコン + コース名 + 作業中バッジ
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = colors.titleColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = task.courseName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (task.isFullyProcessed) {
                    Surface(
                        color = Color(0xFF757575),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "完了",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (task.registeredCount > 0) {
                    Surface(
                        color = Color(0xFF4CAF50),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "作業中",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            // [2行] エリア説明
            Text(
                text = task.pickingAreaName,
                fontSize = 13.sp,
                color = TextGray
            )
            // [3行] 出荷指示: X件　検品済: X件
            Text(
                text = "出荷指示: ${task.totalItems}件　検品済: ${task.registeredCount}件",
                fontSize = 13.sp,
                color = TextGray
            )
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(
    name = "Picking Task Card - Pending",
    showBackground = true,
    widthDp = 200
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
    widthDp = 200
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
    widthDp = 200
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
    name = "Task List - Multiple Items (2 Column Grid)",
    showBackground = true,
    backgroundColor = 0xFFFDFBF2,
    widthDp = 400,
    heightDp = 700
)
@Composable
private fun PreviewTaskListWithMultipleItems() {
    MaterialTheme {
        TaskListContent(
            tasks = listOf(
                PickingTask(
                    taskId = 1,
                    courseCode = "A001",
                    courseName = "Aコース（午前便）",
                    pickingAreaName = "1F 冷凍エリア",
                    waveId = 111,
                    pickingAreaCode = "AREA-A",
                    items = List(10) { index ->
                        biz.smt_life.android.core.domain.model.PickingTaskItem(
                            id = index,
                            itemId = index + 100,
                            itemName = "商品 $index",
                            slipNumber = 2023121500 + index,
                            volume = "500ml",
                            capacityCase = 24,
                            janCode = null,
                            plannedQty = 10.0,
                            plannedQtyType = biz.smt_life.android.core.domain.model.QuantityType.CASE,
                            pickedQty = 0.0,
                            status = biz.smt_life.android.core.domain.model.ItemStatus.PENDING,
                            packaging = "ケース",
                            temperatureType = "冷凍",
                            walkingOrder = 1000 + index,
                            images = emptyList()
                        )
                    }
                ),
                PickingTask(
                    taskId = 2,
                    courseCode = "B002",
                    courseName = "Bコース（午後便）",
                    pickingAreaName = "2F 常温エリア",
                    waveId = 112,
                    pickingAreaCode = "AREA-B",
                    items = List(10) { index ->
                        biz.smt_life.android.core.domain.model.PickingTaskItem(
                            id = index + 10,
                            itemId = index + 200,
                            itemName = "商品 ${index + 10}",
                            slipNumber = 2023121600 + index,
                            volume = "350ml",
                            capacityCase = 24,
                            janCode = null,
                            plannedQty = 10.0,
                            plannedQtyType = biz.smt_life.android.core.domain.model.QuantityType.CASE,
                            pickedQty = if (index < 5) 10.0 else 0.0,
                            status = if (index < 5) biz.smt_life.android.core.domain.model.ItemStatus.PICKING
                                     else biz.smt_life.android.core.domain.model.ItemStatus.PENDING,
                            packaging = "ケース",
                            temperatureType = "常温",
                            walkingOrder = 2000 + index,
                            images = emptyList()
                        )
                    }
                ),
                PickingTask(
                    taskId = 3,
                    courseCode = "C003",
                    courseName = "Cコース（深夜便）",
                    pickingAreaName = "3F 冷蔵エリア",
                    waveId = 113,
                    pickingAreaCode = "AREA-C",
                    items = List(10) { index ->
                        biz.smt_life.android.core.domain.model.PickingTaskItem(
                            id = index + 20,
                            itemId = index + 300,
                            itemName = "商品 ${index + 20}",
                            slipNumber = 2023121700 + index,
                            volume = "1000ml",
                            capacityCase = 12,
                            janCode = null,
                            plannedQty = 10.0,
                            plannedQtyType = biz.smt_life.android.core.domain.model.QuantityType.CASE,
                            pickedQty = 10.0,
                            status = biz.smt_life.android.core.domain.model.ItemStatus.COMPLETED,
                            packaging = "ケース",
                            temperatureType = "冷蔵",
                            walkingOrder = 3000 + index,
                            images = emptyList()
                        )
                    }
                ),
                PickingTask(
                    taskId = 4,
                    courseCode = "D004",
                    courseName = "Dコース（特急便）",
                    pickingAreaName = "1F 冷凍エリア",
                    waveId = 114,
                    pickingAreaCode = "AREA-D",
                    items = List(5) { index ->
                        biz.smt_life.android.core.domain.model.PickingTaskItem(
                            id = index + 30,
                            itemId = index + 400,
                            itemName = "商品 ${index + 30}",
                            slipNumber = 2023121800 + index,
                            volume = "250ml",
                            capacityCase = 48,
                            janCode = null,
                            plannedQty = 5.0,
                            plannedQtyType = biz.smt_life.android.core.domain.model.QuantityType.PIECE,
                            pickedQty = 0.0,
                            status = biz.smt_life.android.core.domain.model.ItemStatus.PENDING,
                            packaging = "バラ",
                            temperatureType = "冷凍",
                            walkingOrder = 4000 + index,
                            images = emptyList()
                        )
                    }
                ),
                PickingTask(
                    taskId = 5,
                    courseCode = "E005",
                    courseName = "Eコース（返品処理）",
                    pickingAreaName = "4F 返品エリア",
                    waveId = 115,
                    pickingAreaCode = "AREA-E",
                    items = List(8) { index ->
                        biz.smt_life.android.core.domain.model.PickingTaskItem(
                            id = index + 40,
                            itemId = index + 500,
                            itemName = "商品 ${index + 40}",
                            slipNumber = 2023121900 + index,
                            volume = "750ml",
                            capacityCase = 12,
                            janCode = null,
                            plannedQty = 8.0,
                            plannedQtyType = biz.smt_life.android.core.domain.model.QuantityType.CASE,
                            pickedQty = if (index < 3) 8.0 else 0.0,
                            status = if (index < 3) biz.smt_life.android.core.domain.model.ItemStatus.PICKING
                                     else biz.smt_life.android.core.domain.model.ItemStatus.PENDING,
                            packaging = "ケース",
                            temperatureType = "常温",
                            walkingOrder = 5000 + index,
                            images = emptyList()
                        )
                    }
                )
            ),
            isRefreshing = false,
            onRefresh = {},
            onTaskClick = {},
            isStartingTask = false
        )
    }
}
