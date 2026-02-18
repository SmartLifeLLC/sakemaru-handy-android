package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    viewModel: IncomingViewModel,
    onNavigateBack: () -> Unit,
    onScheduleSelected: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val product = state.selectedProduct

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${state.selectedWarehouse?.name ?: ""} 入庫処理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        bottomBar = {
            FunctionKeyBar(
                f2 = FunctionKeyAction("戻る", onNavigateBack),
                f3 = FunctionKeyAction("履歴", onNavigateToHistory)
            )
        }
    ) { padding ->
        if (product == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("商品が選択されていません")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Product summary header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = product.itemName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "JAN: ${product.janCodes.firstOrNull() ?: "-"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = listOfNotNull(product.volume, product.temperatureType).joinToString(" / "),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Total quantities
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("合計予定", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${product.totalExpectedQuantity}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("入庫済", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${product.totalReceivedQuantity}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("残", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${product.totalRemainingQuantity}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Schedule items
            items(product.schedules) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    onClick = {
                        if (schedule.status.isSelectable) {
                            viewModel.selectSchedule(schedule)
                            onScheduleSelected()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: IncomingSchedule,
    onClick: () -> Unit
) {
    val isSelectable = schedule.status.isSelectable

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isSelectable) 1f else 0.5f)
            .then(if (isSelectable) Modifier.clickable(onClick = onClick) else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelectable) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = schedule.warehouseName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (!isSelectable) {
                        ScheduleStatusBadge(schedule.status)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${schedule.expectedArrivalDate}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Remaining quantity button
            Surface(
                color = if (isSelectable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "${schedule.remainingQuantity}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelectable) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScheduleStatusBadge(status: IncomingScheduleStatus) {
    val (text, color) = when (status) {
        IncomingScheduleStatus.CONFIRMED -> "確定済" to MaterialTheme.colorScheme.primary
        IncomingScheduleStatus.TRANSMITTED -> "連携済" to MaterialTheme.colorScheme.secondary
        IncomingScheduleStatus.CANCELLED -> "キャンセル" to MaterialTheme.colorScheme.error
        else -> return // PENDING and PARTIAL don't show badge
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 11.sp,
            color = color
        )
    }
}
