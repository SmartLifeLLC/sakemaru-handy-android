package biz.smt_life.android.feature.outbound.picking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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

// ===== Header colors (maintained) =====
private val TitleRed     = Color(0xFFC0392B)
private val AccentOrange = Color(0xFFE67E22)
private val DividerGold  = Color(0xFFF9A825)
private val HeaderBg     = Color(0xFFFDFBF2)
private val BadgeGreen   = Color(0xFF27AE60)

// ===== Body colors (amber/neutral from HTML reference) =====
private val BodyBg       = Color(0xFFF5F5F5) // neutral-100
private val Amber50      = Color(0xFFFFFBEB)
private val Amber200     = Color(0xFFFDE68A)
private val Amber300     = Color(0xFFFCD34D)
private val Amber600     = Color(0xFFD97706)
private val Amber700     = Color(0xFFB45309)
private val Neutral50    = Color(0xFFFAFAFA)
private val Neutral100   = Color(0xFFF5F5F5)
private val Neutral200   = Color(0xFFE5E5E5)
private val Neutral300   = Color(0xFFD4D4D4)
private val Neutral400   = Color(0xFFA3A3A3)
private val Neutral500   = Color(0xFF737373)
private val Neutral600   = Color(0xFF525252)
private val Neutral700   = Color(0xFF404040)
private val ReadonlyText = Color(0xFF888888)

