package biz.smt_life.android.feature.outbound.proxyshipment

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.ProxyShipmentCompletionResult
import biz.smt_life.android.core.domain.model.ProxyShipmentDetail
import biz.smt_life.android.feature.outbound.picking.JanCodeScannerDialog
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import kotlinx.coroutines.delay
import kotlin.math.max

private enum class ProxyShipmentLeaveAction {
    BACK,
    LIST,
    PREV,
    NEXT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyShipmentPickingScreen(
    shipmentDate: String,
    courseKey: String,
    onNavigateBack: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToResult: (Int) -> Unit,
    viewModel: ProxyShipmentPickingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var pendingLeaveAction by remember { mutableStateOf<ProxyShipmentLeaveAction?>(null) }
    var completionResult by remember { mutableStateOf<ProxyShipmentCompletionResult?>(null) }
    var secondsElapsed by remember { mutableLongStateOf(0L) }

    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences(PROXY_PREF_NAME_ORIENTATION, Context.MODE_PRIVATE) }
    var isPortrait by remember { mutableStateOf(prefs.getBoolean(PROXY_PREF_KEY_IS_PORTRAIT, false)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            secondsElapsed++
        }
    }

    LaunchedEffect(isPortrait) {
        activity?.requestedOrientation = if (isPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    LaunchedEffect(shipmentDate, courseKey) {
        viewModel.initialize(shipmentDate, courseKey)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    fun executePendingLeaveAction() {
        when (pendingLeaveAction) {
            ProxyShipmentLeaveAction.BACK -> onNavigateBack()
            ProxyShipmentLeaveAction.LIST -> onNavigateToList()
            ProxyShipmentLeaveAction.PREV -> viewModel.moveToPrev()
            ProxyShipmentLeaveAction.NEXT -> viewModel.moveToNext()
            null -> Unit
        }
        pendingLeaveAction = null
    }

    fun requestBack() {
        if (state.hasUnsavedChanges) {
            pendingLeaveAction = ProxyShipmentLeaveAction.BACK
            showLeaveDialog = true
        } else {
            onNavigateBack()
        }
    }

    fun requestList() {
        if (state.hasUnsavedChanges) {
            pendingLeaveAction = ProxyShipmentLeaveAction.LIST
            showLeaveDialog = true
        } else {
            onNavigateToList()
        }
    }

    fun requestPrev() {
        if (state.hasUnsavedChanges) {
            pendingLeaveAction = ProxyShipmentLeaveAction.PREV
            showLeaveDialog = true
        } else {
            viewModel.moveToPrev()
        }
    }

    fun requestNext() {
        if (state.hasUnsavedChanges) {
            pendingLeaveAction = ProxyShipmentLeaveAction.NEXT
            showLeaveDialog = true
        } else {
            viewModel.moveToNext()
        }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("未保存の変更があります") },
            text = { Text("入力を保存して移動するか、破棄して移動するかを選択してください。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        viewModel.update(onSuccess = ::executePendingLeaveAction)
                    }
                ) {
                    Text("更新して移動")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showLeaveDialog = false
                            executePendingLeaveAction()
                        }
                    ) {
                        Text("破棄して移動")
                    }
                    TextButton(onClick = { showLeaveDialog = false }) {
                        Text("キャンセル")
                    }
                }
            }
        )
    }

    if (state.showJanScannerDialog) {
        JanCodeScannerDialog(
            expectedJanCodes = state.detail?.allocation?.item?.janCodes.orEmpty(),
            onResult = viewModel::onJanScanResult,
            onDismiss = viewModel::dismissScanner
        )
    }

    if (state.showImageDialog) {
        ProxyShipmentImageViewerDialog(
            images = state.detail?.allocation?.item?.images.orEmpty(),
            onDismiss = viewModel::dismissImages
        )
    }

    completionResult?.let { completion ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("作業完了") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("横持ち出荷の作業が完了しました。")
                    completion.stockTransferQueueId?.let { queueId ->
                        Text("移動伝票キューID: $queueId")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        completionResult = null
                        onNavigateToResult(completion.allocation.allocationId)
                    }
                ) {
                    Text("確認")
                }
            }
        )
    }

    Scaffold(
        containerColor = BodyBg,
        topBar = {
            ProxyShipmentPickingHeader(
                isPortrait = isPortrait,
                secondsElapsed = secondsElapsed,
                state = state,
                onBackClick = ::requestBack,
                onRotateClick = {
                    isPortrait = !isPortrait
                    prefs.edit().putBoolean(PROXY_PREF_KEY_IS_PORTRAIT, isPortrait).apply()
                },
                onPrevClick = ::requestPrev,
                onNextClick = ::requestNext
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentOrange)
                    }
                }

                state.detail == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("横持ち出荷詳細を取得できませんでした")
                    }
                }

                else -> {
                    val detail = requireNotNull(state.detail)
                    var offsetX by remember { mutableStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(state.currentAllocationIndex, state.hasUnsavedChanges) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (offsetX > 60f && state.canMovePrev) {
                                            requestPrev()
                                        } else if (offsetX < -60f && state.canMoveNext) {
                                            requestNext()
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
                        ProxyShipmentPickingBody(
                            detail = detail,
                            state = state,
                            isPortrait = isPortrait,
                            onPickedQtyChange = viewModel::onPickedQtyInputChange,
                            onImageClick = viewModel::showImages,
                            onJanClick = viewModel::showScanner,
                            onUpdateClick = {
                                viewModel.complete { completion: ProxyShipmentCompletionResult ->
                                    completionResult = completion
                                }
                            },
                            onListClick = ::requestList
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxyShipmentPickingHeader(
    isPortrait: Boolean,
    secondsElapsed: Long,
    state: ProxyShipmentPickingState,
    onBackClick: () -> Unit,
    onRotateClick: () -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val allocation = state.allocation
    val timerText = formatProxyTimer(secondsElapsed)
    val titleText = allocation?.let {
        "${it.deliveryCourse?.name ?: "配送コース未設定"} / ${ProxyShipmentDateFormatter.toDisplay(it.shipmentDate)}"
    } ?: "横持ち出荷"

    Column(modifier = Modifier.background(HeaderBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("戻る", color = TitleRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            ProxyShipmentAutoShrinkText(
                text = titleText,
                modifier = Modifier.weight(1f),
                maxFontSize = if (isPortrait) 15.sp else 16.sp,
                midFontSize = 14.sp,
                minFontSize = 12.sp,
                maxLines = 1,
                fontWeight = FontWeight.ExtraBold,
                color = TitleRed,
                textAlign = TextAlign.Center
            )
            Surface(color = TimerBg, shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        timerText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
            }
            IconButton(onClick = onRotateClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = "画面回転",
                    tint = AccentOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            allocation?.let {
                ProxyShipmentStatusBadge(
                    status = it.status,
                    compact = true
                )
                if (!isPortrait) {
                    ProxyShipmentCompactChip(
                        text = "出荷元 ${it.pickupWarehouse.name}",
                        background = Color.White,
                        border = Neutral200,
                        contentColor = Neutral500
                    )
                }
                if (state.totalAllocationCount > 1) {
                    ProxyShipmentCompactChip(
                        text = "作業 ${state.currentAllocationIndex + 1}/${state.totalAllocationCount}",
                        background = Color.White,
                        border = Neutral200,
                        contentColor = Neutral500
                    )
                }
                ProxyShipmentCompactChip(
                    text = "伝票 ${it.slipNumber ?: "-"}",
                    background = Color.White,
                    border = Neutral200,
                    contentColor = Neutral500
                )
            } ?: Text(
                text = "横持ち出荷ピッキング",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Neutral500
            )
            Spacer(Modifier.weight(1f))
            if (state.totalAllocationCount > 1) {
                IconButton(onClick = onPrevClick, enabled = state.canMovePrev) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "前へ",
                        tint = if (state.canMovePrev) Color.Black else Color.LightGray
                    )
                }
                IconButton(onClick = onNextClick, enabled = state.canMoveNext) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "次へ",
                        tint = if (state.canMoveNext) Color.Black else Color.LightGray
                    )
                }
            }
        }

        HorizontalDivider(thickness = 2.dp, color = DividerGold)
    }
}

@Composable
private fun ProxyShipmentPickingBody(
    detail: ProxyShipmentDetail,
    state: ProxyShipmentPickingState,
    isPortrait: Boolean,
    onPickedQtyChange: (String) -> Unit,
    onImageClick: () -> Unit,
    onJanClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onListClick: () -> Unit
) {
    if (isPortrait) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(0.43f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Neutral200)
            ) {
                ProxyShipmentProductPanel(
                    detail = detail,
                    isPortrait = true,
                    janScanFeedback = state.janScanFeedback,
                    onImageClick = onImageClick,
                    onJanClick = onJanClick
                )
            }
            Surface(
                modifier = Modifier
                    .weight(0.57f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Neutral200)
            ) {
                ProxyShipmentOperationPanel(
                    detail = detail,
                    isPortrait = true,
                    state = state,
                    onPickedQtyChange = onPickedQtyChange,
                    onUpdateClick = onUpdateClick,
                    onListClick = onListClick
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Neutral200)
            ) {
                ProxyShipmentProductPanel(
                    detail = detail,
                    isPortrait = false,
                    janScanFeedback = state.janScanFeedback,
                    onImageClick = onImageClick,
                    onJanClick = onJanClick
                )
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Neutral200)
            ) {
                ProxyShipmentOperationPanel(
                    detail = detail,
                    isPortrait = false,
                    state = state,
                    onPickedQtyChange = onPickedQtyChange,
                    onUpdateClick = onUpdateClick,
                    onListClick = onListClick
                )
            }
        }
    }
}

