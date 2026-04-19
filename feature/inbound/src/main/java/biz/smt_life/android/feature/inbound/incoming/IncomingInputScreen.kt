package biz.smt_life.android.feature.inbound.incoming

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.IncomingQuantityType
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.Location
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun IncomingInputScreen(
    viewModel: IncomingViewModel,
    onNavigateBack: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val schedule = state.selectedSchedule
    val product = state.selectedProduct
    val targetWarehouseName = schedule?.warehouseName.orEmpty()
    val workingWarehouseName = state.selectedWarehouse?.name.orEmpty()
    val isVirtualWarehouse = schedule != null && state.selectedWarehouse?.id != schedule.warehouseId
    val quantity = state.inputQuantity.toIntOrNull()
    val quantityError = when {
        state.inputQuantity.isBlank() -> null
        quantity == null -> "数量は数値で入力してください"
        quantity <= 0 -> "数量は 1 以上で入力してください"
        schedule != null && quantity > schedule.remainingQuantity -> "数量は残数以内で入力してください"
        else -> null
    }
    val swipeSchedules = if (state.isFromHistory) {
        emptyList()
    } else {
        state.selectedProduct?.schedules.orEmpty().filter { it.status.canStartWork }
    }
    val currentScheduleIndex = swipeSchedules.indexOfFirst { it.id == schedule?.id }
    val canMovePrev = currentScheduleIndex > 0
    val canMoveNext = currentScheduleIndex >= 0 && currentScheduleIndex < swipeSchedules.lastIndex
    val scheduleLabel = when {
        state.isFromHistory -> "履歴編集"
        currentScheduleIndex >= 0 -> "作業 ${currentScheduleIndex + 1}/${swipeSchedules.size}"
        else -> "入荷入力"
    }
    val arrivalDateLabel = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    var showDatePicker by remember { mutableStateOf(false) }

    fun movePrev() {
        if (canMovePrev) {
            viewModel.selectSchedule(swipeSchedules[currentScheduleIndex - 1])
        }
    }

    fun moveNext() {
        if (canMoveNext) {
            viewModel.selectSchedule(swipeSchedules[currentScheduleIndex + 1])
        }
    }

    Scaffold(
        containerColor = IncomingBodyBg,
        topBar = {
            Column(modifier = Modifier.background(IncomingHeaderBg)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                            tint = IncomingTitleRed
                        )
                        Text(
                            text = if (state.isFromHistory) "履歴" else "一覧",
                            color = IncomingTitleRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = ::movePrev, enabled = canMovePrev) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "前へ",
                            tint = if (canMovePrev) IncomingTextPrimary else IncomingNeutral300,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IncomingCompactChip(
                            text = scheduleLabel,
                            background = IncomingAmber50,
                            border = IncomingAmber200,
                            contentColor = IncomingAmber700
                        )
                        IncomingCompactChip(text = "作業倉庫 $workingWarehouseName")
                        IncomingCompactChip(
                            text = "入荷対象 $targetWarehouseName",
                            background = if (isVirtualWarehouse) IncomingWarningOrange.copy(alpha = 0.12f) else IncomingAmber50,
                            border = if (isVirtualWarehouse) IncomingWarningOrange.copy(alpha = 0.35f) else IncomingAmber200,
                            contentColor = if (isVirtualWarehouse) IncomingWarningOrange else IncomingAmber700
                        )
                        if (!isPortrait) {
                            IncomingCompactChip(text = "入荷日 $arrivalDateLabel")
                        }
                        schedule?.let {
                            IncomingCompactChip(text = "残 ${it.remainingQuantity}")
                        }
                    }
                    IconButton(onClick = ::moveNext, enabled = canMoveNext) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "次へ",
                            tint = if (canMoveNext) IncomingTextPrimary else IncomingNeutral300,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                HorizontalDivider(thickness = 2.dp, color = IncomingDividerGold)
            }
        }
    ) { padding ->
        if (schedule == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("スケジュールが選択されていません", color = IncomingReadonlyText)
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            var offsetX by remember(schedule.id, state.isFromHistory) { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(schedule.id, canMovePrev, canMoveNext, state.isFromHistory) {
                        if (state.isFromHistory) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX > 60f && canMovePrev) {
                                    movePrev()
                                } else if (offsetX < -60f && canMoveNext) {
                                    moveNext()
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
                IncomingInputBody(
                    state = state,
                    schedule = schedule,
                    productName = product?.itemName ?: state.currentWorkItem?.schedule?.itemName.orEmpty(),
                    productCode = product?.itemCode ?: state.currentWorkItem?.schedule?.itemCode.orEmpty(),
                    janCode = product?.primaryJanCode ?: state.currentWorkItem?.schedule?.primaryJanCode,
                    specLine = listOfNotNull(product?.fullVolume, product?.temperatureType).joinToString(" / "),
                    isPortrait = isPortrait,
                    workingWarehouseName = workingWarehouseName,
                    targetWarehouseName = targetWarehouseName,
                    isVirtualWarehouse = isVirtualWarehouse,
                    quantityError = quantityError,
                    onQuantityChange = viewModel::onQuantityChange,
                    onExpirationDateChange = viewModel::onExpirationDateChange,
                    onLocationSearchChange = viewModel::onLocationSearchChange,
                    onSelectLocation = viewModel::selectLocation,
                    onFillRemaining = viewModel::setQuantityToExpected,
                    onOpenDatePicker = { showDatePicker = true },
                    onSubmit = { viewModel.submitEntry(onSubmitSuccess) }
                )
            }

            state.errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("閉じる", color = IncomingAccentOrange)
                        }
                    }
                ) {
                    Text(message)
                }
            }

            state.successMessage?.let { message ->
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    color = IncomingAccentOrange,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomingPanelBg
                    )
                }
            }

            if (state.isSubmitting) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    color = IncomingPanelBg.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = IncomingAccentOrange)
                        Text("処理中...", color = IncomingTextPrimary)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            val date = Instant.ofEpochMilli(selected)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE)
                            viewModel.onExpirationDateChange(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = IncomingAccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("キャンセル")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun IncomingInputBody(
    state: IncomingState,
    schedule: IncomingSchedule,
    productName: String,
    productCode: String,
    janCode: String?,
    specLine: String,
    isPortrait: Boolean,
    workingWarehouseName: String,
    targetWarehouseName: String,
    isVirtualWarehouse: Boolean,
    quantityError: String?,
    onQuantityChange: (String) -> Unit,
    onExpirationDateChange: (String) -> Unit,
    onLocationSearchChange: (String) -> Unit,
    onSelectLocation: (Location) -> Unit,
    onFillRemaining: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onSubmit: () -> Unit
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
                    .weight(0.42f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = IncomingPanelBg,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, IncomingNeutral200)
            ) {
                IncomingProductPanel(
                    productName = productName,
                    productCode = productCode,
                    janCode = janCode,
                    specLine = specLine,
                    schedule = schedule,
                    workingWarehouseName = workingWarehouseName,
                    targetWarehouseName = targetWarehouseName,
                    isVirtualWarehouse = isVirtualWarehouse
                )
            }
            Surface(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = IncomingPanelBg,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, IncomingNeutral200)
            ) {
                IncomingOperationPanel(
                    state = state,
                    schedule = schedule,
                    isPortrait = isPortrait,
                    quantityError = quantityError,
                    onQuantityChange = onQuantityChange,
                    onExpirationDateChange = onExpirationDateChange,
                    onLocationSearchChange = onLocationSearchChange,
                    onSelectLocation = onSelectLocation,
                    onFillRemaining = onFillRemaining,
                    onOpenDatePicker = onOpenDatePicker,
                    onSubmit = onSubmit
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
                color = IncomingPanelBg,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, IncomingNeutral200)
            ) {
                IncomingProductPanel(
                    productName = productName,
                    productCode = productCode,
                    janCode = janCode,
                    specLine = specLine,
                    schedule = schedule,
                    workingWarehouseName = workingWarehouseName,
                    targetWarehouseName = targetWarehouseName,
                    isVirtualWarehouse = isVirtualWarehouse
                )
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = IncomingPanelBg,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, IncomingNeutral200)
            ) {
                IncomingOperationPanel(
                    state = state,
                    schedule = schedule,
                    isPortrait = isPortrait,
                    quantityError = quantityError,
                    onQuantityChange = onQuantityChange,
                    onExpirationDateChange = onExpirationDateChange,
                    onLocationSearchChange = onLocationSearchChange,
                    onSelectLocation = onSelectLocation,
                    onFillRemaining = onFillRemaining,
                    onOpenDatePicker = onOpenDatePicker,
                    onSubmit = onSubmit
                )
            }
        }
    }
}