/**
 * Outbound Picking Screen (2.5.2 - 出庫データ入力).
 * Header is maintained from existing design.
 * Body redesigned to match HTML reference:
 * - Left pane: Product info + location/slip + order qty + ship qty + action buttons
 * - Right pane: History list with delete buttons
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

    LaunchedEffect(task.taskId) {
        viewModel.initialize(task)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    if (state.showImageDialog && state.currentItem != null) {
        ImageViewerDialog(
            images = state.currentItem!!.images,
            onDismiss = { viewModel.dismissImageDialog() }
        )
    }

    Scaffold(
        containerColor = BodyBg,
        topBar = {
            // ===== HEADER (maintained exactly as-is) =====
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
                                text = "出庫",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TitleRed
                            )
                            val courseName = state.originalTask?.courseName
                            if (!courseName.isNullOrBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    onClick = onNavigateToCourseList,
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
                                    text = "${state.registeredCount} / ${state.totalCount}",
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        // No bottomBar - buttons are now in left pane
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
                OutboundPickingBody(
                    state = state,
                    onPickedQtyChange = viewModel::onPickedQtyChange,
                    onImageClick = { viewModel.showImageDialog() },
                    onRegisterClick = viewModel::registerCurrentItem,
                    onHistoryClick = onNavigateToHistory,
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
                    OutlinedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Neutral200)
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
                            if (state.originalTask?.isFullyProcessed == true) {
                                // 作業済（全アイテムCOMPLETED/SHORTAGE）
                                Text(
                                    text = "作業が完了しました。",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF212529)
                                )
                            } else {
                                // 全商品登録完了（確定前）
                                Text(
                                    text = "すべての商品が登録されました。",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF212529)
                                )
                                Text(
                                    text = "確定を押下してください。",
                                    fontSize = 14.sp,
                                    color = Neutral500
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onNavigateToHistory,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .height(40.dp)
                                            .widthIn(min = 120.dp),
                                        border = BorderStroke(1.dp, Neutral300)
                                    ) {
                                        Text(
                                            text = "キャンセル",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Neutral700
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.completeTask(onSuccess = onTaskCompleted) },
                                        enabled = !state.isCompleting,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .height(40.dp)
                                            .widthIn(min = 120.dp)
                                    ) {
                                        if (state.isCompleting) {
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
            }
        }
    }
}

// ===== NEW BODY LAYOUT =====

@Composable
private fun OutboundPickingBody(
    state: OutboundPickingState,
    onPickedQtyChange: (String) -> Unit,
    onImageClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentItem = state.currentItem!!
    val originalTask = state.originalTask!!

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ===== LEFT PANE: Product info + Qty + Buttons =====
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, Neutral200)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // (a) Product info header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentItem.itemName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = androidx.compose.ui.text.TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                        val specLine = buildString {
                            if (currentItem.janCode != null) append(currentItem.janCode)
                            if (currentItem.volume != null) {
                                if (isNotEmpty()) append(" / ")
                                append(currentItem.volume)
                            }
                            if (currentItem.capacityCase != null) {
                                if (isNotEmpty()) append(" / ")
                                append("入数:${currentItem.capacityCase}")
                            }
                        }
                        if (specLine.isNotEmpty()) {
                            Text(
                                text = specLine,
                                fontSize = 16.sp,
                                color = Neutral500,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Text(
                            text = "得意先名: ${currentItem.customerName ?: ""}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Neutral500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    // Image button
                    Surface(
                        modifier = Modifier
                            .width(44.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Amber50,
                        border = BorderStroke(1.dp, Amber200),
                        onClick = { onImageClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = "商品画像",
                                tint = if (state.hasImages) Amber600 else Neutral400,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // (b) Location & Slip number (2 rows)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ロケーション",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Neutral500,
                        modifier = Modifier.width(100.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .background(Amber50, RoundedCornerShape(6.dp))
                            .border(1.dp, Amber300, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = originalTask.pickingAreaName.ifBlank { "未設定" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (originalTask.pickingAreaName.isBlank()) Neutral400 else Color.Black
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "伝票番号",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Neutral500,
                        modifier = Modifier.width(100.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .background(Amber50, RoundedCornerShape(6.dp))
                            .border(1.dp, Amber300, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = currentItem.slipNumber.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

            }
        }

        // ===== RIGHT PANE: Order qty + Ship qty + Buttons =====
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, Neutral200)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // (c) ケース & バラ with 受注数 inline (2 columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val caseQty = if (state.quantityTypeLabel == "ケース")
                        String.format("%.0f", currentItem.plannedQty) else "0"
                    val pieceQty = if (state.quantityTypeLabel == "バラ")
                        String.format("%.0f", currentItem.plannedQty) else "0"

                    // ケース column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ケース（受注数：$caseQty）",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Neutral500,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = if (state.quantityTypeLabel == "ケース")
                                state.pickedQtyInput.removeSuffix(".0") else "",
                            onValueChange = onPickedQtyChange,
                            enabled = !state.isUpdating,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Amber600,
                                unfocusedBorderColor = Neutral300
                            )
                        )
                    }

                    // バラ column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "バラ（受注数：$pieceQty）",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Neutral500,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = if (state.quantityTypeLabel == "バラ")
                                state.pickedQtyInput.removeSuffix(".0") else "",
                            onValueChange = onPickedQtyChange,
                            enabled = !state.isUpdating,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Amber600,
                                unfocusedBorderColor = Neutral300
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // (e) Action buttons (登録 / 履歴)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 登録/確定
                    Button(
                        onClick = onRegisterClick,
                        enabled = state.canRegister,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber600,
                            contentColor = Color.White
                        )
                    ) {
                        if (state.isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "登録",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // 履歴
                    Button(
                        onClick = onHistoryClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber50,
                            contentColor = Amber700
                        ),
                        border = BorderStroke(1.dp, Amber300)
                    ) {
                        Text("履歴", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ===== Dialogs (maintained) =====

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
                                            .background(Neutral100),
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
                                            color = if (isSelected) AccentOrange else Neutral300,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

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

// ========== Previews ==========

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "P21 - HTML Design Body",
    showBackground = true,
    widthDp = 800,
    heightDp = 600
)
@Composable
private fun PreviewOutboundPickingBody() {
    val items = List(10) { index ->
        PickingTaskItem(
            id = index,
            itemId = 300 + index,
            itemName = if (index == 5) "サッポロ生ビール黒ラベル 500ml缶" else "商品 ${index + 1}",
            slipNumber = 2023121700 + index,
            volume = "1000ml",
            capacityCase = 12,
            janCode = if (index % 2 == 0) "490177712345${index}" else null,
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

    MaterialTheme {
        Scaffold(
            containerColor = BodyBg,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Inventory2, null, tint = AccentOrange, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("出庫", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TitleRed)
                                Spacer(Modifier.width(8.dp))
                                Surface(color = BadgeGreen, shape = RoundedCornerShape(20.dp)) {
                                    Text("Cコース（深夜便）", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Surface(color = AccentOrange, shape = RoundedCornerShape(12.dp)) {
                                    Text("5 / 10", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {}) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る", tint = TitleRed)
                            }
                        },
                        actions = {
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Home, "ホーム", tint = TitleRed)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderBg)
                    )
                    HorizontalDivider(thickness = 2.dp, color = DividerGold)
                }
            }
        ) { padding ->
            OutboundPickingBody(
                state = state,
                onPickedQtyChange = {},
                onImageClick = {},
                onRegisterClick = {},
                onHistoryClick = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}