@Composable
private fun ProxyShipmentProductPanel(
    detail: ProxyShipmentDetail,
    isPortrait: Boolean,
    janScanFeedback: ProxyShipmentJanScanFeedback?,
    onImageClick: () -> Unit,
    onJanClick: () -> Unit
) {
    val allocation = detail.allocation
    val item = allocation.item
    val specLine = buildString {
        if (!item.volume.isNullOrBlank()) append(item.volume)
        if (item.capacityCase != null) {
            if (isNotEmpty()) append(" / ")
            append("入数:${item.capacityCase}")
        }
        if (!item.temperatureType.isNullOrBlank()) {
            if (isNotEmpty()) append(" / ")
            append(item.temperatureType)
        }
    }
    val janLine = proxyShipmentJanLine(item.janCodes)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dense = maxHeight < 290.dp
        val extraDense = maxHeight < 235.dp
        val gap = if (extraDense) 4.dp else 6.dp
        val titleSize = when {
            extraDense -> 14.sp
            dense -> 16.sp
            else -> 18.sp
        }
        val bodySize = when {
            extraDense -> 11.sp
            dense -> 12.sp
            else -> 13.sp
        }
        val emphasisSize = when {
            extraDense -> 12.sp
            dense -> 13.sp
            else -> 14.sp
        }
        val buttonHeight = if (extraDense) 36.dp else 40.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (extraDense) 6.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            ProxyShipmentAutoShrinkText(
                text = item.name,
                maxFontSize = titleSize,
                midFontSize = (titleSize.value - 1f).sp,
                minFontSize = if (extraDense) 12.sp else 13.sp,
                maxLines = 2,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )

            ProxyShipmentAutoShrinkText(
                text = buildString {
                    append("商品コード ${item.code}")
                    if (specLine.isNotBlank()) {
                        append(" / ")
                        append(specLine)
                    }
                },
                maxFontSize = emphasisSize,
                midFontSize = bodySize,
                minFontSize = 11.sp,
                maxLines = 2,
                fontWeight = FontWeight.Bold,
                color = Neutral500
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Neutral200)
            ) {
                ProxyShipmentMetricGrid(
                    entries = buildList {
                        if (isPortrait) {
                            add("出荷元" to allocation.pickupWarehouse.name)
                            add("送り先" to allocation.destinationWarehouse.name)
                        }
                        add("得意先" to (allocation.customer?.name ?: "未設定"))
                        add("コース" to (allocation.deliveryCourse?.name ?: "未設定"))
                        if (isPortrait) {
                            add("伝票" to (allocation.slipNumber?.toString() ?: "-"))
                            add("出荷日" to ProxyShipmentDateFormatter.toDisplay(allocation.shipmentDate))
                        }
                    },
                    columns = 2,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    labelFontSize = if (extraDense) 10.sp else 11.sp,
                    valueFontSize = bodySize,
                    minValueFontSize = 10.sp
                )
            }

            if (isPortrait) {
                ProxyShipmentJanSection(
                    janLine = janLine,
                    janScanFeedback = janScanFeedback,
                    maxFontSize = if (extraDense) 24.sp else 28.sp,
                    midFontSize = if (extraDense) 20.sp else 24.sp,
                    minFontSize = if (extraDense) 16.sp else 18.sp,
                    titleFontSize = if (extraDense) 11.sp else 12.sp,
                    lineColor = if (item.janCodes.isEmpty()) Neutral400 else Color.Black
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProxyShipmentActionHeaderButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    icon = Icons.Default.Image,
                    label = "商品画像",
                    tint = if (item.images.isNotEmpty()) Amber600 else Neutral400,
                    bg = Amber50,
                    border = Amber200,
                    enabled = item.images.isNotEmpty(),
                    onClick = onImageClick
                )
                ProxyShipmentActionHeaderButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    icon = Icons.Default.QrCodeScanner,
                    label = "JAN確認",
                    tint = Color(0xFF388E3C),
                    bg = Color(0xFFE8F5E9),
                    border = Color(0xFF81C784),
                    enabled = item.janCodes.isNotEmpty(),
                    onClick = onJanClick
                )
            }
        }
    }
}

