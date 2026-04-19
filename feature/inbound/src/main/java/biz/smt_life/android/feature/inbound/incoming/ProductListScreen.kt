package biz.smt_life.android.feature.inbound.incoming

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.IncomingProduct

@Composable
fun ProductListScreen(
    viewModel: IncomingViewModel,
    onNavigateBack: () -> Unit,
    onProductSelected: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    var showJanScannerDialog by remember { mutableStateOf(false) }
    var isJanScannerInCamera by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initializeDefaultWarehouse()
    }

    if (showJanScannerDialog) {
        IncomingJanScannerDialog(
            isInCamera = isJanScannerInCamera,
            onScan = { code ->
                showJanScannerDialog = false
                viewModel.onProductBarcodeScan(code)
            },
            onDismiss = { showJanScannerDialog = false }
        )
    }

    Scaffold(
        containerColor = IncomingBodyBg,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
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
                            text = "メニュー",
                            color = IncomingTitleRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = state.selectedWarehouse?.name ?: "入荷",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomingTitleRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "履歴",
                            tint = IncomingAccentOrange
                        )
                        Text(
                            text = "履歴",
                            color = IncomingAccentOrange,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.selectedWarehouse?.let { warehouse ->
                        IncomingCompactChip(
                            text = "作業倉庫 ${warehouse.code}",
                            background = IncomingAmber50,
                            border = IncomingAmber200,
                            contentColor = IncomingAmber700
                        )
                    }
                    if (state.products.isNotEmpty()) {
                        IncomingCompactChip(text = "商品 ${state.products.size}件")
                    }
                    if (state.workingScheduleIds.isNotEmpty()) {
                        IncomingCompactChip(
                            text = "作業中 ${state.workingScheduleIds.size}件",
                            background = IncomingBadgeGreen.copy(alpha = 0.12f),
                            border = IncomingBadgeGreen.copy(alpha = 0.35f),
                            contentColor = IncomingBadgeGreen
                        )
                    }
                }
                HorizontalDivider(thickness = 2.dp, color = IncomingDividerGold)
            }
        }
    ) { padding ->
        when {
            state.selectedWarehouse == null && state.isLoadingWarehouses -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = IncomingAccentOrange)
                }
            }

            state.selectedWarehouse == null && state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = IncomingTitleRed,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Button(
                            onClick = { viewModel.initializeDefaultWarehouse(force = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = IncomingAccentOrange)
                        ) {
                            Text("再試行")
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (isPortrait) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = "検索（JANコード / 商品コード / 商品名）",
                                        color = IncomingReadonlyText
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = IncomingAccentOrange)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { }),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = IncomingPanelBg,
                                    unfocusedContainerColor = IncomingPanelBg,
                                    focusedBorderColor = IncomingAccentOrange,
                                    unfocusedBorderColor = IncomingNeutral300
                                )
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SearchCameraButton(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    label = "JAN(IN)",
                                    icon = Icons.Default.PhotoCamera,
                                    tint = IncomingBadgeGreen,
                                    background = IncomingBadgeGreen.copy(alpha = 0.12f),
                                    border = IncomingBadgeGreen.copy(alpha = 0.35f),
                                    onClick = {
                                        isJanScannerInCamera = true
                                        showJanScannerDialog = true
                                    }
                                )
                                SearchCameraButton(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    label = "JAN(OUT)",
                                    icon = Icons.Default.PhotoCamera,
                                    tint = IncomingTitleRed,
                                    background = IncomingTitleRed.copy(alpha = 0.08f),
                                    border = IncomingTitleRed.copy(alpha = 0.24f),
                                    onClick = {
                                        isJanScannerInCamera = false
                                        showJanScannerDialog = true
                                    }
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                placeholder = {
                                    Text(
                                        text = "検索（JANコード / 商品コード / 商品名）",
                                        color = IncomingReadonlyText
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = IncomingAccentOrange)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { }),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = IncomingPanelBg,
                                    unfocusedContainerColor = IncomingPanelBg,
                                    focusedBorderColor = IncomingAccentOrange,
                                    unfocusedBorderColor = IncomingNeutral300
                                )
                            )
                            Row(
                                modifier = Modifier.size(width = 248.dp, height = 56.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SearchCameraButton(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    label = "JAN(IN)",
                                    icon = Icons.Default.PhotoCamera,
                                    tint = IncomingBadgeGreen,
                                    background = IncomingBadgeGreen.copy(alpha = 0.12f),
                                    border = IncomingBadgeGreen.copy(alpha = 0.35f),
                                    onClick = {
                                        isJanScannerInCamera = true
                                        showJanScannerDialog = true
                                    }
                                )
                                SearchCameraButton(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    label = "JAN(OUT)",
                                    icon = Icons.Default.PhotoCamera,
                                    tint = IncomingTitleRed,
                                    background = IncomingTitleRed.copy(alpha = 0.08f),
                                    border = IncomingTitleRed.copy(alpha = 0.24f),
                                    onClick = {
                                        isJanScannerInCamera = false
                                        showJanScannerDialog = true
                                    }
                                )
                            }
                        }
                    }

                    when {
                        state.isLoadingProducts || (state.isSearching && state.products.isEmpty()) -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = IncomingAccentOrange)
                            }
                        }

                        state.products.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("入荷予定がありません", color = IncomingReadonlyText)
                            }
                        }

                        else -> {
                            if (isPortrait) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(state.products, key = { it.itemId }) { product ->
                                        ProductCard(
                                            product = product,
                                            isWorking = product.schedules.any { it.id in state.workingScheduleIds },
                                            onClick = {
                                                viewModel.selectProduct(product)
                                                onProductSelected()
                                            }
                                        )
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    gridItems(
                                        state.products,
                                        key = { product -> product.itemId }
                                    ) { product ->
                                        ProductCard(
                                            product = product,
                                            isWorking = product.schedules.any { it.id in state.workingScheduleIds },
                                            onClick = {
                                                viewModel.selectProduct(product)
                                                onProductSelected()
                                            }
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

@Composable
private fun SearchCameraButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    tint: Color,
    background: Color,
    border: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = BorderStroke(1.dp, border),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: IncomingProduct,
    isWorking: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isWorking) 2.dp else 1.dp,
            color = if (isWorking) IncomingBadgeGreen else IncomingNeutral200
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.itemName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomingTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "JAN ${product.primaryJanCode ?: "-"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomingNeutral500
                    )
                    Text(
                        text = buildString {
                            append("商品コード ${product.itemCode}")
                            val specLine = listOfNotNull(product.fullVolume, product.temperatureType)
                                .joinToString(" / ")
                            if (specLine.isNotBlank()) {
                                append(" / ")
                                append(specLine)
                            }
                        },
                        fontSize = 13.sp,
                        color = IncomingNeutral500
                    )
                }
                if (isWorking) {
                    Surface(
                        color = IncomingBadgeGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "作業中",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomingBadgeGreen
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IncomingMetricBadge(
                    label = "予定",
                    value = product.totalExpectedQuantity,
                    background = IncomingAmber50,
                    contentColor = IncomingAmber700
                )
                IncomingMetricBadge(
                    label = "済",
                    value = product.totalReceivedQuantity,
                    background = IncomingBadgeGreen.copy(alpha = 0.14f),
                    contentColor = IncomingBadgeGreen
                )
                IncomingMetricBadge(
                    label = "残",
                    value = product.totalRemainingQuantity,
                    background = IncomingAccentOrange.copy(alpha = 0.14f),
                    contentColor = IncomingAccentOrange
                )
            }
        }
    }
}
