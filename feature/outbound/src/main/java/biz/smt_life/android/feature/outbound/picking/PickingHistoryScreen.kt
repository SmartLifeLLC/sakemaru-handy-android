package biz.smt_life.android.feature.outbound.picking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType

// ===== P21/P22 共通カラー =====
private val TitleRed     = Color(0xFFC0392B)
private val AccentOrange = Color(0xFFE67E22)
private val DividerGold  = Color(0xFFF9A825)
private val BodyBg       = Color.White
private val HeaderBg     = Color(0xFFFDFBF2)
private val TextPrimary  = Color(0xFF212529)
private val TextSecond   = Color(0xFF555555)
private val BorderGray   = Color(0xFFCCCCCC)
private val ReadonlyText = Color(0xFF888888)
private val BadgeGreen   = Color(0xFF27AE60)

/**
 * Picking History Screen (2.5.3 - 出庫処理＞履歴).
 *
 * Two modes:
 * - Editable mode: show PICKING items with delete (F3) and confirm-all (F4) buttons
 * - Read-only mode: all items COMPLETED/SHORTAGE, no action buttons
 *
 * @param taskId The picking task ID to show history for
 * @param onNavigateBack Navigate back to previous screen
 * @param onHistoryConfirmed Callback when user confirms all (navigate back to course list)
 * @param viewModel ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickingHistoryScreen(
    taskId: Int,
    onNavigateBack: () -> Unit,
    onHistoryConfirmed: () -> Unit,
    onItemClick: (PickingTaskItem) -> Unit = { _ -> },
    viewModel: PickingHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize viewModel with taskId - it will observe the repository flow
    LaunchedEffect(taskId) {
        viewModel.initialize(taskId)
    }

    // Show error messages
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Delete confirmation dialog
    if (state.itemToDelete != null) {
        DeleteConfirmationDialog(
            item = state.itemToDelete!!,
            onConfirm = {
                viewModel.deleteHistoryItem(
                    item = state.itemToDelete!!,
                    onSuccess = onNavigateBack
                )
            },
            onCancel = { viewModel.dismissDeleteDialog() }
        )
    }

    // Confirm-all dialog
    if (state.showConfirmDialog) {
        ConfirmAllDialog(
            isConfirming = state.isConfirming,
            onConfirm = {
                viewModel.confirmAll(onSuccess = onHistoryConfirmed)
            },
            onCancel = { viewModel.dismissConfirmDialog() }
        )
    }

    Scaffold(
        containerColor = BodyBg,
        topBar = {
            Column {
                TopAppBar(
                    modifier = Modifier.height(60.dp),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Inventory2,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "出庫履歴",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TitleRed
                            )
                            val courseName = state.task?.courseName
                            if (!courseName.isNullOrBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = BadgeGreen,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = courseName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            lineHeight = 12.sp
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            text = "▼",
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            lineHeight = 10.sp
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = AccentOrange,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${state.task?.registeredCount ?: 0} / ${state.task?.totalItems ?: 0}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp)
                                )
                            }
                            if (state.warehouseName.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "｜${state.warehouseName}",
                                    fontSize = 14.sp,
                                    color = AccentOrange
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る",
                                tint = TitleRed
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HeaderBg
                    )
                )
                HorizontalDivider(thickness = 2.dp, color = DividerGold)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (state.isEditableMode && state.historyItems.isNotEmpty()) {
                HistoryBottomBar(
                    onConfirmAllClick = { viewModel.showConfirmDialog() },
                    canConfirm = state.canConfirmAll
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentOrange)
                }
            }
            state.historyItems.isEmpty() && state.isReadOnlyMode -> {
                // Read-only mode with no PICKING items (all completed)
                ReadOnlyModeContent(
                    task = state.task!!,
                    modifier = Modifier.padding(padding)
                )
            }
            state.historyItems.isEmpty() -> {
                // No history items at all
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "出庫履歴がありません",
                        fontSize = 16.sp,
                        color = ReadonlyText
                    )
                }
            }
            else -> {
                HistoryListContent(
                    state = state,
                    onItemClick = onItemClick,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun HistoryListContent(
    state: PickingHistoryState,
    onItemClick: (PickingTaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.historyItems, key = { it.id }) { item ->
            HistoryItemCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: PickingTaskItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, BorderGray),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1: Item name
            Text(
                text = item.itemName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Row 2: JAN / 伝票番号
            val janDisplay = item.janCode ?: "-"
            Text(
                text = "$janDisplay / 伝票番号: ${item.slipNumber}",
                fontSize = 13.sp,
                color = TextSecond
            )

            // Row 3: 出庫数 / 受注数
            val qtyLabel = when (item.plannedQtyType) {
                QuantityType.CASE -> "ケース"
                QuantityType.PIECE -> "バラ"
            }
            Text(
                text = "出庫: ${String.format("%.0f", item.pickedQty)} $qtyLabel / 受注: ${String.format("%.0f", item.plannedQty)} $qtyLabel",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun ReadOnlyModeContent(
    task: PickingTask,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AccentOrange
            )
            Text(
                text = "すべての商品が確定済みです",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = task.courseName,
                fontSize = 16.sp,
                color = TextSecond
            )
            Text(
                text = "履歴は参照のみ可能です。変更はできません。",
                fontSize = 14.sp,
                color = ReadonlyText
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 13.sp,
            color = TextSecond
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun StatusBadge(status: biz.smt_life.android.core.domain.model.ItemStatus) {
    val (text, color) = when (status) {
        biz.smt_life.android.core.domain.model.ItemStatus.PENDING   -> "未登録" to TitleRed
        biz.smt_life.android.core.domain.model.ItemStatus.PICKING   -> "登録済み" to AccentOrange
        biz.smt_life.android.core.domain.model.ItemStatus.COMPLETED -> "完了" to BadgeGreen
        biz.smt_life.android.core.domain.model.ItemStatus.SHORTAGE  -> "欠品" to TitleRed
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HistoryBottomBar(
    onConfirmAllClick: () -> Unit,
    canConfirm: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onConfirmAllClick,
                enabled = canConfirm,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                modifier = Modifier.widthIn(min = 200.dp)
            ) {
                Text("確定")
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    item: PickingTaskItem,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("削除確認") },
        text = {
            Column {
                Text("以下の履歴を削除しますか？")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.itemName,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = TitleRed)
            ) {
                Text("削除")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
private fun ConfirmAllDialog(
    isConfirming: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isConfirming) onCancel() }
    ) {
        OutlinedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = BadgeGreen,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "すべての出庫履歴を確定しますか？",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "確定後は変更できません。",
                    fontSize = 14.sp,
                    color = ReadonlyText
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isConfirming,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .widthIn(min = 120.dp),
                        border = BorderStroke(1.dp, BorderGray)
                    ) {
                        Text(
                            text = "キャンセル",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = !isConfirming,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .widthIn(min = 120.dp)
                    ) {
                        if (isConfirming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "確定",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========== Preview Section ==========

@Preview(
    name = "History Item Card - Picking",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewHistoryItemCardPicking() {
    MaterialTheme {
        HistoryItemCard(
            item = PickingTaskItem(
                id = 1,
                itemId = 101,
                itemName = "サッポロ生ビール黒ラベル 500ml缶",
                slipNumber = 2023121500,
                volume = "500ml",
                capacityCase = 24,
                janCode = "4901777123456",
                plannedQty = 24.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 20.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING,
                packaging = "packaging",
                temperatureType = "temperatureType",
                walkingOrder = 12234,
                images = emptyList()
            ),
            onClick = {}
        )
    }
}

@Preview(
    name = "History Item Card - Completed",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewHistoryItemCardCompleted() {
    MaterialTheme {
        HistoryItemCard(
            item = PickingTaskItem(
                id = 2,
                itemId = 102,
                itemName = "アサヒスーパードライ 350ml缶",
                slipNumber = 2023121500,
                volume = "350ml",
                capacityCase = 24,
                janCode = "4901777234567",
                plannedQty = 12.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 12.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.COMPLETED,
                packaging = "packaging",
                temperatureType = "temperatureType",
                walkingOrder = 12234,
                images = emptyList()
            ),
            onClick = {}
        )
    }
}

@Preview(
    name = "History Item Card - Shortage",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewHistoryItemCardShortage() {
    MaterialTheme {
        HistoryItemCard(
            item = PickingTaskItem(
                id = 3,
                itemId = 103,
                itemName = "キリン一番搾り 500ml缶",
                slipNumber = 2023121500,
                volume = "500ml",
                capacityCase = 24,
                janCode = "4901777345678",
                plannedQty = 10.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 0.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.SHORTAGE,
                packaging = "packaging",
                temperatureType = "temperatureType",
                walkingOrder = 12234,
                images = emptyList()
            ),
            onClick = {}
        )
    }
}

@Preview(
    name = "Read-Only Mode Content",
    showBackground = true,
    widthDp = 400,
    heightDp = 600
)
@Composable
private fun PreviewReadOnlyModeContent() {
    MaterialTheme {
        ReadOnlyModeContent(
            task = PickingTask(
                taskId = 1,
                courseCode = "A001",
                courseName = "Aコース（午前便）",
                pickingAreaName = "1F 冷凍エリア",
                waveId = 11,
                pickingAreaCode = "pickingAreaCode",
                items = emptyList()
            )
        )
    }
}

@Preview(
    name = "Delete Confirmation Dialog",
    showBackground = true
)
@Composable
private fun PreviewDeleteConfirmationDialog() {
    MaterialTheme {
        DeleteConfirmationDialog(
            item = PickingTaskItem(
                id = 1,
                itemId = 101,
                itemName = "サッポロ生ビール黒ラベル 500ml缶",
                slipNumber = 2023121500,
                volume = "500ml",
                capacityCase = 24,
                janCode = "4901777123456",
                plannedQty = 24.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 20.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING,
                packaging = "packaging",
                temperatureType = "temperatureType",
                walkingOrder = 12234,
                images = emptyList()
            ),
            onConfirm = {},
            onCancel = {}
        )
    }
}

@Preview(
    name = "Confirm All Dialog",
    showBackground = true
)
@Composable
private fun PreviewConfirmAllDialog() {
    MaterialTheme {
        ConfirmAllDialog(
            isConfirming = false,
            onConfirm = {},
            onCancel = {}
        )
    }
}

@Preview(
    name = "Status Badge - All States",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewStatusBadges() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusBadge(status = biz.smt_life.android.core.domain.model.ItemStatus.PENDING)
            StatusBadge(status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING)
            StatusBadge(status = biz.smt_life.android.core.domain.model.ItemStatus.COMPLETED)
            StatusBadge(status = biz.smt_life.android.core.domain.model.ItemStatus.SHORTAGE)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Full Page - Editable Mode (Landscape)",
    showBackground = true,
    widthDp = 1280,
    heightDp = 800
)
@Composable
private fun PreviewFullPageEditableMode() {
    val mockTask = PickingTask(
        taskId = 1,
        courseCode = "A001",
        courseName = "Aコース（午前便）",
        pickingAreaName = "1F 冷凍エリア",
        waveId = 111,
        pickingAreaCode = "AREA-A",
        items = listOf(
            PickingTaskItem(
                id = 1,
                itemId = 101,
                itemName = "サッポロ生ビール黒ラベル 500ml缶",
                slipNumber = 2023121501,
                volume = "500ml",
                capacityCase = 24,
                janCode = "4901777123456",
                plannedQty = 24.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 20.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING,
                packaging = "ケース",
                temperatureType = "冷凍",
                walkingOrder = 1001,
                images = emptyList()
            ),
            PickingTaskItem(
                id = 2,
                itemId = 102,
                itemName = "アサヒスーパードライ 350ml缶",
                slipNumber = 2023121502,
                volume = "350ml",
                capacityCase = 24,
                janCode = "4901777234567",
                plannedQty = 12.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 12.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING,
                packaging = "ケース",
                temperatureType = "冷凍",
                walkingOrder = 1002,
                images = emptyList()
            ),
            PickingTaskItem(
                id = 3,
                itemId = 103,
                itemName = "キリン一番搾り 500ml缶",
                slipNumber = 2023121503,
                volume = "500ml",
                capacityCase = 24,
                janCode = "4901777345678",
                plannedQty = 10.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 0.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING,
                packaging = "ケース",
                temperatureType = "冷凍",
                walkingOrder = 1003,
                images = emptyList()
            ),
            PickingTaskItem(
                id = 4,
                itemId = 104,
                itemName = "サントリー ザ・プレミアム・モルツ",
                slipNumber = 2023121504,
                volume = "350ml",
                capacityCase = 24,
                janCode = "4901777456789",
                plannedQty = 15.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 15.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING,
                packaging = "ケース",
                temperatureType = "冷凍",
                walkingOrder = 1004,
                images = emptyList()
            ),
            PickingTaskItem(
                id = 5,
                itemId = 105,
                itemName = "エビスビール 500ml缶",
                slipNumber = 2023121505,
                volume = "500ml",
                capacityCase = 24,
                janCode = null,
                plannedQty = 8.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 5.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING,
                packaging = "ケース",
                temperatureType = "冷凍",
                walkingOrder = 1005,
                images = emptyList()
            ),
            PickingTaskItem(
                id = 6,
                itemId = 106,
                itemName = "アサヒドライゼロ 350ml缶",
                slipNumber = 2023121506,
                volume = "350ml",
                capacityCase = 24,
                janCode = "4901777567890",
                plannedQty = 20.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 20.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING,
                packaging = "ケース",
                temperatureType = "冷凍",
                walkingOrder = 1006,
                images = emptyList()
            )
        )
    )

    val mockState = PickingHistoryState(
        task = mockTask,
        isLoading = false,
        errorMessage = null
    )

    MaterialTheme {
        Scaffold(
            containerColor = BodyBg,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Inventory2,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "出庫履歴",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TitleRed
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "戻る",
                                    tint = TitleRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = HeaderBg
                        )
                    )
                    HorizontalDivider(thickness = 2.dp, color = DividerGold)
                }
            },
            bottomBar = {
                HistoryBottomBar(
                    onConfirmAllClick = {},
                    canConfirm = true
                )
            }
        ) { padding ->
            HistoryListContent(
                state = mockState,
                onItemClick = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Full Page - Read-Only Mode (Landscape)",
    showBackground = true,
    widthDp = 1280,
    heightDp = 800
)
@Composable
private fun PreviewFullPageReadOnlyMode() {
    val mockTask = PickingTask(
        taskId = 2,
        courseCode = "B002",
        courseName = "Bコース（午後便）",
        pickingAreaName = "2F 常温エリア",
        waveId = 112,
        pickingAreaCode = "AREA-B",
        items = listOf(
            PickingTaskItem(
                id = 1,
                itemId = 201,
                itemName = "サッポロ生ビール黒ラベル 500ml缶",
                slipNumber = 2023121601,
                volume = "500ml",
                capacityCase = 24,
                janCode = "4901777123456",
                plannedQty = 24.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 24.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.COMPLETED,
                packaging = "ケース",
                temperatureType = "常温",
                walkingOrder = 2001,
                images = emptyList()
            ),
            PickingTaskItem(
                id = 2,
                itemId = 202,
                itemName = "アサヒスーパードライ 350ml缶",
                slipNumber = 2023121602,
                volume = "350ml",
                capacityCase = 24,
                janCode = "4901777234567",
                plannedQty = 12.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 12.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.COMPLETED,
                packaging = "ケース",
                temperatureType = "常温",
                walkingOrder = 2002,
                images = emptyList()
            )
        )
    )

    MaterialTheme {
        Scaffold(
            containerColor = BodyBg,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Inventory2,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "出庫履歴",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TitleRed
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "戻る",
                                    tint = TitleRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = HeaderBg
                        )
                    )
                    HorizontalDivider(thickness = 2.dp, color = DividerGold)
                }
            }
        ) { padding ->
            ReadOnlyModeContent(
                task = mockTask,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Full Page - Editable Mode with History Items (Landscape)",
    showBackground = true,
    widthDp = 800,
    heightDp = 1280
)
@Composable
private fun PreviewFullPageWithPickingItems() {
    val mockTask = PickingTask(
        taskId = 3,
        courseCode = "C003",
        courseName = "Cコース（深夜便）",
        pickingAreaName = "3F 冷蔵エリア",
        waveId = 113,
        pickingAreaCode = "AREA-C",
        items = listOf(
            PickingTaskItem(
                id = 1,
                itemId = 301,
                itemName = "サッポロ生ビール黒ラベル 500ml缶",
                slipNumber = 2023121701,
                volume = "500ml",
                capacityCase = 24,
                janCode = "4901777123456",
                plannedQty = 24.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 20.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING,
                packaging = "ケース",
                temperatureType = "冷蔵",
                walkingOrder = 3001,
                images = emptyList()
            ),
            PickingTaskItem(
                id = 2,
                itemId = 302,
                itemName = "キリン一番搾り 500ml缶",
                slipNumber = 2023121702,
                volume = "500ml",
                capacityCase = 24,
                janCode = "4901777345678",
                plannedQty = 10.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 0.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.SHORTAGE,
                packaging = "ケース",
                temperatureType = "冷蔵",
                walkingOrder = 3002,
                images = emptyList()
            ),
            PickingTaskItem(
                id = 3,
                itemId = 303,
                itemName = "アサヒスーパードライ 350ml缶",
                slipNumber = 2023121703,
                volume = "350ml",
                capacityCase = 24,
                janCode = "4901777234567",
                plannedQty = 12.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 12.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING,
                packaging = "ケース",
                temperatureType = "冷蔵",
                walkingOrder = 3003,
                images = emptyList()
            ),
            PickingTaskItem(
                id = 4,
                itemId = 304,
                itemName = "サントリー ザ・プレミアム・モルツ",
                slipNumber = 2023121704,
                volume = "350ml",
                capacityCase = 24,
                janCode = "4901777456789",
                plannedQty = 15.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 15.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.PICKING,
                packaging = "ケース",
                temperatureType = "冷蔵",
                walkingOrder = 3004,
                images = emptyList()
            )
        )
    )

    val mockState = PickingHistoryState(
        task = mockTask,
        isLoading = false,
        errorMessage = null
    )

    MaterialTheme {
        Scaffold(
            containerColor = BodyBg,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Inventory2,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "出庫履歴",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TitleRed
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "戻る",
                                    tint = TitleRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = HeaderBg
                        )
                    )
                    HorizontalDivider(thickness = 2.dp, color = DividerGold)
                }
            },
            bottomBar = {
                HistoryBottomBar(
                    onConfirmAllClick = {},
                    canConfirm = true
                )
            }
        ) { padding ->
            HistoryListContent(
                state = mockState,
                onItemClick = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}
