package biz.smt_life.android.feature.outbound.picking

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * @param viewModel ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickingHistoryScreen(
    taskId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToMain: () -> Unit = {},
    onItemClick: (PickingTaskItem) -> Unit = { _ -> },
    viewModel: PickingHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ===== Orientation control (shared with P21) =====
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences("p21_orientation_prefs", Context.MODE_PRIVATE) }
    var isPortrait by remember { mutableStateOf(prefs.getBoolean("p21_is_portrait", false)) }

    LaunchedEffect(isPortrait) {
        activity?.requestedOrientation = if (isPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    fun toggleOrientation() {
        isPortrait = !isPortrait
        prefs.edit().putBoolean("p21_is_portrait", isPortrait).apply()
    }

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


    Scaffold(
        containerColor = BodyBg,
        topBar = {
            Column {
                TopAppBar(
                    modifier = Modifier.height(60.dp),
                    title = {
                        val headerText = "${state.registeredGroupCount}/${state.totalGroupCount} 完了"
                        if (isPortrait) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = onNavigateBack,
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "戻る",
                                            tint = TitleRed,
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("もどる", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TitleRed)
                                    }
                                }
                                Spacer(Modifier.width(24.dp))
                                Surface(
                                    onClick = { toggleOrientation() },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "画面回転",
                                            tint = AccentOrange,
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("画面回転", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    Surface(
                                        onClick = onNavigateBack,
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Transparent
                                    ) {
                                        Text("もどる", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TitleRed, modifier = Modifier.padding(horizontal = 8.dp))
                                    }
                                }
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(headerText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                                }
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                    Surface(
                                        onClick = { toggleOrientation() },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Transparent
                                    ) {
                                        Text("画面回転", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentOrange, modifier = Modifier.padding(horizontal = 8.dp))
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {},
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HeaderBg
                    )
                )
                HorizontalDivider(thickness = 2.dp, color = DividerGold)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {}
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
            state.historyItems.isEmpty() -> {
                // No registered items at all
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
                    isPortrait = isPortrait,
                    onItemClick = if (state.isReadOnlyMode) { _ -> } else onItemClick,
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun HistoryListContent(
    state: PickingHistoryState,
    isPortrait: Boolean,
    onItemClick: (PickingTaskItem) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val groupedItems = state.groupedHistoryItems

    Column(modifier = modifier.fillMaxSize()) {
        if (isPortrait) {
            // Progress count at top for Portrait mode
            Surface(
                color = AccentOrange,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${state.registeredGroupCount} / ${state.totalGroupCount} 件完了",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
            }
        }

        // Scrollable Grid
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(if (isPortrait) 1 else 2),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groupedItems.size, key = { groupedItems[it].itemId }) { index ->
                val groupedItem = groupedItems[index]
                GroupedHistoryItemCard(
                    item = groupedItem,
                    onClick = {
                        state.historyItems.firstOrNull { it.itemId == groupedItem.itemId }
                            ?.let { onItemClick(it) }
                    }
                )
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onNavigateBack,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("もどる", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GroupedHistoryItemCard(
    item: GroupedHistoryItem,
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
            // Row 1: Location code & Work number
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.locationCode ?: "-",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "作業番号 ${item.workNumber}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecond
                )
            }

            // Row 2: Item name
            Text(
                text = item.itemName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // Row 4: Quantities
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.totalCasePlanned > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ケース: ${String.format("%.0f", item.totalCasePicked)}/${String.format("%.0f", item.totalCasePlanned)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (item.totalCasePicked < item.totalCasePlanned) {
                            Text(
                                text = " (欠品数: ${String.format("%.0f", item.totalCasePlanned - item.totalCasePicked)})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                    }
                }
                if (item.totalPiecePlanned > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "バラ: ${String.format("%.0f", item.totalPiecePicked)}/${String.format("%.0f", item.totalPiecePlanned)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (item.totalPiecePicked < item.totalPiecePlanned) {
                            Text(
                                text = " (欠品数: ${String.format("%.0f", item.totalPiecePlanned - item.totalPiecePicked)})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                    }
                }
            }
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


// ========== Preview Section ==========

@Preview(
    name = "Grouped History Item Card",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewGroupedHistoryItemCard() {
    MaterialTheme {
        GroupedHistoryItemCard(
            item = GroupedHistoryItem(
                itemId = 101,
                itemName = "サッポロ生ビール黒ラベル 500ml缶",
                locationCode = "A-01-02",
                janCode = "4901777123456",
                totalCasePlanned = 24.0,
                totalCasePicked = 20.0,
                totalPiecePlanned = 10.0,
                totalPiecePicked = 8.0,
                customerCount = 3
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
                        modifier = Modifier.height(60.dp),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    color = AccentOrange,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "3 / 6 件完了",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            Surface(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "戻る",
                                        tint = TitleRed,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("もどる", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleRed)
                                }
                            }
                        },
                        actions = {
                            Surface(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "画面回転",
                                        tint = AccentOrange,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("画面回転", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = HeaderBg
                        )
                    )
                    HorizontalDivider(thickness = 2.dp, color = DividerGold)
                }
            },
            bottomBar = {}
        ) { padding ->
            HistoryListContent(
                state = mockState,
                isPortrait = false,
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
                        modifier = Modifier.height(60.dp),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    color = AccentOrange,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "3 / 6 件完了",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            Surface(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "戻る",
                                        tint = TitleRed,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("もどる", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleRed)
                                }
                            }
                        },
                        actions = {
                            Surface(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "画面回転",
                                        tint = AccentOrange,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("画面回転", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                                }
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
                        modifier = Modifier.height(60.dp),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    color = AccentOrange,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "3 / 6 件完了",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            Surface(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "戻る",
                                        tint = TitleRed,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("もどる", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleRed)
                                }
                            }
                        },
                        actions = {
                            Surface(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "画面回転",
                                        tint = AccentOrange,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("画面回転", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = HeaderBg
                        )
                    )
                    HorizontalDivider(thickness = 2.dp, color = DividerGold)
                }
            },
            bottomBar = {}
        ) { padding ->
            HistoryListContent(
                state = mockState,
                isPortrait = false,
                onItemClick = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}
