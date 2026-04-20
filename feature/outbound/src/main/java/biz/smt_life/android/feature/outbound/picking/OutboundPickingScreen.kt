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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay

// ===== Header colors (maintained and enhanced per harness) =====
private val TitleRed     = Color(0xFFC0392B)
private val AccentOrange = Color(0xFFE67E22)
private val DividerGold  = Color(0xFFF9A825)
private val HeaderBg     = Color(0xFFFDFBF2)
private val BadgeGreen   = Color(0xFF27AE60)
private val ProgressGray = Color(0xFFE0E0E0)
private val TimerBg      = Color(0xFFF0F0F0)

// ===== Body colors =====
private val BodyBg       = Color(0xFFF5F5F5)
private val Amber50      = Color(0xFFFFFBEB)
private val Amber200     = Color(0xFFFDE68A)
private val Amber300     = Color(0xFFFCD34D)
private val Amber600     = Color(0xFFD97706)
private val Amber700     = Color(0xFFB45309)
private val Neutral200   = Color(0xFFE5E5E5)
private val Neutral300   = Color(0xFFD4D4D4)
private val Neutral400   = Color(0xFFA3A3A3)
private val Neutral500   = Color(0xFF737373)
private val ReadonlyText = Color(0xFF888888)

// ===== Orientation persistence =====
private const val PREF_NAME_P21 = "p21_orientation_prefs"
private const val PREF_KEY_IS_PORTRAIT = "p21_is_portrait"

/**
 * Outbound Picking Screen (2.5.2 - 出庫データ入力).
 * Updated Header to 3-tier layout per upgrade-design-2.md.
 */
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

    // ===== Timer Logic =====
    var secondsElapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            secondsElapsed++
        }
    }

    // ===== Orientation control =====
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences(PREF_NAME_P21, Context.MODE_PRIVATE) }
    var isPortrait by remember { mutableStateOf(prefs.getBoolean(PREF_KEY_IS_PORTRAIT, false)) }

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
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    if (state.showImageDialog && state.currentGroup != null) {
        ImageViewerDialog(images = state.currentGroup!!.images, onDismiss = { viewModel.dismissImageDialog() })
    }

    if (state.showJanScannerDialog && state.currentGroup != null) {
        JanCodeScannerDialog(
            expectedJanCode = state.currentGroup!!.janCode,
            onResult = { code, match -> viewModel.onJanScanResult(code, match) },
            onDismiss = { viewModel.dismissJanScannerDialog() }
        )
    }

    Scaffold(
        containerColor = BodyBg,
        topBar = {
            OutboundPickingHeader(
                isPortrait = isPortrait,
                secondsElapsed = secondsElapsed,
                currentGroupIndex = state.currentGroupIndex,
                totalGroupCount = state.groupedItems.size,
                registeredCount = state.registeredGroupCount,
                onBackClick = onNavigateBack,
                onRotateClick = { toggleOrientation() },
                onPrevClick = { viewModel.moveToPrevGroup() },
                onNextClick = { viewModel.moveToNextGroup() }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.currentGroup != null && state.originalTask != null -> {
                    var offsetX by remember { mutableStateOf(0f) }
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (offsetX > 60f && state.canMovePrev) viewModel.moveToPrevGroup()
                                    else if (offsetX < -60f && state.canMoveNext) viewModel.moveToNextGroup()
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
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                else -> {
                    CompletionPlaceholder(
                        isFullyProcessed = state.originalTask?.isFullyProcessed == true,
                        isCompleting = state.isCompleting,
                        onComplete = { viewModel.completeTask(onSuccess = onTaskCompleted) }
                    )
                }
            }
        }
    }
}

// ===== NEW HEADER COMPONENT (3-Tier) =====