@Composable
private fun IncomingProductPanel(
    productName: String,
    productCode: String,
    janCode: String?,
    specLine: String,
    schedule: IncomingSchedule,
    workingWarehouseName: String,
    targetWarehouseName: String,
    isVirtualWarehouse: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = productName,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = IncomingTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "JAN ${janCode ?: "-"}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = IncomingNeutral500
        )
        Text(
            text = buildString {
                append("商品コード $productCode")
                if (specLine.isNotBlank()) {
                    append(" / ")
                    append(specLine)
                }
            },
            fontSize = 14.sp,
            color = IncomingNeutral500,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            border = BorderStroke(1.dp, IncomingNeutral200)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isVirtualWarehouse) {
                    IncomingCompactChip(
                        text = "作業倉庫 $workingWarehouseName",
                        background = IncomingWarningOrange.copy(alpha = 0.12f),
                        border = IncomingWarningOrange.copy(alpha = 0.35f),
                        contentColor = IncomingWarningOrange
                    )
                }
                IncomingCompactChip(
                    text = "入荷対象倉庫 $targetWarehouseName",
                    background = IncomingAmber50,
                    border = IncomingAmber200,
                    contentColor = IncomingAmber700
                )
                schedule.expectedArrivalDate?.takeIf { it.isNotBlank() }?.let {
                    Text("入荷予定日 $it", fontSize = 13.sp, color = IncomingNeutral500, fontWeight = FontWeight.Bold)
                }
                schedule.location?.let { location ->
                    Text(
                        text = "既定ロケーション ${location.displayName ?: location.fullDisplayName}",
                        fontSize = 13.sp,
                        color = IncomingNeutral500,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IncomingMetricBadge("予定", schedule.expectedQuantity, IncomingAmber50, IncomingAmber700)
            IncomingMetricBadge("済", schedule.receivedQuantity, IncomingBadgeGreen.copy(alpha = 0.14f), IncomingBadgeGreen)
            IncomingMetricBadge("残", schedule.remainingQuantity, IncomingAccentOrange.copy(alpha = 0.14f), IncomingAccentOrange)
        }

        ScheduleInputStatusBadge(status = schedule.status)
    }
}

