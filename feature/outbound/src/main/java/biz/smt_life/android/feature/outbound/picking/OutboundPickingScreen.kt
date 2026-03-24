package biz.smt_life.android.feature.outbound.picking

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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

// ===== Orientation persistence =====
private const val PREF_NAME_P21 = "p21_orientation_prefs"
private const val PREF_KEY_IS_PORTRAIT = "p21_is_portrait"

/**
 * New 3-Tier Header for P21.
 * Supports Portrait and Landscape variations based on the specification.
 */
@Composable
private fun OutboundPickingHeader(
    state: OutboundPickingState,
    isPortrait: Boolean,
    onNavigateBack: () -> Unit,
    toggleOrientation: () -> Unit,
    moveToPrevGroup: () -> Unit,
    moveToNextGroup: () -> Unit
) {
    val elapsedTimeSeconds = state.elapsedTimeSeconds
    val mm = (elapsedTimeSeconds / 60).toString().padStart(2, '0')
    val ss = (elapsedTimeSeconds % 60).toString().padStart(2, '0')
    val timeText = "$mm:$ss"

    val currentPage = state.currentGroupIndex + 1
    val totalPages = state.totalGroupCount
    // 進捗（％）は完了率を表す
    val progress = if (totalPages > 0) state.registeredGroupCount.toFloat() / totalPages.toFloat() else 0f

    val progressColor = Color(0xFFDD833A)
    val bgColor = Color(0xFFE5E5E5) // Neutral200

    val strokeStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        drawStyle = Stroke(
            miter = 10f,
            width = 4f,
            join = StrokeJoin.Round
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TIER 1
        if (isPortrait) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNavigateBack, contentPadding = PaddingValues(0.dp)) {
                    Text("配送コース", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleRed)
                }

                TextButton(onClick = toggleOrientation, contentPadding = PaddingValues(0.dp)) {
                    Text("画面回転", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onNavigateBack, contentPadding = PaddingValues(0.dp)) {
                        Text("もどる", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleRed)
                    }
                    Text(" | ", fontSize = 16.sp, color = Color.Gray)
                    TextButton(onClick = toggleOrientation, contentPadding = PaddingValues(0.dp)) {
                        Text("画面回転", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Timer", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                    Text(timeText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }

                Text("${state.registeredGroupCount}/$totalPages 完了", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.DarkGray)
            }
        }

        // TIER 2 (Portrait Only)
        if (isPortrait) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(bgColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .background(progressColor, RoundedCornerShape(12.dp))
                    )

                    // Text overlay
                    val overlayText = "$currentPage/$totalPages"
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = overlayText,
                            style = strokeStyle
                        )
                        Text(
                            text = overlayText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Timer", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                    Text(timeText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }
            }
        }

        // TIER 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF37474F), RoundedCornerShape(6.dp))
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = moveToPrevGroup, enabled = currentPage > 1, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "前へ",
                    tint = if (currentPage > 1) Color.White else Color.Gray
                )
            }
            Text(
                "作業番号 $currentPage/$totalPages",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = moveToNextGroup, enabled = currentPage < totalPages, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "次へ",
                    tint = if (currentPage < totalPages) Color.White else Color.Gray
                )
            }
        }
    }
}

