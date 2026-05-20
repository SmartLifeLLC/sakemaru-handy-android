package biz.smt_life.android.feature.stockdisposal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.StockDisposalItem

@Composable
fun StockDisposalRoute(
    onNavigateBack: () -> Unit,
    viewModel: StockDisposalViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.selectedItem != null) {
        StockDisposalInputScreen(
            state = state,
            onBack = viewModel::onBackToSearch,
            onCaseQuantityChange = viewModel::onCaseQuantityChange,
            onPieceQuantityChange = viewModel::onPieceQuantityChange,
            onReasonSelected = viewModel::onReasonSelected,
            onNoteChange = viewModel::onNoteChange,
            onSubmit = viewModel::onSubmit,
            onDismissSubmitResult = viewModel::onDismissSubmitResult
        )
    } else {
        StockDisposalSearchScreen(
            state = state,
            onNavigateBack = onNavigateBack,
            onSearchKeywordChange = viewModel::onSearchKeywordChange,
            onSearch = viewModel::onSearch,
            onBarcodeScan = viewModel::onBarcodeScan,
            onItemSelected = viewModel::onItemSelected,
            onDismissSubmitResult = viewModel::onDismissSubmitResult
        )
    }
}

@Composable
fun StockDisposalSearchScreen(
    state: StockDisposalUiState,
    onNavigateBack: () -> Unit,
    onSearchKeywordChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBarcodeScan: (String) -> Unit,
    onItemSelected: (StockDisposalItem) -> Unit,
    onDismissSubmitResult: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(state.submitSuccess) {
        if (state.submitSuccess) {
            snackbarHostState.showSnackbar("在庫調節を登録しました")
            onDismissSubmitResult()
        }
    }

    if (showScanner) {
        JanScannerDialog(
            onScan = { code ->
                showScanner = false
                onBarcodeScan(code)
            },
            onDismiss = { showScanner = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A2634))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "戻る",
                        tint = Color.White
                    )
                }
                Text(
                    text = "在庫調節",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "選択中の倉庫",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = state.warehouseName.ifBlank { "倉庫未設定" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A2634)
                    )
                    state.warehouseCode.takeIf { it.isNotBlank() }?.let { code ->
                        Text(
                            text = "コード: $code",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.searchKeyword,
                    onValueChange = onSearchKeywordChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("商品コード・名前で検索") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() })
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showScanner = true }) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "JANスキャン",
                        tint = Color(0xFF7B1FA2)
                    )
                }
                Button(
                    onClick = onSearch,
                    enabled = state.searchKeyword.isNotBlank() && !state.isSearching
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("検索")
                }
            }

            if (state.isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (state.hasSearched && state.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("該当する商品がありません", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.itemId }) { item ->
                        ItemRow(
                            item = item,
                            onItemSelected = onItemSelected
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: StockDisposalItem,
    onItemSelected: (StockDisposalItem) -> Unit
) {
    val realStock = item.stock.actualQuantity
    val theoreticalStock = item.stock.theoreticalQuantity
    val hasStock = realStock != 0 || theoreticalStock != 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemSelected(item) }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.itemCode,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A2634)
            )
            item.displayPackaging?.takeIf { it.isNotEmpty() }?.let { pkg ->
                Text(
                    text = pkg,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7B1FA2)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.displayName ?: item.itemName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        ItemSpecRow(item = item)

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StockQuantityText(
                label = "実在庫",
                quantity = realStock,
                modifier = Modifier.weight(1f),
                valueColor = if (hasStock) Color(0xFF1976D2) else Color.Gray
            )
            StockQuantityText(
                label = "理論在庫",
                quantity = theoreticalStock,
                modifier = Modifier.weight(1f),
                valueColor = Color(0xFF1A2634)
            )
        }
        if (!hasStock) {
            Text(
                text = "在庫なし",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ItemSpecRow(item: StockDisposalItem) {
    val specs = buildList {
        item.displayPackaging?.takeIf { it.isNotBlank() }?.let { add("容量 $it") }
        item.capacityCase?.takeIf { it > 0 }?.let { add("入数 $it") }
        item.capacityCarton?.takeIf { it > 0 }?.let { add("カートン $it") }
        item.orderJanCode?.takeIf { it.isNotBlank() }?.let { add("発注JAN $it") }
    }

    if (specs.isEmpty()) {
        return
    }

    Text(
        text = specs.joinToString("   "),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF7B1FA2),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun StockQuantityText(
    label: String,
    quantity: Int,
    modifier: Modifier = Modifier,
    valueColor: Color = Color(0xFF1A2634)
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
        Text(
            text = quantity.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
