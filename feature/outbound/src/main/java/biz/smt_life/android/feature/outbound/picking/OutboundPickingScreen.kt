package biz.smt_life.android.feature.outbound.picking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.ItemStatus
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType

/**
 * Outbound Picking Screen (2.5.2 - 出庫データ入力).
 * Displays current picking item and allows quantity input.
 *
 * @param task The picking task to work with
 * @param onNavigateBack Navigate back to course list or previous screen
 * @param onNavigateToCourseList Navigate back to course list (コース(F6) button)
 * @param onNavigateToHistory Navigate to picking history (履歴(F7) button)
 * @param onNavigateToMain Navigate to main menu (ホーム(F8) button)
 * @param onTaskCompleted Callback when task is successfully completed (確定)
 * @param viewModel ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundPickingScreen(
    task: PickingTask,
    onNavigateBack: () -> Unit,
    onNavigateToCourseList: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMain: () -> Unit,
    onTaskCompleted: () -> Unit,
    viewModel: OutboundPickingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize viewModel with task
    LaunchedEffect(task.taskId) {
        viewModel.initialize(task)
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

    // Completion Dialog
    if (state.showCompletionDialog) {
        CompletionConfirmationDialog(
            isCompleting = state.isCompleting,
            onConfirm = {
                viewModel.completeTask(onSuccess = onTaskCompleted)
            },
            onCancel = {
                viewModel.dismissCompletionDialog()
                onNavigateToHistory()
            }
        )
    }

    // Image Viewer Dialog
    if (state.showImageDialog && state.currentItem != null) {
        ImageViewerDialog(
            images = state.currentItem!!.images,
            onDismiss = { viewModel.dismissImageDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("出庫データ入力") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る(F4)"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToMain) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "ホーム(F8)"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            OutboundPickingBottomBar(
                state = state,
                onPrevClick = viewModel::moveToPrevItem,
                onNextClick = viewModel::moveToNextItem,
                onRegisterClick = viewModel::registerCurrentItem,
                onImageClick = { viewModel.showImageDialog() },
                onCourseClick = onNavigateToCourseList,
                onHistoryClick = onNavigateToHistory
            )
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
                    CircularProgressIndicator()
                }
            }
            state.currentItem != null && state.task != null -> {
                OutboundPickingContent(
                    state = state,
                    onPickedQtyChange = viewModel::onPickedQtyChange,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("商品がありません")
                }
            }
        }
    }
}

@Composable
private fun OutboundPickingContent(
    state: OutboundPickingState,
    onPickedQtyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentItem = state.currentItem!!
    val task = state.task!!

    // Split into left and right panes, each with independent scrolling
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // LEFT PANE: Course info, Item info, Product details (read-only)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Course Header (use counters from state, not filtered task)
            CourseHeaderCard(
                courseName = task.courseName,
                pickingAreaName = task.pickingAreaName,
                registeredCount = state.registeredCount, // From originalTask, not filtered
                totalCount = state.totalCount             // From originalTask, not filtered
            )

            // Item Information Card (容量, 入数, JAN)
            ItemInformationCard(
                item = currentItem,
                slipNumber = currentItem.slipNumber.toString(),
            )
        }

        // RIGHT PANE: Quantity Input (editable)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quantity Input Card
            QuantityInputCard(
                plannedQty = currentItem.plannedQty,
                quantityType = state.quantityTypeLabel,
                pickedQtyInput = state.pickedQtyInput,
                onPickedQtyChange = onPickedQtyChange,
                isUpdating = state.isUpdating,
                formatQuantity = state::formatQuantity
            )
        }
    }
}

@Composable
private fun CourseHeaderCard(
    courseName: String,
    pickingAreaName: String,
    registeredCount: Int,  // Changed from processedCount
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "コース",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$registeredCount / $totalCount",  // Use registeredCount
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = courseName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "フロア: $pickingAreaName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LinearProgressIndicator(
                progress = { if (totalCount > 0) registeredCount.toFloat() / totalCount.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ItemInformationCard(
    item: PickingTaskItem,
    slipNumber: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()

            // slipNumber (伝票番号)
            InfoRow(label = "伝票番号", value = slipNumber)

            // Volume (容量)
            if (item.volume != null) {
                InfoRow(label = "容量", value = item.volume!!)
            } else {
                InfoRow(label = "容量", value = "—")
            }

            // Capacity per case (入数)
            if (item.capacityCase != null) {
                InfoRow(label = "入数", value = "${item.capacityCase} 個/ケース")
            } else {
                InfoRow(label = "入数", value = "—")
            }

            // JAN code
            if (item.janCode != null) {
                InfoRow(label = "JAN", value = item.janCode!!)
            } else {
                InfoRow(label = "JAN", value = "—")
            }
        }
    }
}

@Composable
private fun QuantityInputCard(
    plannedQty: Double,
    quantityType: String,
    pickedQtyInput: String,
    onPickedQtyChange: (String) -> Unit,
    isUpdating: Boolean,
    formatQuantity: (Double, String) -> String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "数量",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Planned Quantity (Read-only)
            OutlinedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "出荷数量",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = String.format("%.1f %s", plannedQty, quantityType),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            // Picked Quantity (Editable)
            Text(
                text = "出庫数量",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = pickedQtyInput,
                onValueChange = onPickedQtyChange,
                label = { Text("出庫数量 ($quantityType)") },
                enabled = !isUpdating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text("数量を入力してください。不足の場合は0を入力。")
                }
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun OutboundPickingBottomBar(
    state: OutboundPickingState,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onImageClick: () -> Unit,
    onCourseClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        val smallShape = RoundedCornerShape(6.dp)
        val smallTextStyle = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            lineHeight = 12.sp
        )
        val contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)

        Column(modifier = Modifier.padding(12.dp)) {
            // Top row: Register, Prev, Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val buttonModifier = Modifier
                    .weight(1f)

                // 画像(F5) - Show image viewer if images are available
                OutlinedButton(
                    onClick = onImageClick,
                    enabled = state.hasImages && !state.isUpdating,
                    shape = smallShape,
                    contentPadding = contentPadding,
                    modifier = buttonModifier
                ) {
                    Text("商品の画像", style = smallTextStyle)
                }

                // コース(F6)
                OutlinedButton(
                    onClick = onCourseClick,
                    enabled = !state.isUpdating,
                    shape = smallShape,
                    contentPadding = contentPadding,
                    modifier = buttonModifier
                ) {
                    Text(
                        "コース変更",
                        style = smallTextStyle,
                        textAlign = TextAlign.Center,
                    )
                }

                // 履歴(F7)
                OutlinedButton(
                    onClick = onHistoryClick,
                    enabled = !state.isUpdating,
                    shape = smallShape,
                    contentPadding = contentPadding,
                    modifier = buttonModifier
                ) {
                    Text("履歴", style = smallTextStyle)
                }

                // 前へ(F2)
                OutlinedButton(
                    onClick = onPrevClick,
                    enabled = state.canMovePrev && !state.isUpdating,
                    shape = smallShape,
                    contentPadding = contentPadding,
                    modifier = buttonModifier
                ) {
                    Text("前へ", style = smallTextStyle)
                }

                // 登録(F1)
                Button(
                    onClick = onRegisterClick,
                    enabled = state.canRegister,
                    shape = smallShape,
                    contentPadding = contentPadding,
                    modifier = buttonModifier
                ) {
                    if (state.isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("登録", style = smallTextStyle)
                    }
                }

                // 次へ(F3)
                OutlinedButton(
                    onClick = onNextClick,
                    enabled = state.canMoveNext && !state.isUpdating,
                    shape = smallShape,
                    contentPadding = contentPadding,
                    modifier = buttonModifier
                ) {
                    Text("次へ", style = smallTextStyle)
                }
            }
        }
    }
}

/**
 * Completion Confirmation Dialog (per spec 2.5.2).
 * Message: すべての商品登録を完了しました。確定しますか？
 * Buttons: 確定 (complete task) / キャンセル (navigate to history)
 */