@Composable
private fun IncomingOperationPanel(
    state: IncomingState,
    schedule: IncomingSchedule,
    isPortrait: Boolean,
    quantityError: String?,
    onQuantityChange: (String) -> Unit,
    onExpirationDateChange: (String) -> Unit,
    onLocationSearchChange: (String) -> Unit,
    onSelectLocation: (Location) -> Unit,
    onFillRemaining: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onSubmit: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val compact = maxHeight < 320.dp
        val isLandscape = !isPortrait
        val buttonHeight = if (compact) 40.dp else 44.dp
        val quantityInputHeight = if (isLandscape) 36.dp else 54.dp
        val quantityInputFontSize = if (isLandscape) 20.sp else 24.sp
        val quantityInputVerticalPadding = if (isLandscape) 2.dp else 4.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = IncomingAmber50,
                border = BorderStroke(1.dp, IncomingAmber300)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "入荷数量",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IncomingTitleRed
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onFillRemaining) {
                            Text("残数セット", color = IncomingAccentOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IncomingCompactNumberInput(
                            value = state.inputQuantity,
                            onValueChange = onQuantityChange,
                            enabled = !state.isSubmitting,
                            fontSize = quantityInputFontSize,
                            verticalPadding = quantityInputVerticalPadding,
                            modifier = Modifier
                                .width(88.dp)
                                .height(quantityInputHeight)
                        )
                        Text(
                            text = "/ ${schedule.remainingQuantity}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IncomingNeutral500
                        )
                        if (isLandscape) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IncomingQuantityTypeChip(
                                    label = "ケース",
                                    active = schedule.quantityType == IncomingQuantityType.CASE
                                )
                                IncomingQuantityTypeChip(
                                    label = "バラ",
                                    active = schedule.quantityType == IncomingQuantityType.PIECE
                                )
                            }
                        }
                    }
                    quantityError?.let {
                        Text(text = it, fontSize = 12.sp, color = IncomingWarningRed, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    IncomingExpirationSection(
                        expirationDate = state.inputExpirationDate,
                        onExpirationDateChange = onExpirationDateChange,
                        onOpenDatePicker = onOpenDatePicker,
                        modifier = Modifier.weight(1f)
                    )
                    IncomingLocationSection(
                        locationSearch = state.inputLocationSearch,
                        onLocationSearchChange = onLocationSearchChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                IncomingCompactChip(
                    text = "入荷日 ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                    background = Color.White,
                    border = IncomingNeutral200,
                    contentColor = IncomingNeutral500
                )
                IncomingExpirationSection(
                    expirationDate = state.inputExpirationDate,
                    onExpirationDateChange = onExpirationDateChange,
                    onOpenDatePicker = onOpenDatePicker
                )
                IncomingLocationSection(
                    locationSearch = state.inputLocationSearch,
                    onLocationSearchChange = onLocationSearchChange
                )
            }

            if (state.isLoadingLocations) {
                CircularProgressIndicator(
                    color = IncomingAccentOrange,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (state.locationSuggestions.isNotEmpty()) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = if (compact) 120.dp else 160.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, IncomingNeutral200)
                ) {
                    LazyColumn {
                        items(state.locationSuggestions.size) { index ->
                            val location = state.locationSuggestions[index]
                            LocationSuggestionItem(
                                location = location,
                                onClick = { onSelectLocation(location) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSubmit,
                    enabled = !state.isSubmitting && quantityError == null && schedule.remainingQuantity > 0 && state.inputQuantity.isNotBlank() && state.inputQuantity.toIntOrNull() != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomingAccentOrange)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (state.isFromHistory) "更新" else "登録",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onFillRemaining,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IncomingAmber50,
                        contentColor = IncomingAmber700
                    ),
                    border = BorderStroke(1.dp, IncomingAmber200)
                ) {
                    Text("残数セット", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun IncomingLocationSection(
    locationSearch: String,
    onLocationSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "ロケーション（作業倉庫側）",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = IncomingNeutral500
        )
        OutlinedTextField(
            value = locationSearch,
            onValueChange = onLocationSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("ロケーション検索") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.PinDrop,
                    contentDescription = null,
                    tint = IncomingAccentOrange
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = IncomingPanelBg,
                unfocusedContainerColor = IncomingPanelBg,
                focusedBorderColor = IncomingAccentOrange,
                unfocusedBorderColor = IncomingNeutral300
            )
        )
    }
}

@Composable
private fun IncomingExpirationSection(
    expirationDate: String,
    onExpirationDateChange: (String) -> Unit,
    onOpenDatePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "賞味期限（任意）",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = IncomingNeutral500
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = expirationDate,
                onValueChange = onExpirationDateChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = IncomingPanelBg,
                    unfocusedContainerColor = IncomingPanelBg,
                    focusedBorderColor = IncomingAccentOrange,
                    unfocusedBorderColor = IncomingNeutral300
                )
            )
            IncomingHeaderActionButton(
                modifier = Modifier.width(104.dp),
                icon = Icons.Default.EditCalendar,
                label = "日付選択",
                tint = IncomingAccentOrange,
                background = IncomingAmber50,
                border = IncomingAmber200,
                onClick = onOpenDatePicker
            )
        }
    }
}

