package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biz.smt_life.android.core.domain.model.IncomingProduct

// ─── Color definitions ────────────────────────────────────────────────────────
private val AccentGreen  = Color(0xFF27AE60)
private val LightGreen   = Color(0xFF66BB6A)
private val BodyBg       = Color.White
private val HeaderBg     = Color(0xFFF0FFF4)
private val DividerGreen = Color(0xFFD5F5E3)
private val CardBorder   = Color(0xFFB2DFDB)
private val TextPrimary  = Color(0xFF212529)
private val TextSecond   = Color(0xFF555555)
private val ReadonlyText = Color(0xFF888888)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: IncomingViewModel,
    onNavigateBack: () -> Unit,
    onProductSelected: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.loadProducts()
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
                f2 = FunctionKeyAction("検索") { focusRequester.requestFocus() },
                f3 = FunctionKeyAction("履歴", onNavigateToHistory),
                centerAligned = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("検索（JANコード/商品名）", color = ReadonlyText) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AccentGreen)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* debounce handles it */ }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = CardBorder
                )
            )

            // Product list
            when {
                state.isSearching && state.products.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentGreen)
                    }
                }
                state.products.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("入荷予定がありません", color = ReadonlyText)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.products) { product ->
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
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // JAN code and item code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "JAN: ${product.janCodes.firstOrNull() ?: "-"}",
                    fontSize = 12.sp,
                    color = TextSecond
                )
                Text(
                    text = "Code: ${product.itemCode}",
                    fontSize = 12.sp,
                    color = TextSecond
                )
            }

            // Product name
            Text(
                text = product.itemName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Volume, temperature + badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listOfNotNull(product.volume, product.temperatureType).joinToString(" / "),
                    fontSize = 12.sp,
                    color = TextSecond
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 残 badge
                    Surface(
                        color = AccentGreen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "残: ${product.totalRemainingQuantity}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    // 済 badge
                    Surface(
                        color = LightGreen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "済: ${product.totalReceivedQuantity}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // 作業中 badge
            if (isWorking) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = AccentGreen,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "作業中",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