@Composable
private fun ProxyShipmentOperationPanel(
    detail: ProxyShipmentDetail,
    isPortrait: Boolean,
    state: ProxyShipmentPickingState,
    onPickedQtyChange: (String) -> Unit,
    onUpdateClick: () -> Unit,
    onListClick: () -> Unit
) {
    val allocation = detail.allocation
    val janLine = proxyShipmentJanLine(allocation.item.janCodes)
    val currentPickedQty = state.parsedPickedQty ?: allocation.pickedQty

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val extraDense = maxHeight < 270.dp
        val gap = if (extraDense) 4.dp else 6.dp
        val titleSize = if (extraDense) 13.sp else 14.sp
        val bodySize = if (extraDense) 11.sp else 12.sp
        val strongSize = if (extraDense) 15.sp else 17.sp
        val buttonHeight = if (extraDense) 40.dp else 44.dp
        val locationRows = detail.candidateLocations.chunked(2)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (extraDense) 6.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            if (!isPortrait) {
                ProxyShipmentJanSection(
                    janLine = janLine,
                    janScanFeedback = state.janScanFeedback,
                    maxFontSize = if (extraDense) 24.sp else 28.sp,
                    midFontSize = if (extraDense) 20.sp else 24.sp,
                    minFontSize = if (extraDense) 16.sp else 18.sp,
                    titleFontSize = if (extraDense) 11.sp else 12.sp,
                    lineColor = if (allocation.item.janCodes.isEmpty()) Neutral400 else Color.Black
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Amber50,
                border = BorderStroke(1.dp, Amber300)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("数量入力", fontSize = titleSize, fontWeight = FontWeight.ExtraBold, color = TitleRed)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("実績", fontSize = bodySize, fontWeight = FontWeight.Bold, color = Neutral500)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProxyShipmentNumberInput(
                                value = state.pickedQtyInput,
                                onValueChange = onPickedQtyChange,
                                enabled = !state.isSubmitting,
                                modifier = Modifier
                                    .width(if (extraDense) 74.dp else 82.dp)
                                    .height(if (extraDense) 42.dp else 46.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            ProxyShipmentAutoShrinkText(
                                text = "/${allocation.assignQty} ${allocation.assignQtyType.label}",
                                maxFontSize = strongSize,
                                midFontSize = bodySize,
                                minFontSize = 11.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        }
                    }
                    state.quantityErrorMessage?.let {
                        Text(it, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                    ProxyShipmentCompactChip(
                        text = "残数 ${max(allocation.assignQty - currentPickedQty, 0)} ${allocation.assignQtyType.label}",
                        background = Color.White,
                        border = Amber200,
                        contentColor = AccentOrange
                    )
                }
            }

            detail.shortageDetail?.takeIf { isPortrait }?.let { shortage ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Neutral200)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("欠品情報", fontSize = titleSize, fontWeight = FontWeight.ExtraBold, color = Neutral500)
                        ProxyShipmentMetricGrid(
                            entries = listOf(
                                "受注数" to shortage.orderQty.toString(),
                                "引当予定" to shortage.plannedQty.toString(),
                                "通常出荷済" to shortage.pickedQty.toString(),
                                "欠品数" to shortage.shortageQty.toString(),
                                "受注単位" to shortage.qtyTypeAtOrder
                            ),
                            columns = 2,
                            modifier = Modifier.fillMaxWidth(),
                            labelFontSize = 10.sp,
                            valueFontSize = bodySize,
                            minValueFontSize = 10.sp
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Neutral200)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("候補ロケーション", fontSize = titleSize, fontWeight = FontWeight.ExtraBold, color = Neutral500)
                    if (detail.candidateLocations.isEmpty()) {
                        Text("候補ロケーションはありません", fontSize = bodySize, color = ReadonlyText)
                    } else {
                        locationRows.forEach { rowLocations ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowLocations.forEach { location ->
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Amber50,
                                        border = BorderStroke(1.dp, Amber200)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            ProxyShipmentAutoShrinkText(
                                                text = location.code,
                                                maxFontSize = if (extraDense) 12.sp else 13.sp,
                                                midFontSize = 11.sp,
                                                minFontSize = 10.sp,
                                                maxLines = 1,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                "在庫 ${location.availableQty}",
                                                fontSize = if (extraDense) 10.sp else 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentOrange
                                            )
                                        }
                                    }
                                }
                                if (rowLocations.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onUpdateClick,
                    enabled = state.canUpdate,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber600)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("更新", fontSize = bodySize, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onListClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Amber300)
                    ) {
                        Text("一覧", fontSize = bodySize, fontWeight = FontWeight.Bold, color = Amber700)
                    }
                }
        }
    }
}

