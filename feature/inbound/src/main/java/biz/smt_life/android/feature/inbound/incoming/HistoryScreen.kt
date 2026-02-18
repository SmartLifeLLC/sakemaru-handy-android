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
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.IncomingWorkStatus
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: IncomingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProductList: () -> Unit,
    onNavigateToInput: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

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
                f3 = FunctionKeyAction("リスト", onNavigateToProductList)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Text(
                text = "本日の入庫履歴",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            when {
                state.isLoadingHistory -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.historyItems.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("本日の入庫履歴はありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.historyItems) { workItem ->
                            HistoryCard(
                                workItem = workItem,
                                onClick = {
                                    if (isEditable(workItem)) {
                                        viewModel.selectHistoryItem(workItem)
                                        onNavigateToInput()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Error snackbar
        state.errorMessage?.let { message ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) { Text("閉じる") }
                }
            ) { Text(message) }
        }
    }
}

private fun isEditable(workItem: IncomingWorkItem): Boolean {
    if (workItem.status == IncomingWorkStatus.CANCELLED) return false
    // Schedule status check would need to be done with additional data
    // For now, allow editing of WORKING and COMPLETED items
    return workItem.status == IncomingWorkStatus.WORKING || workItem.status == IncomingWorkStatus.COMPLETED
}

@Composable
private fun HistoryCard(
    workItem: IncomingWorkItem,
    onClick: () -> Unit
) {
    val editable = isEditable(workItem)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (editable) 1f else 0.6f)
            .then(if (editable) Modifier.clickable(onClick = onClick) else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = if (editable) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Schedule info
                val schedule = workItem.schedule
                if (schedule != null) {
                    Text(
                        text = "JAN: -  Code: ${schedule.itemCode}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = schedule.itemName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = schedule.warehouseName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "入庫: ${workItem.workArrivalDate}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    WorkStatusBadge(workItem.status)
                }
            }

            // Quantity
            Text(
                text = "${workItem.workQuantity}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun WorkStatusBadge(status: IncomingWorkStatus) {
    val (text, color) = when (status) {
        IncomingWorkStatus.WORKING -> "作業中" to MaterialTheme.colorScheme.tertiary
        IncomingWorkStatus.COMPLETED -> "完了" to MaterialTheme.colorScheme.primary
        IncomingWorkStatus.CANCELLED -> "キャンセル" to MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