/**
 * Outbound Picking Screen (2.5.2 - 出庫データ入力).
 * Header is maintained from existing design.
 * Body redesigned to match HTML reference:
 * - Left pane: Product info + location/slip + order qty + ship qty + action buttons
 * - Right pane: History list with delete buttons
 * Supports Portrait/Landscape toggle via TopAppBar button.
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
    editItemId: Int? = null,
    viewModel: OutboundPickingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ===== Orientation control =====
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences(PREF_NAME_P21, Context.MODE_PRIVATE) }
    var isPortrait by remember { mutableStateOf(prefs.getBoolean(PREF_KEY_IS_PORTRAIT, false)) }

    // Apply orientation
    LaunchedEffect(isPortrait) {
        activity?.requestedOrientation = if (isPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    fun toggleOrientation() {
        isPortrait = !isPortrait
        prefs.edit().putBoolean(PREF_KEY_IS_PORTRAIT, isPortrait).apply()
    }

    LaunchedEffect(task.taskId, editItemId) {
        viewModel.initialize(task, editItemId)
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

    if (state.showImageDialog && state.currentGroup != null) {
        ImageViewerDialog(
            images = state.currentGroup!!.images,
            onDismiss = { viewModel.dismissImageDialog() }
        )
    }

    if (state.showJanScannerDialog && state.currentGroup != null) {
        JanCodeScannerDialog(
            expectedJanCode = state.currentGroup!!.janCode,
            isInCamera = state.isJanScannerInCamera,
            onResult = { code, match -> viewModel.onJanScanResult(code, match) },
            onDismiss = { viewModel.dismissJanScannerDialog() }
        )
    }

    Scaffold(
        containerColor = BodyBg,
        topBar = {
            if (!state.isLoading && state.originalTask != null && state.currentGroup != null) {
                OutboundPickingHeader(
                    state = state,
                    isPortrait = isPortrait,
                    onNavigateBack = onNavigateBack,
                    toggleOrientation = { toggleOrientation() },
                    moveToPrevGroup = { viewModel.moveToPrevGroup() },
                    moveToNextGroup = { viewModel.moveToNextGroup() }
                )
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
            state.currentGroup != null && state.originalTask != null -> {
                var offsetX by remember { mutableStateOf(0f) }
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX > 60f && state.canMovePrev) {
                                    viewModel.moveToPrevGroup()
                                } else if (offsetX < -60f && state.canMoveNext) {
                                    viewModel.moveToNextGroup()
                                }
                                offsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount
                            }
                        )
                    }
                ) {
                    OutboundPickingBody(
                        state = state,
                        isPortrait = isPortrait,
                        onTotalCaseInputChange = viewModel::onTotalCaseInputChange,
                        onTotalPieceInputChange = viewModel::onTotalPieceInputChange,
                        onCustomerCaseQtyChange = viewModel::onCustomerCaseQtyChange,
                        onCustomerPieceQtyChange = viewModel::onCustomerPieceQtyChange,
                        onImageClick = { viewModel.showImageDialog() },
                        onJanScanClick = { isInCamera -> viewModel.showJanScannerDialog(isInCamera) },
                        onRegisterClick = {
                            performHapticFeedback(context)
                            viewModel.registerGroupedItem()
                        },
                        onHistoryClick = onNavigateToHistory,
                        moveToPrevGroup = viewModel::moveToPrevGroup,
                        moveToNextGroup = viewModel::moveToNextGroup,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
                                // 全商品登録完了
                                Text(
                                    text = "すべての商品が登録されました。",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF212529)
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.completeTask(onSuccess = onTaskCompleted) },
                                    enabled = !state.isCompleting,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                ) {
                                    if (state.isCompleting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Text(
                                            text = "完了",
                                            fontSize = 20.sp,
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

// ===== NEW BODY LAYOUT (Grouped) =====

@Composable
private fun OutboundPickingBody(
    state: OutboundPickingState,
    isPortrait: Boolean,
    onTotalCaseInputChange: (String) -> Unit,
    onTotalPieceInputChange: (String) -> Unit,
    onCustomerCaseQtyChange: (Int, String) -> Unit,
    onCustomerPieceQtyChange: (Int, String) -> Unit,
    onImageClick: () -> Unit,
    onJanScanClick: (Boolean) -> Unit,
    onRegisterClick: () -> Unit,
    onHistoryClick: () -> Unit,
    moveToPrevGroup: () -> Unit,
    moveToNextGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val group = state.currentGroup!!
    val isEditable = state.isEditable

    if (isPortrait) {
        Column(
            modifier = modifier.fillMaxSize().padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            AnimatedContent(
                targetState = state.currentGroupIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }.using(SizeTransform(clip = false))
                },
                label = "SwipeAnimationPortrait"
            ) { targetIndex ->
                val targetGroup = state.groupedItems.getOrNull(targetIndex) ?: state.currentGroup!!
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(0.35f).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), 
                        color = if (state.isCurrentGroupRegistered) Color(0xFFEEEEEE) else Color.White,
                        shadowElevation = 1.dp, border = BorderStroke(1.dp, Neutral200)
                    ) {
                        Column {
                            if (!isEditable) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFFFFF3E0)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Lock, null, tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("読取専用: 欠品処理中または出荷済みのため変更できません", fontSize = 11.sp, color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            ProductInfoSection(group = targetGroup, hasImages = state.hasImages, onImageClick = onImageClick, onJanScanClick = onJanScanClick, janScanResult = state.currentJanScanResult, isPortrait = true)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(0.65f).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), 
                        color = if (state.isCurrentGroupRegistered) Color(0xFFEEEEEE) else Color.White,
                        shadowElevation = 1.dp, border = BorderStroke(1.dp, Neutral200)
                    ) {
                        GroupedQuantitySection(
                            state = state,
                            onTotalCaseInputChange = onTotalCaseInputChange,
                            onTotalPieceInputChange = onTotalPieceInputChange,
                            onCustomerCaseQtyChange = onCustomerCaseQtyChange,
                            onCustomerPieceQtyChange = onCustomerPieceQtyChange,
                            onRegisterClick = onRegisterClick,
                            onHistoryClick = onHistoryClick
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize().padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            AnimatedContent(
                targetState = state.currentGroupIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }.using(SizeTransform(clip = false))
                },
                label = "SwipeAnimationLandscape"
            ) { targetIndex ->
                val targetGroup = state.groupedItems.getOrNull(targetIndex) ?: state.currentGroup!!
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(12.dp), 
                        color = if (state.isCurrentGroupRegistered) Color(0xFFEEEEEE) else Color.White,
                        shadowElevation = 1.dp, border = BorderStroke(1.dp, Neutral200)
                    ) {
                        Column {
                            if (!isEditable) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFFFFF3E0)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Filled.Lock, null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("読取専用（編集不可）", fontSize = 14.sp, color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            ProductInfoSection(group = targetGroup, hasImages = state.hasImages, onImageClick = onImageClick, onJanScanClick = onJanScanClick, janScanResult = state.currentJanScanResult, isPortrait = false)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = RoundedCornerShape(12.dp), 
                        color = if (state.isCurrentGroupRegistered) Color(0xFFEEEEEE) else Color.White,
                        shadowElevation = 1.dp, border = BorderStroke(1.dp, Neutral200)
                    ) {
                        GroupedQuantitySection(
                            state = state,
                            onTotalCaseInputChange = onTotalCaseInputChange,
                            onTotalPieceInputChange = onTotalPieceInputChange,
                            onCustomerCaseQtyChange = onCustomerCaseQtyChange,
                            onCustomerPieceQtyChange = onCustomerPieceQtyChange,
                            onRegisterClick = onRegisterClick,
                            onHistoryClick = onHistoryClick
                        )
                    }
                }
            }
        }
    }
}

// ===== Extracted Components =====

@Composable
private fun ProductInfoSection(
    group: GroupedPickingItem,
    hasImages: Boolean,
    onImageClick: () -> Unit,
    onJanScanClick: (Boolean) -> Unit,
    janScanResult: JanScanResult?,
    isPortrait: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 商品名（2行固定、超えたら文字縮小）
        var itemNameFontSize by remember(group.itemId) { mutableStateOf(18.sp) }
        Text(
            text = group.itemName,
            fontSize = itemNameFontSize,
            fontWeight = FontWeight.ExtraBold,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (result.hasVisualOverflow && itemNameFontSize > 12.sp) {
                    itemNameFontSize = itemNameFontSize * 0.9f
                }
            },
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )

        // JANコード（大きめ表示）+ 一致/不一致バッジ
        if (!group.janCode.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = group.janCode, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Neutral500)
                if (janScanResult != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if (janScanResult.isMatch) Color(0xFF27AE60) else Color(0xFFE74C3C),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (janScanResult.isMatch) "JAN一致" else "不一致",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // 容量・入数 & ロケーション
        val specLine = buildString {
            if (group.volume != null) append(group.volume)
            if (group.capacityCase != null) {
                if (isNotEmpty()) append(" / ")
                append("入数:${group.capacityCase}")
            }
        }

        if (isPortrait) {
            if (specLine.isNotEmpty()) {
                Text(text = specLine, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Neutral500)
            }

            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Amber50, RoundedCornerShape(6.dp))
                    .border(1.dp, Amber300, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = group.locationCode?.ifBlank { null } ?: "未設定",
                    fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    color = if (group.locationCode.isNullOrBlank()) Neutral400 else Color.Black
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (specLine.isNotEmpty()) {
                        Text(text = specLine, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Neutral500)
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                        .background(Amber50, RoundedCornerShape(6.dp))
                        .border(1.dp, Amber300, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = group.locationCode?.ifBlank { null } ?: "未設定",
                        fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = if (group.locationCode.isNullOrBlank()) Neutral400 else Color.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }

        // 画像確認 | JAN(IN) | JAN(OUT) ボタン（横並び）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1.2f).height(44.dp),
                shape = RoundedCornerShape(8.dp), color = Amber50,
                border = BorderStroke(1.dp, Amber200),
                onClick = { onImageClick() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Image, "商品画像", tint = if (hasImages) Amber600 else Neutral400, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("商品画像", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (hasImages) Amber600 else Neutral400)
                }
            }
            Surface(
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(8.dp), color = Color(0xFFE3F2FD),
                border = BorderStroke(1.dp, Color(0xFF64B5F6)),
                onClick = { onJanScanClick(true) }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.CameraAlt, "JAN(内)", tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("JAN(内)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                }
            }
            Surface(
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(8.dp), color = Color(0xFFF3E5F5),
                border = BorderStroke(1.dp, Color(0xFFBA68C8)),
                onClick = { onJanScanClick(false) }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.CameraAlt, "JAN(外)", tint = Color(0xFF7B1FA2), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("JAN(外)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                }
            }
        }
    }
}

@Composable
private fun GroupedQuantitySection(
    state: OutboundPickingState,
    onTotalCaseInputChange: (String) -> Unit,
    onTotalPieceInputChange: (String) -> Unit,
    onCustomerCaseQtyChange: (Int, String) -> Unit,
    onCustomerPieceQtyChange: (Int, String) -> Unit,
    onRegisterClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // === テーブルヘッダー: 区分 | ケース | バラ ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (state.isCurrentGroupRegistered) Color(0xFFE0E0E0) else Amber50, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .border(1.dp, if (state.isCurrentGroupRegistered) Color(0xFFBDBDBD) else Amber300, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("区分", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Neutral500,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("ケース", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Neutral500,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("バラ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Neutral500,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }

        // === 合計行 ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = if (state.isCurrentGroupRegistered) Color(0xFFBDBDBD) else Amber300)
                .background(if (state.isCurrentGroupRegistered) Color(0xFFF5F5F5) else Color(0xFFFFF8E1))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("合計", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TitleRed,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            // ケース合計入力
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    val isCaseError = (state.totalCaseInput.toDoubleOrNull() ?: 0.0) > state.totalCasePlanned
                    CompactNumberInput(
                        value = state.totalCaseInput,
                        onValueChange = onTotalCaseInputChange,
                        enabled = !state.isUpdating && state.isEditable,
                        isError = isCaseError,
                        modifier = Modifier.width(56.dp).height(38.dp)
                    )
                    Text("/${String.format("%.0f", state.totalCasePlanned)}", fontSize = 18.sp, color = if (isCaseError) Color.Red else Neutral500)
                }
            }
            // バラ合計入力
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    val isPieceError = (state.totalPieceInput.toDoubleOrNull() ?: 0.0) > state.totalPiecePlanned
                    CompactNumberInput(
                        value = state.totalPieceInput,
                        onValueChange = onTotalPieceInputChange,
                        enabled = !state.isUpdating && state.isEditable,
                        isError = isPieceError,
                        modifier = Modifier.width(56.dp).height(38.dp)
                    )
                    Text("/${String.format("%.0f", state.totalPiecePlanned)}", fontSize = 18.sp, color = if (isPieceError) Color.Red else Neutral500)
                }
            }
        }

        HorizontalDivider(color = Amber300, thickness = 2.dp)

        var isExpanded by remember { mutableStateOf(false) }

        // === 得意先別出荷数内訳ラベル（折りたたみトリガー） ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "得意先別出荷数内訳",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Neutral500,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "閉じる" else "開く",
                tint = Neutral500
            )
        }

        // === 得意先別リスト（スクロール可能） ===
        AnimatedVisibility(visible = isExpanded) {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                itemsIndexed(state.currentGroup?.customerEntries ?: emptyList()) { index, entry ->
                    CustomerEntryRow(
                        entry = entry,
                        onCaseQtyChange = { onCustomerCaseQtyChange(index, it) },
                        onPieceQtyChange = { onCustomerPieceQtyChange(index, it) },
                        isUpdating = state.isUpdating || !state.isEditable
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (state.quantityErrorMessage != null) {
            Text(
                text = state.quantityErrorMessage,
                color = TitleRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp)
            )
        }

        // === 登録・履歴ボタン ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRegisterClick,
                enabled = state.canRegister,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isCurrentGroupRegistered) Color(0xFF9E9E9E) else Amber600, 
                    contentColor = Color.White
                )
            ) {
                if (state.isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(if (state.isCurrentGroupRegistered) "登録済み" else "登録", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber50, contentColor = Amber700),
                border = BorderStroke(1.dp, Amber300)
            ) {
                Text("履歴", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CustomerEntryRow(
    entry: CustomerEntry,
    onCaseQtyChange: (String) -> Unit,
    onPieceQtyChange: (String) -> Unit,
    isUpdating: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = Neutral200)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 得意先名（ヘッダーの「区分」列と同じ weight）
        Text(
            text = entry.customerCode.ifBlank { entry.customerName.ifBlank { "—" } },
            modifier = Modifier.weight(1f),
            fontSize = 18.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        // ケース列
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (entry.caseEntry != null) {
                val isCaseError = (entry.caseEntry.pickedQtyInput.toDoubleOrNull() ?: 0.0) > entry.caseEntry.plannedQty
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CompactNumberInput(
                        value = entry.caseEntry.pickedQtyInput,
                        onValueChange = onCaseQtyChange,
                        enabled = !isUpdating,
                        isError = isCaseError,
                        modifier = Modifier.width(56.dp).height(36.dp)
                    )
                    Text("/${String.format("%.0f", entry.caseEntry.plannedQty)}", fontSize = 18.sp, color = if (isCaseError) Color.Red else Neutral500)
                }
            } else {
                Text("—", fontSize = 13.sp, color = Neutral300)
            }
        }
        // バラ列
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (entry.pieceEntry != null) {
                val isPieceError = (entry.pieceEntry.pickedQtyInput.toDoubleOrNull() ?: 0.0) > entry.pieceEntry.plannedQty
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CompactNumberInput(
                        value = entry.pieceEntry.pickedQtyInput,
                        onValueChange = onPieceQtyChange,
                        enabled = !isUpdating,
                        isError = isPieceError,
                        modifier = Modifier.width(56.dp).height(36.dp)
                    )
                    Text("/${String.format("%.0f", entry.pieceEntry.plannedQty)}", fontSize = 18.sp, color = if (isPieceError) Color.Red else Neutral500)
                }
            } else {
                Text("—", fontSize = 13.sp, color = Neutral300)
            }
        }
    }
}

// ===== Compact Number Input (minimal padding) =====

@Composable
private fun CompactNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    // 外部からvalueが変わった場合に同期
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }
    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            onValueChange(newValue.text)
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Color.Black
        ),
        modifier = modifier
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    // フォーカス時に全選択
                    textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length))
                }
            },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isError) Color(0xFFFFEBEE) else Color.White, RoundedCornerShape(4.dp))
                    .border(
                        width = if (isFocused || isError) 2.dp else 1.dp,
                        color = if (isError) Color.Red else if (isFocused) Amber600 else Neutral300,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
        }
    )
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
        text = { Text("すべての商品登録を完了しますか？") },
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
                    Text("完了")
                }
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

private fun previewGroupedItems(): List<GroupedPickingItem> = listOf(
    GroupedPickingItem(
        itemId = 300,
        itemName = "サッポロ生ビール黒ラベル 500ml缶",
        janCode = "4901773000001",
        volume = "500ml",
        capacityCase = 24,
        locationCode = "A-01-03",
        images = emptyList(),
        walkingOrder = 3000,
        customerEntries = listOf(
            CustomerEntry(
                caseEntry = CustomerEntryDetail(pickingItemResultId = 1, plannedQty = 5.0, pickedQtyInput = "5"),
                pieceEntry = null,
                customerName = "居酒屋A",
                customerCode = "C001",
                slipNumbers = listOf(2023121700)
            ),
            CustomerEntry(
                caseEntry = CustomerEntryDetail(pickingItemResultId = 2, plannedQty = 5.0, pickedQtyInput = "5"),
                pieceEntry = CustomerEntryDetail(pickingItemResultId = 3, plannedQty = 3.0, pickedQtyInput = "3"),
                customerName = "レストランB",
                customerCode = "C002",
                slipNumbers = listOf(2023121701, 2023121702)
            ),
            CustomerEntry(
                caseEntry = null,
                pieceEntry = CustomerEntryDetail(pickingItemResultId = 4, plannedQty = 5.0, pickedQtyInput = "5"),
                customerName = "ホテルC",
                customerCode = "C003",
                slipNumbers = listOf(2023121703)
            )
        )
    )
)

private fun previewSampleTask(): PickingTask {
    val items = List(10) { index ->
        PickingTaskItem(
            id = index,
            itemId = 300 + (index / 3),
            itemName = "商品 ${index + 1}",
            slipNumber = 2023121700 + index,
            volume = "500ml",
            capacityCase = 24,
            janCode = "490177300000${index}",
            plannedQty = 5.0,
            plannedQtyType = if (index % 2 == 0) QuantityType.CASE else QuantityType.PIECE,
            pickedQty = if (index < 3) 5.0 else 0.0,
            status = if (index < 3) ItemStatus.PICKING else ItemStatus.PENDING,
            packaging = "ケース",
            temperatureType = "冷蔵",
            walkingOrder = 3000 + index,
            images = emptyList()
        )
    }
    return PickingTask(
        taskId = 1,
        courseCode = "C003",
        courseName = "Cコース（深夜便）",
        pickingAreaName = "3F 冷蔵エリア",
        waveId = 113,
        pickingAreaCode = "AREA-C",
        items = items
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "P21 - Grouped Landscape",
    showBackground = true,
    widthDp = 800,
    heightDp = 600
)
@Composable
private fun PreviewOutboundPickingBody() {
    val state = OutboundPickingState(
        originalTask = previewSampleTask(),
        groupedItems = previewGroupedItems(),
        currentGroupIndex = 0,
        totalCaseInput = "10",
        totalPieceInput = "8",
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
                                    Text("${state.registeredGroupCount} / ${state.totalGroupCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
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
                isPortrait = false,
                onTotalCaseInputChange = {},
                onTotalPieceInputChange = {},
                onCustomerCaseQtyChange = { _, _ -> },
                onCustomerPieceQtyChange = { _, _ -> },
                onImageClick = {},
                onJanScanClick = {},
                onRegisterClick = {},
                onHistoryClick = {},
                moveToPrevGroup = {},
                moveToNextGroup = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}

private fun performHapticFeedback(context: android.content.Context) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {
        // Vibration not available
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "P21 - Grouped Portrait",
    showBackground = true,
    widthDp = 420,
    heightDp = 800
)
@Composable
private fun PreviewOutboundPickingBodyPortrait() {
    val state = OutboundPickingState(
        originalTask = previewSampleTask(),
        groupedItems = previewGroupedItems(),
        currentGroupIndex = 0,
        totalCaseInput = "10",
        totalPieceInput = "8",
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
                                    Text("${state.registeredGroupCount} / ${state.totalGroupCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {}) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る", tint = TitleRed)
                            }
                        },
                        actions = {
                            IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Refresh, "画面回転", tint = AccentOrange, modifier = Modifier.size(26.dp))
                            }
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
                isPortrait = true,
                onTotalCaseInputChange = {},
                onTotalPieceInputChange = {},
                onCustomerCaseQtyChange = { _, _ -> },
                onCustomerPieceQtyChange = { _, _ -> },
                onImageClick = {},
                onJanScanClick = {},
                onRegisterClick = {},
                onHistoryClick = {},
                moveToPrevGroup = {},
                moveToNextGroup = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}