@Composable
private fun OutboundPickingHeader(
    isPortrait: Boolean,
    secondsElapsed: Long,
    currentGroupIndex: Int,
    totalGroupCount: Int,
    registeredCount: Int,
    onBackClick: () -> Unit,
    onRotateClick: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val timerText = formatTimer(secondsElapsed)
    val currentPage = currentGroupIndex + 1
    val progress = if (totalGroupCount > 0) registeredCount.toFloat() / totalGroupCount else 0f
    val percentText = "${(progress * 100).toInt()}%"

    Column(modifier = Modifier.background(HeaderBg)) {
        // --- 1段目: Header ---
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPortrait) {
                // Portrait: [Back] [Timer] [Rotate]
                TextButton(onClick = onBackClick) {
                    Text("もどる", color = TitleRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Surface(color = TimerBg, shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(timerText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRotateClick) {
                    Text("画面回転", color = TitleRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Landscape: [Back | Rotate] [Timer] [Progress Text]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBackClick) {
                        Text("もどる", color = TitleRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("|", color = Color.LightGray, modifier = Modifier.padding(horizontal = 4.dp))
                    TextButton(onClick = onRotateClick) {
                        Text("画面回転", color = TitleRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(timerText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "完了: $registeredCount/$totalGroupCount",
                    color = TitleRed, fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        // --- 2段目: Progress Bar ---
        Box(
            modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(24.dp),
                color = AccentOrange,
                trackColor = ProgressGray,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            // Overlay text with white outline (shadow)
            Text(
                text = if (isPortrait) "$currentPage/$totalGroupCount" else percentText,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    shadow = Shadow(color = Color.Black, blurRadius = 4f)
                )
            )
        }

        // --- 3段目: Controller ---
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevClick, enabled = currentPage > 1) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "前へ",
                    tint = if (currentPage > 1) Color.Black else Color.LightGray,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "作業番号 $currentPage/$totalGroupCount",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
            IconButton(onClick = onNextClick, enabled = currentPage < totalGroupCount) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "次へ",
                    tint = if (currentPage < totalGroupCount) Color.Black else Color.LightGray,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        HorizontalDivider(thickness = 2.dp, color = DividerGold)
    }
}

private fun formatTimer(seconds: Long): String {
    val m = (seconds / 60).toString().padStart(2, '0')
    val s = (seconds % 60).toString().padStart(2, '0')
    return "$m:$s"
}

// ===== BODY LAYOUT (Cleaned up redundant navigation) =====

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
    modifier: Modifier = Modifier
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
        label = "SwipeAnimation"
    ) { targetIndex ->
        val group = state.groupedItems.getOrNull(targetIndex) ?: state.currentGroup!!
        if (isPortrait) {
            Column(
                modifier = modifier.fillMaxSize().padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(0.4f).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), color = Color.White,
                    shadowElevation = 1.dp, border = BorderStroke(1.dp, Neutral200)
                ) {
                    ProductInfoSection(group = group, hasImages = state.hasImages, onImageClick = onImageClick, onJanScanClick = onJanScanClick, janScanResult = state.currentJanScanResult)
                }
                Surface(
                    modifier = Modifier.weight(0.6f).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), color = Color.White,
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
        } else {
            Row(
                modifier = modifier.fillMaxSize().padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp), color = Color.White,
                    shadowElevation = 1.dp, border = BorderStroke(1.dp, Neutral200)
                ) {
                    ProductInfoSection(group = group, hasImages = state.hasImages, onImageClick = onImageClick, onJanScanClick = onJanScanClick, janScanResult = state.currentJanScanResult)
                }
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp), color = Color.White,
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

@Composable
private fun CompletionPlaceholder(
    isFullyProcessed: Boolean,
    isCompleting: Boolean,
    onComplete: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                Icon(imageVector = Icons.Filled.CheckCircle, null, tint = BadgeGreen, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                if (isFullyProcessed) {
                    Text("作業が完了しました。", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212529))
                } else {
                    Text("すべての商品が登録されました。", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212529))
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onComplete,
                        enabled = !isCompleting,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        if (isCompleting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        else Text("完了", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ===== Existing Helper Sections (ProductInfoSection, GroupedQuantitySection, etc. maintained) =====

@Composable
private fun ProductInfoSection(
    group: GroupedPickingItem,
    hasImages: Boolean,
    onImageClick: () -> Unit,
    onJanScanClick: (Boolean) -> Unit,
    janScanResult: JanScanResult?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        var itemNameFontSize by remember(group.itemId) { mutableStateOf(18.sp) }
        Text(
            text = group.itemName,
            fontSize = itemNameFontSize,
            fontWeight = FontWeight.ExtraBold,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (result.hasVisualOverflow && itemNameFontSize > 12.sp) itemNameFontSize *= 0.9f
            },
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
        if (!group.janCode.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = group.janCode, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Neutral500)
                if (janScanResult != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = if (janScanResult.isMatch) BadgeGreen else Color(0xFFE74C3C), shape = RoundedCornerShape(6.dp)) {
                        Text(if (janScanResult.isMatch) "JAN一致" else "不一致", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
        }
        val specLine = buildString {
            if (group.volume != null) append(group.volume)
            if (group.capacityCase != null) { if (isNotEmpty()) append(" / "); append("入数:${group.capacityCase}") }
        }
        if (specLine.isNotEmpty()) Text(text = specLine, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Neutral500)
        Box(modifier = Modifier.fillMaxWidth().background(Amber50, RoundedCornerShape(6.dp)).border(1.dp, Amber300, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.CenterStart) {
            Text(text = group.locationCode?.ifBlank { null } ?: "未設定", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if (group.locationCode.isNullOrBlank()) Neutral400 else Color.Black)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HeaderButton(Modifier.weight(1.2f), Icons.Filled.Image, "商品画像", if (hasImages) Amber600 else Neutral400, Amber50, Amber200, onImageClick)
            HeaderButton(Modifier.weight(1f), Icons.Filled.CheckCircle, "JAN(IN)", Color(0xFF388E3C), Color(0xFFE8F5E9), Color(0xFF81C784)) { onJanScanClick(true) }
            HeaderButton(Modifier.weight(1f), Icons.Filled.CheckCircle, "JAN(OUT)", Color(0xFF388E3C), Color(0xFFE8F5E9), Color(0xFF81C784)) { onJanScanClick(false) }
        }
    }
}

@Composable
private fun HeaderButton(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, bg: Color, border: Color, onClick: () -> Unit) {
    Surface(modifier = modifier.height(44.dp), shape = RoundedCornerShape(8.dp), color = bg, border = BorderStroke(1.dp, border), onClick = onClick) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(2.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
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
    Column(modifier = modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().background(Amber50, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).border(1.dp, Amber300, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TableHeaderText("区分", Modifier.weight(1f))
            TableHeaderText("ケース", Modifier.weight(1f))
            TableHeaderText("バラ", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth().border(1.dp, Amber300).background(Color(0xFFFFF8E1)).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("合計", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TitleRed, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            QuantityInputBox(Modifier.weight(1f), state.totalCaseInput, onTotalCaseInputChange, state.totalCasePlanned, !state.isUpdating)
            QuantityInputBox(Modifier.weight(1f), state.totalPieceInput, onTotalPieceInputChange, state.totalPiecePlanned, !state.isUpdating)
        }
        HorizontalDivider(color = Amber300, thickness = 2.dp)
        var isExpanded by remember { mutableStateOf(false) }
        Row(modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("得意先別出荷数内訳", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Neutral500, modifier = Modifier.weight(1f))
            Icon(if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = Neutral500)
        }
        AnimatedVisibility(visible = isExpanded) {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                itemsIndexed(state.currentGroup?.customerEntries ?: emptyList()) { index, entry ->
                    CustomerEntryRow(entry = entry, onCaseQtyChange = { onCustomerCaseQtyChange(index, it) }, onPieceQtyChange = { onCustomerPieceQtyChange(index, it) }, isUpdating = state.isUpdating)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRegisterClick, enabled = state.canRegister, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Amber600)) {
                if (state.isUpdating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("登録", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onHistoryClick, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Amber50, contentColor = Amber700), border = BorderStroke(1.dp, Amber300)) {
                Text("履歴", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TableHeaderText(text: String, modifier: Modifier) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Neutral500, modifier = modifier, textAlign = TextAlign.Center)
}

@Composable
private fun QuantityInputBox(modifier: Modifier, value: String, onValueChange: (String) -> Unit, planned: Double, enabled: Boolean) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompactNumberInput(value, onValueChange, enabled, Modifier.width(56.dp).height(38.dp))
            Text("/${String.format("%.0f", planned)}", fontSize = 18.sp, color = Neutral500)
        }
    }
}

@Composable
private fun CustomerEntryRow(entry: CustomerEntry, onCaseQtyChange: (String) -> Unit, onPieceQtyChange: (String) -> Unit, isUpdating: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().border(0.5.dp, Neutral200).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(entry.customerCode.ifBlank { entry.customerName.ifBlank { "—" } }, modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (entry.caseEntry != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompactNumberInput(entry.caseEntry.pickedQtyInput, onCaseQtyChange, !isUpdating, Modifier.width(56.dp).height(36.dp))
                    Text("/${String.format("%.0f", entry.caseEntry.plannedQty)}", fontSize = 18.sp, color = Neutral500)
                }
            } else Text("—", fontSize = 13.sp, color = Neutral300)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (entry.pieceEntry != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompactNumberInput(entry.pieceEntry.pickedQtyInput, onPieceQtyChange, !isUpdating, Modifier.width(56.dp).height(36.dp))
                    Text("/${String.format("%.0f", entry.pieceEntry.plannedQty)}", fontSize = 18.sp, color = Neutral500)
                }
            } else Text("—", fontSize = 13.sp, color = Neutral300)
        }
    }
}

@Composable
private fun CompactNumberInput(value: String, onValueChange: (String) -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember(value) { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) { if (textFieldValue.text != value) textFieldValue = TextFieldValue(value, TextRange(value.length)) }
    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue -> textFieldValue = newValue; onValueChange(newValue.text) },
        enabled = enabled, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black),
        modifier = modifier.onFocusChanged { if (it.isFocused) { isFocused = true; textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length)) } else isFocused = false },
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize().background(Color.White, RoundedCornerShape(4.dp)).border(if (isFocused) 2.dp else 1.dp, if (isFocused) Amber600 else Neutral300, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp), contentAlignment = Alignment.Center) { inner() }
        }
    )
}

@Composable
private fun ImageViewerDialog(images: List<String>, onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { images.size.coerceAtLeast(1) })
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Column {
                Row(modifier = Modifier.fillMaxWidth().background(HeaderBg).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Image, null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("商品画像", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleRed)
                    if (images.size > 1) {
                        Surface(color = AccentOrange, shape = RoundedCornerShape(10.dp), modifier = Modifier.padding(start = 8.dp)) {
                            Text("${pagerState.currentPage + 1} / ${images.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Close, null, tint = TitleRed, modifier = Modifier.size(20.dp)) }
                }
                HorizontalDivider(color = DividerGold, thickness = 2.dp)
                if (images.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Image, null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(48.dp))
                            Text("画像が登録されていません", fontSize = 14.sp, color = ReadonlyText)
                        }
                    }
                } else {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) { page ->
                        SubcomposeAsyncImage(model = images[page], contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) {
                            when (painter.state) {
                                is AsyncImagePainter.State.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(40.dp)) }
                                is AsyncImagePainter.State.Error -> Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5)), Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.Image, null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(48.dp))
                                        Text("画像を読み込めません", fontSize = 13.sp, color = ReadonlyText)
                                    }
                                }
                                else -> SubcomposeAsyncImageContent()
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), Arrangement.End) { TextButton(onClick = onDismiss) { Text("閉じる", color = AccentOrange, fontWeight = FontWeight.Bold) } }
            }
        }
    }
}

private fun performHapticFeedback(context: android.content.Context) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {}
}

// ========== Previews ==========

@Preview(name = "P21 Header - Portrait", showBackground = true, widthDp = 420)
@Composable
private fun PreviewP21HeaderPortrait() {
    OutboundPickingHeader(
        isPortrait = true,
        secondsElapsed = 75, // 01:15
        currentGroupIndex = 0,
        totalGroupCount = 47,
        registeredCount = 1,
        onBackClick = {},
        onRotateClick = {},
        onPrevClick = {},
        onNextClick = {}
    )
}

@Preview(name = "P21 Header - Landscape", showBackground = true, widthDp = 800)
@Composable
private fun PreviewP21HeaderLandscape() {
    OutboundPickingHeader(
        isPortrait = false,
        secondsElapsed = 125, // 02:05
        currentGroupIndex = 23,
        totalGroupCount = 47,
        registeredCount = 24,
        onBackClick = {},
        onRotateClick = {},
        onPrevClick = {},
        onNextClick = {}
    )
}