@Composable
private fun CompletionConfirmationDialog(
    isCompleting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isCompleting) onCancel() },
        title = { Text("完了確認") },
        text = { Text("すべての商品登録を完了しました。確定しますか？") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isCompleting
            ) {
                if (isCompleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("確定")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isCompleting
            ) {
                Text("キャンセル")
            }
        }
    )
}

/**
 * Image Viewer Dialog (画像 F5).
 * Shows product images from the server in a simple dialog.
 * For now, displays the first image. Can be enhanced to show thumbnails/carousel.
 */
@Composable
private fun ImageViewerDialog(
    images: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("商品画像") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (images.isEmpty()) {
                    Text(
                        text = "画像が登録されていません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Display first image
                    // Note: In production, use Coil or Glide to load images from URL
                    Text(
                        text = "画像URL: ${images.first()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "画像の表示にはCoilまたはGlideの実装が必要です。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    // TODO: Implement image loading with Coil
                    // AsyncImage(
                    //     model = images.first(),
                    //     contentDescription = "商品画像",
                    //     modifier = Modifier
                    //         .fillMaxWidth()
                    //         .heightIn(max = 400.dp)
                    // )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

// ========== Preview Section ==========

@Preview(
    name = "Course Header Card",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewCourseHeaderCard() {
    MaterialTheme {
        CourseHeaderCard(
            courseName = "Aコース（午前便）",
            pickingAreaName = "1F 冷凍エリア",
            registeredCount = 5,
            totalCount = 10
        )
    }
}

@Preview(
    name = "Item Information Card",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewItemInformationCard() {
    MaterialTheme {
        ItemInformationCard(
            item = PickingTaskItem(
                id = 1,
                itemId = 101,
                itemName = "サッポロ生ビール黒ラベル 500ml缶",
                janCode = "4901777123456",
                volume = "500ml",
                capacityCase = 24,
                plannedQty = 24.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = 0.0,
                status = ItemStatus.PENDING,
                packaging = "ケース",
                temperatureType = "冷凍",
                walkingOrder = 12234,
                images = emptyList(),
                slipNumber = 2023121500
            ),
            slipNumber = "20231215001"
        )
    }
}

@Preview(
    name = "Quantity Input Card",
    showBackground = true,
    widthDp = 400
)
@Composable
private fun PreviewQuantityInputCard() {
    MaterialTheme {
        QuantityInputCard(
            plannedQty = 24.0,
            quantityType = "ケース",
            pickedQtyInput = "20",
            onPickedQtyChange = {},
            isUpdating = false,
            formatQuantity = { qty, type -> String.format("%.1f %s", qty, type) }
        )
    }
}

@Preview(
    name = "Completion Dialog",
    showBackground = true
)
@Composable
private fun PreviewCompletionConfirmationDialog() {
    MaterialTheme {
        CompletionConfirmationDialog(
            isCompleting = false,
            onConfirm = {},
            onCancel = {}
        )
    }
}

@Preview(
    name = "Image Viewer Dialog - Empty",
    showBackground = true
)
@Composable
private fun PreviewImageViewerDialogEmpty() {
    MaterialTheme {
        ImageViewerDialog(
            images = emptyList(),
            onDismiss = {}
        )
    }
}

@Preview(
    name = "Image Viewer Dialog - With Image",
    showBackground = true
)
@Composable
private fun PreviewImageViewerDialogWithImage() {
    MaterialTheme {
        ImageViewerDialog(
            images = listOf("https://example.com/image.jpg"),
            onDismiss = {}
        )
    }
}

// ========== Full Screen Previews ==========

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Outbound Picking Screen - Normal State",
    showBackground = true,
    widthDp = 800,
    heightDp = 600
)
@Composable
private fun PreviewOutboundPickingScreenNormal() {
    MaterialTheme {
        val sampleTask = PickingTask(
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
                    slipNumber = 2023121500,
                    volume = "500ml",
                    capacityCase = 24,
                    janCode = "4901777123456",
                    plannedQty = 24.0,
                    plannedQtyType = QuantityType.CASE,
                    pickedQty = 0.0,
                    status = ItemStatus.PENDING,
                    packaging = "ケース",
                    temperatureType = "冷凍",
                    walkingOrder = 1000,
                    images = emptyList()
                ),
                PickingTaskItem(
                    id = 2,
                    itemId = 102,
                    itemName = "アサヒスーパードライ 350ml缶",
                    slipNumber = 2023121501,
                    volume = "350ml",
                    capacityCase = 24,
                    janCode = "4901777234567",
                    plannedQty = 12.0,
                    plannedQtyType = QuantityType.CASE,
                    pickedQty = 12.0,
                    status = ItemStatus.PICKING,
                    packaging = "ケース",
                    temperatureType = "冷凍",
                    walkingOrder = 1001,
                    images = emptyList()
                ),
                PickingTaskItem(
                    id = 3,
                    itemId = 103,
                    itemName = "キリン一番搾り 500ml缶",
                    slipNumber = 2023121502,
                    volume = "500ml",
                    capacityCase = 24,
                    janCode = "4901777345678",
                    plannedQty = 10.0,
                    plannedQtyType = QuantityType.CASE,
                    pickedQty = 10.0,
                    status = ItemStatus.COMPLETED,
                    packaging = "ケース",
                    temperatureType = "冷凍",
                    walkingOrder = 1002,
                    images = emptyList()
                )
            )
        )

        val state = OutboundPickingState(
            originalTask = sampleTask,
            pendingItems = listOf(sampleTask.items[0]), // Only PENDING items
            currentIndex = 0,
            pickedQtyInput = "",
            isLoading = false,
            warehouseId = 1
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("出庫データ入力") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る(F4)"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "ホーム(F8)"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                OutboundPickingBottomBar(
                    state = state,
                    onPrevClick = {},
                    onNextClick = {},
                    onRegisterClick = {},
                    onImageClick = {},
                    onCourseClick = {},
                    onHistoryClick = {}
                )
            }
        ) { padding ->
            OutboundPickingContent(
                state = state,
                onPickedQtyChange = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Outbound Picking Screen - With Input",
    showBackground = true,
    widthDp = 800,
    heightDp = 600
)
@Composable
private fun PreviewOutboundPickingScreenWithInput() {
    MaterialTheme {
        val sampleTask = PickingTask(
            taskId = 1,
            courseCode = "B002",
            courseName = "Bコース（午後便）",
            pickingAreaName = "2F 常温エリア",
            waveId = 112,
            pickingAreaCode = "AREA-B",
            items = listOf(
                PickingTaskItem(
                    id = 1,
                    itemId = 201,
                    itemName = "コカ・コーラ 500mlペットボトル",
                    slipNumber = 2023121600,
                    volume = "500ml",
                    capacityCase = 24,
                    janCode = "4902102123456",
                    plannedQty = 48.0,
                    plannedQtyType = QuantityType.CASE,
                    pickedQty = 0.0,
                    status = ItemStatus.PENDING,
                    packaging = "ケース",
                    temperatureType = "常温",
                    walkingOrder = 2000,
                    images = listOf("https://example.com/cola.jpg")
                )
            )
        )

        val state = OutboundPickingState(
            originalTask = sampleTask,
            pendingItems = sampleTask.items,
            currentIndex = 0,
            pickedQtyInput = "45",
            isLoading = false,
            warehouseId = 1
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("出庫データ入力") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る(F4)"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "ホーム(F8)"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                OutboundPickingBottomBar(
                    state = state,
                    onPrevClick = {},
                    onNextClick = {},
                    onRegisterClick = {},
                    onImageClick = {},
                    onCourseClick = {},
                    onHistoryClick = {}
                )
            }
        ) { padding ->
            OutboundPickingContent(
                state = state,
                onPickedQtyChange = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Outbound Picking Screen - In Progress (5/10)",
    showBackground = true,
    widthDp = 800,
    heightDp = 600
)
@Composable
private fun PreviewOutboundPickingScreenInProgress() {
    MaterialTheme {
        val items = List(10) { index ->
            PickingTaskItem(
                id = index,
                itemId = 300 + index,
                itemName = "商品 ${index + 1}",
                slipNumber = 2023121700 + index,
                volume = "1000ml",
                capacityCase = 12,
                janCode = null,
                plannedQty = 10.0,
                plannedQtyType = QuantityType.CASE,
                pickedQty = if (index < 5) 10.0 else 0.0,
                status = if (index < 5) ItemStatus.PICKING
                         else ItemStatus.PENDING,
                packaging = "ケース",
                temperatureType = "冷蔵",
                walkingOrder = 3000 + index,
                images = emptyList()
            )
        }

        val sampleTask = PickingTask(
            taskId = 1,
            courseCode = "C003",
            courseName = "Cコース（深夜便）",
            pickingAreaName = "3F 冷蔵エリア",
            waveId = 113,
            pickingAreaCode = "AREA-C",
            items = items
        )

        val pendingItems = items.filter {
            it.status == ItemStatus.PENDING
        }

        val state = OutboundPickingState(
            originalTask = sampleTask,
            pendingItems = pendingItems,
            currentIndex = 0,
            pickedQtyInput = "10",
            isLoading = false,
            warehouseId = 1
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("出庫データ入力") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る(F4)"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "ホーム(F8)"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                OutboundPickingBottomBar(
                    state = state,
                    onPrevClick = {},
                    onNextClick = {},
                    onRegisterClick = {},
                    onImageClick = {},
                    onCourseClick = {},
                    onHistoryClick = {}
                )
            }
        ) { padding ->
            OutboundPickingContent(
                state = state,
                onPickedQtyChange = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Outbound Picking Screen - Piece Type",
    showBackground = true,
    widthDp = 800,
    heightDp = 600
)
@Composable
private fun PreviewOutboundPickingScreenPieceType() {
    MaterialTheme {
        val sampleTask = PickingTask(
            taskId = 1,
            courseCode = "D004",
            courseName = "Dコース（特急便）",
            pickingAreaName = "1F 冷凍エリア",
            waveId = 114,
            pickingAreaCode = "AREA-D",
            items = listOf(
                PickingTaskItem(
                    id = 1,
                    itemId = 401,
                    itemName = "ハーゲンダッツ バニラ 120ml",
                    slipNumber = 2023121800,
                    volume = "120ml",
                    capacityCase = 24,
                    janCode = "4901234567890",
                    plannedQty = 36.0,
                    plannedQtyType = QuantityType.PIECE,
                    pickedQty = 0.0,
                    status = ItemStatus.PENDING,
                    packaging = "バラ",
                    temperatureType = "冷凍",
                    walkingOrder = 4000,
                    images = emptyList()
                )
            )
        )

        val state = OutboundPickingState(
            originalTask = sampleTask,
            pendingItems = sampleTask.items,
            currentIndex = 0,
            pickedQtyInput = "30",
            isLoading = false,
            warehouseId = 1
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("出庫データ入力") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る(F4)"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "ホーム(F8)"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                OutboundPickingBottomBar(
                    state = state,
                    onPrevClick = {},
                    onNextClick = {},
                    onRegisterClick = {},
                    onImageClick = {},
                    onCourseClick = {},
                    onHistoryClick = {}
                )
            }
        ) { padding ->
            OutboundPickingContent(
                state = state,
                onPickedQtyChange = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}