@Composable
private fun IncomingQuantityTypeChip(
    label: String,
    active: Boolean
) {
    Surface(
        color = if (active) IncomingAccentOrange.copy(alpha = 0.12f) else IncomingAmber50,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (active) IncomingAccentOrange.copy(alpha = 0.35f) else IncomingNeutral200
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) IncomingAccentOrange else IncomingNeutral500
        )
    }
}

@Composable
private fun ScheduleInputStatusBadge(status: biz.smt_life.android.core.domain.model.IncomingScheduleStatus) {
    val (text, color) = when (status) {
        biz.smt_life.android.core.domain.model.IncomingScheduleStatus.PENDING -> "未入荷" to IncomingAccentOrange
        biz.smt_life.android.core.domain.model.IncomingScheduleStatus.PARTIAL -> "一部入荷" to IncomingWarningOrange
        biz.smt_life.android.core.domain.model.IncomingScheduleStatus.CONFIRMED -> "確定済" to IncomingBadgeGreen
        biz.smt_life.android.core.domain.model.IncomingScheduleStatus.TRANSMITTED -> "連携済" to IncomingNeutral500
        biz.smt_life.android.core.domain.model.IncomingScheduleStatus.CANCELLED -> "キャンセル" to IncomingWarningRed
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun IncomingCompactNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    verticalPadding: androidx.compose.ui.unit.Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val filtered = newValue.text.filter { it.isDigit() }
            textFieldValue = TextFieldValue(filtered, TextRange(filtered.length))
            onValueChange(filtered)
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = IncomingTextPrimary,
            textAlign = TextAlign.Center
        ),
        modifier = modifier.onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            if (focusState.isFocused) {
                textFieldValue = textFieldValue.copy(
                    selection = TextRange(0, textFieldValue.text.length)
                )
            }
        },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(IncomingPanelBg, RoundedCornerShape(8.dp))
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) IncomingAccentOrange else IncomingNeutral300,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = verticalPadding),
                contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
        }
    )
}

@Composable
private fun LocationSuggestionItem(
    location: Location,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = location.displayName ?: location.fullDisplayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = IncomingTextPrimary
        )
        val code = listOfNotNull(location.code1, location.code2, location.code3)
            .filter { it.isNotBlank() }
            .joinToString("-")
        if (code.isNotBlank()) {
            Text(
                text = code,
                fontSize = 12.sp,
                color = IncomingNeutral500
            )
        }
    }
}
