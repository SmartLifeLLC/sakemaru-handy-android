package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biz.smt_life.android.core.domain.model.Location
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─── Color definitions ────────────────────────────────────────────────────────
private val AccentGreen  = Color(0xFF27AE60)
private val DarkGreen    = Color(0xFF1A7A4A)
private val BodyBg       = Color.White
private val HeaderBg     = Color(0xFFF0FFF4)
private val DividerGreen = Color(0xFFD5F5E3)
private val CardBorder   = Color(0xFFB2DFDB)
private val TextPrimary  = Color(0xFF212529)
private val TextSecond   = Color(0xFF555555)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingInputScreen(
    viewModel: IncomingViewModel,
    onNavigateBack: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Navigate back on success
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            kotlinx.coroutines.delay(1500)
            viewModel.clearSuccess()
            onSubmitSuccess()
        }
    }

    val scheduleInfo = state.selectedSchedule
    val workItem = state.currentWorkItem
    val itemName = if (state.isFromHistory) {
        workItem?.schedule?.itemName ?: ""
    } else {
        state.selectedProduct?.itemName ?: ""
    }
    val expectedQty = if (state.isFromHistory) {
        workItem?.schedule?.remainingQuantity ?: 0
    } else {
        scheduleInfo?.remainingQuantity ?: 0
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
                                tint = AccentGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "入荷処理 ｜ ${state.selectedWarehouse?.name ?: ""}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る",
                                tint = AccentGreen
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderBg)
                )
                HorizontalDivider(thickness = 1.dp, color = DividerGreen)
            }
        },
        bottomBar = {
            FunctionKeyBar(
                f1 = FunctionKeyAction("賞味") { showDatePicker = true },
                f2 = FunctionKeyAction("戻る", onNavigateBack),
                f3 = FunctionKeyAction("登録") { viewModel.submitIncoming() },
                centerAligned = true
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product info header
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = itemName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (!state.isFromHistory) {
                            val product = state.selectedProduct
                            if (product != null) {
                                Text(
                                    text = "JAN: ${product.janCodes.firstOrNull() ?: "-"}  Code: ${product.itemCode}",
                                    fontSize = 12.sp,
                                    color = TextSecond
                                )
                            }
                        }
                    }
                }

                // Arrival date
                Text("入荷日: $today", fontSize = 14.sp, color = TextSecond)

                // Quantity input
                Column {
                    Text(
                        text = "入荷数量  入荷予定: $expectedQty",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.inputQuantity,
                        onValueChange = { viewModel.onQuantityChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = CardBorder
                        )
                    )
                }

                // Expiration date
                Column {
                    Text(
                        text = "賞味期限（任意）",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.inputExpirationDate,
                        onValueChange = { viewModel.onExpirationDateChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("YYYY-MM-DD") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "カレンダー",
                                    tint = AccentGreen
                                )
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = CardBorder
                        )
                    )
                }

                // Location search
                Column {
                    Text(
                        text = "ロケーション",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.inputLocationSearch,
                        onValueChange = { viewModel.onLocationSearchChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ロケーション検索") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = CardBorder
                        )
                    )

                    // Location suggestions
                    if (state.locationSuggestions.isNotEmpty()) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CardBorder),
                            elevation = CardDefaults.outlinedCardElevation(defaultElevation = 4.dp)
                        ) {
                            Column {
                                state.locationSuggestions.forEach { location ->
                                    LocationSuggestionItem(
                                        location = location,
                                        onClick = { viewModel.selectLocation(location) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Success message overlay
            state.successMessage?.let { message ->
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    color = AccentGreen,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(24.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Submitting indicator
            if (state.isSubmitting) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = AccentGreen)
                        Spacer(Modifier.height(8.dp))
                        Text("登録中...", color = TextPrimary)
                    }
                }
            }

            // Error
            state.errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("閉じる", color = AccentGreen)
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.onExpirationDateChange(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                    showDatePicker = false
                }) { Text("OK", color = AccentGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("キャンセル", color = TextSecond)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                headline = {
                    Text(
                        text = "賞味期限選択",
                        modifier = androidx.compose.ui.Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        color = AccentGreen
                    )
                }
            )
        }
    }
}

@Composable
private fun LocationSuggestionItem(
    location: Location,
    onClick: () -> Unit
) {
    Text(
        text = location.displayName,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        fontSize = 14.sp,
        color = DarkGreen
    )
    HorizontalDivider(color = Color(0xFFEEEEEE))
}