@Composable
private fun ProxyShipmentJanSection(
    janLine: String,
    janScanFeedback: ProxyShipmentJanScanFeedback?,
    maxFontSize: TextUnit,
    midFontSize: TextUnit,
    minFontSize: TextUnit,
    titleFontSize: TextUnit,
    lineColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Amber50,
        border = BorderStroke(1.dp, Amber300)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("JANコード", fontSize = titleFontSize, fontWeight = FontWeight.ExtraBold, color = TitleRed)
            ProxyShipmentAutoShrinkText(
                text = janLine,
                maxFontSize = maxFontSize,
                midFontSize = midFontSize,
                minFontSize = minFontSize,
                maxLines = 1,
                fontWeight = FontWeight.ExtraBold,
                color = lineColor
            )
            janScanFeedback?.let { feedback ->
                ProxyShipmentCompactChip(
                    text = if (feedback.isMatch) {
                        "JAN一致 ${feedback.scannedCode}"
                    } else {
                        "JAN不一致 ${feedback.scannedCode}"
                    },
                    background = if (feedback.isMatch) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    border = if (feedback.isMatch) Color(0xFF81C784) else Color(0xFFE57373),
                    contentColor = if (feedback.isMatch) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
    }
}

private fun proxyShipmentJanLine(janCodes: List<String>): String {
    return if (janCodes.isEmpty()) {
        "JAN : 未登録"
    } else {
        "JAN : ${janCodes.joinToString(separator = " / ")}"
    }
}

@Composable
private fun ProxyShipmentMetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Neutral500)
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun ProxyShipmentCompactChip(
    text: String,
    background: Color,
    border: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = BorderStroke(1.dp, border)
    ) {
        ProxyShipmentAutoShrinkText(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxFontSize = 12.sp,
            midFontSize = 11.sp,
            minFontSize = 10.sp,
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
private fun ProxyShipmentMetricGrid(
    entries: List<Pair<String, String>>,
    columns: Int,
    modifier: Modifier = Modifier,
    labelFontSize: TextUnit,
    valueFontSize: TextUnit,
    minValueFontSize: TextUnit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        entries.chunked(columns.coerceAtLeast(1)).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowEntries.forEach { (label, value) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = labelFontSize,
                            fontWeight = FontWeight.Medium,
                            color = Neutral500
                        )
                        ProxyShipmentAutoShrinkText(
                            text = value,
                            maxFontSize = valueFontSize,
                            midFontSize = (valueFontSize.value - 1f).sp,
                            minFontSize = minValueFontSize,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
                if (rowEntries.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProxyShipmentAutoShrinkText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit,
    midFontSize: TextUnit,
    minFontSize: TextUnit,
    maxLines: Int,
    fontWeight: FontWeight,
    color: Color,
    textAlign: TextAlign? = null
) {
    val fontSize = proxyShipmentAdaptiveFont(
        text = text,
        maxFontSize = maxFontSize,
        midFontSize = midFontSize,
        minFontSize = minFontSize,
        maxLines = maxLines
    )
    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign
    )
}

private fun proxyShipmentAdaptiveFont(
    text: String,
    maxFontSize: TextUnit,
    midFontSize: TextUnit,
    minFontSize: TextUnit,
    maxLines: Int
): TextUnit {
    val midThreshold = if (maxLines >= 2) 24 else 16
    val minThreshold = if (maxLines >= 2) 42 else 26
    return when {
        text.length >= minThreshold -> minFontSize
        text.length >= midThreshold -> midFontSize
        else -> maxFontSize
    }
}

@Composable
private fun ProxyShipmentActionHeaderButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    border: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
private fun ProxyShipmentNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }

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
        textStyle = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = Color.Black
        ),
        modifier = modifier.onFocusChanged {
            if (it.isFocused) {
                isFocused = true
                textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length))
            } else {
                isFocused = false
            }
        },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .border(
                        if (isFocused) 2.dp else 1.dp,
                        if (isFocused) Amber600 else Neutral300,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
        }
    )
}

@Composable
private fun ProxyShipmentImageViewerDialog(
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
                .height(if (images.isEmpty()) 260.dp else 420.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HeaderBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("商品画像", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleRed)
                    if (images.size > 1) {
                        Surface(
                            color = AccentOrange,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                "${pagerState.currentPage + 1} / ${images.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TitleRed, modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(color = DividerGold, thickness = 2.dp)

                if (images.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(48.dp))
                            Text("画像が登録されていません", fontSize = 14.sp, color = ReadonlyText)
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        SubcomposeAsyncImage(
                            model = images[page],
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        ) {
                            when (painter.state) {
                                is AsyncImagePainter.State.Loading -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = AccentOrange, modifier = Modifier.size(40.dp))
                                }

                                is AsyncImagePainter.State.Error -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFF5F5F5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(48.dp))
                                        Text("画像を読み込めません", fontSize = 13.sp, color = ReadonlyText)
                                    }
                                }

                                else -> SubcomposeAsyncImageContent()
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
