package biz.smt_life.android.feature.outbound.picking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import biz.smt_life.android.core.domain.model.ItemStatus
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType

// ===== P20/P21 共通カラー =====
private val TitleRed     = Color(0xFFC0392B)
private val AccentOrange = Color(0xFFE67E22)
private val DividerGold  = Color(0xFFF9A825)
private val BodyBg       = Color.White
private val HeaderBg     = Color(0xFFFDFBF2)
private val TextPrimary  = Color(0xFF212529)
private val TextSecond   = Color(0xFF555555)
private val BorderGray   = Color(0xFFCCCCCC)
private val ReadonlyBg   = Color(0xFFF5F5F5)
private val ReadonlyText = Color(0xFF888888)
private val BadgeGreen   = Color(0xFF27AE60)

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
                                text = "出庫",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TitleRed
                            )
                            val courseName = state.originalTask?.courseName
                            if (!courseName.isNullOrBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = BadgeGreen,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = courseName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = AccentOrange,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${state.registeredCount} / ${state.totalCount}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
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
                                contentDescription = "戻る(F4)",
                                tint = TitleRed
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToMain) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "ホーム(F8)",
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
            state.currentItem != null && state.originalTask != null -> {
                OutboundPickingContent(
                    state = state,
                    onPickedQtyChange = viewModel::onPickedQtyChange,
                    onImageClick = { viewModel.showImageDialog() },
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
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentItem = state.currentItem!!
    val originalTask = state.originalTask!!

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(BodyBg),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // LEFT PANE: Item info (read-only)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            ItemInformationCard(
                item = currentItem,
                slipNumber = currentItem.slipNumber.toString(),
                pickingAreaName = originalTask.pickingAreaName,
                onImageClick = onImageClick,
                modifier = Modifier.fillMaxHeight()
            )
        }

        // RIGHT PANE: Quantity Input (editable)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            QuantityInputCard(
                plannedQty = currentItem.plannedQty,
                quantityType = state.quantityTypeLabel,
                pickedQtyInput = state.pickedQtyInput,
                onPickedQtyChange = onPickedQtyChange,
                isUpdating = state.isUpdating,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ItemInformationCard(
    item: PickingTaskItem,
    slipNumber: String,
    pickingAreaName: String,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasImages = item.images.isNotEmpty()

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, BorderGray),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.itemName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { if (hasImages) onImageClick() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = "商品画像",
                        tint = if (hasImages) AccentOrange else Color(0xFFCCCCCC),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            HorizontalDivider(
                color = Color(0xFFEEEEEE),
                modifier = Modifier.padding(vertical = 2.dp)
            )
            InfoRow(label = "伝票番号", value = slipNumber)
            if (item.volume != null) {
                InfoRow(label = "容量", value = item.volume!!)
            } else {
                InfoRow(label = "容量", value = "—")
            }
            if (item.capacityCase != null) {
                InfoRow(label = "入数", value = "${item.capacityCase} 個/ケース")
            } else {
                InfoRow(label = "入数", value = "—")
            }
            if (item.janCode != null) {
                InfoRow(label = "JAN", value = item.janCode!!)
            } else {
                InfoRow(label = "JAN", value = "—")
            }
            InfoRow(label = "ロケ", value = pickingAreaName.ifBlank { "—" })
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
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, AccentOrange),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "数量",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TitleRed
            )

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // ラベル行（同じ高さに固定）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "出荷数量（受注）",
                    fontSize = 12.sp,
                    color = TextSecond,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "出庫数量",
                    fontSize = 12.sp,
                    color = AccentOrange,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }

            // ボックス行（完全同一サイズ）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 左: 出荷数量（読み取り専用）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(ReadonlyBg, RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%.1f %s", plannedQty, quantityType),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReadonlyText
                    )
                }

                // 右: 出庫数量（入力）
                OutlinedTextField(
                    value = pickedQtyInput,
                    onValueChange = onPickedQtyChange,
                    placeholder = { Text("0", fontSize = 16.sp) },
                    enabled = !isUpdating,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange,
                        unfocusedBorderColor = BorderGray
                    )
                )
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val buttonModifier = Modifier.weight(1f)

                // 画像(F5)
                OutlinedButton(
                    onClick = onImageClick,
                    enabled = state.hasImages && !state.isUpdating,
                    shape = smallShape,
                    contentPadding = contentPadding,
                    modifier = buttonModifier
                ) {
                    Text("商品の画像", style = smallTextStyle)
                }

                // コース変更(F6)
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
                        textAlign = TextAlign.Center
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
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    modifier = buttonModifier
                ) {
                    if (state.isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
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
                enabled = !isCompleting,
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                if (isCompleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
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
 * Displays product images with HorizontalPager for multi-image support.
 * Uses Coil for async image loading with loading/error states.
 */
@Composable
private fun ImageViewerDialog(
    images: List<String>,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { images.size.coerceAtLeast(1) })

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column {
                // ===== ヘッダー =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HeaderBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "商品画像",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TitleRed
                    )
                    if (images.size > 1) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = AccentOrange,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${images.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "閉じる",
                            tint = TitleRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = DividerGold, thickness = 2.dp)

                // ===== 画像エリア =====
                if (images.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = null,
                                tint = Color(0xFFCCCCCC),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "画像が登録されていません",
                                fontSize = 14.sp,
                                color = ReadonlyText
                            )
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) { page ->
                        SubcomposeAsyncImage(
                            model = images[page],
                            contentDescription = "商品画像 ${page + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        ) {
                            when (painter.state) {
                                is AsyncImagePainter.State.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = AccentOrange,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                                is AsyncImagePainter.State.Error -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(ReadonlyBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Image,
                                                contentDescription = null,
                                                tint = Color(0xFFCCCCCC),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Text(
                                                text = "画像を読み込めません",
                                                fontSize = 13.sp,
                                                color = ReadonlyText
                                            )
                                        }
                                    }
                                }
                                else -> SubcomposeAsyncImageContent()
                            }
                        }
                    }

                    // ページドット（複数画像の場合）
                    if (images.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(images.size) { index ->
                                val isSelected = index == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(if (isSelected) 10.dp else 7.dp)
                                        .background(
                                            color = if (isSelected) AccentOrange else BorderGray,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

                // ===== フッター =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("閉じる", color = AccentOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ========== Preview Section ==========

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
            slipNumber = "20231215001",
            pickingAreaName = "1F 冷凍エリア",
            onImageClick = {}
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
            isUpdating = false
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
                )
            )
        )

        val state = OutboundPickingState(
            originalTask = sampleTask,
            pendingItems = listOf(sampleTask.items[0]),
            currentIndex = 0,
            pickedQtyInput = "",
            isLoading = false,
            warehouseId = 1
        )

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
                                    text = "出庫",
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
                                    contentDescription = "戻る(F4)",
                                    tint = TitleRed
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "ホーム(F8)",
                                    tint = TitleRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderBg)
                    )
                    HorizontalDivider(thickness = 2.dp, color = DividerGold)
                }
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
                onImageClick = {},
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
                                    text = "出庫",
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
                                    contentDescription = "戻る(F4)",
                                    tint = TitleRed
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "ホーム(F8)",
                                    tint = TitleRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderBg)
                    )
                    HorizontalDivider(thickness = 2.dp, color = DividerGold)
                }
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
                onImageClick = {},
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
                status = if (index < 5) ItemStatus.PICKING else ItemStatus.PENDING,
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

        val pendingItems = items.filter { it.status == ItemStatus.PENDING }

        val state = OutboundPickingState(
            originalTask = sampleTask,
            pendingItems = pendingItems,
            currentIndex = 0,
            pickedQtyInput = "10",
            isLoading = false,
            warehouseId = 1
        )

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
                                    text = "出庫",
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
                                    contentDescription = "戻る(F4)",
                                    tint = TitleRed
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "ホーム(F8)",
                                    tint = TitleRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderBg)
                    )
                    HorizontalDivider(thickness = 2.dp, color = DividerGold)
                }
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
                onImageClick = {},
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
                                    text = "出庫",
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
                                    contentDescription = "戻る(F4)",
                                    tint = TitleRed
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "ホーム(F8)",
                                    tint = TitleRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderBg)
                    )
                    HorizontalDivider(thickness = 2.dp, color = DividerGold)
                }
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
                onImageClick = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}
