package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.IncomingWorkStatus
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus

// ─── Color definitions ────────────────────────────────────────────────────────
private val AccentGreen  = Color(0xFF27AE60)
private val DarkGreen    = Color(0xFF1A7A4A)
private val BodyBg       = Color.White
private val HeaderBg     = Color(0xFFF0FFF4)
private val DividerGreen = Color(0xFFD5F5E3)
private val HistCardBg   = Color(0xFFF0FFF4)
private val HistCardBdr  = Color(0xFFA5D6A7)
private val TextPrimary  = Color(0xFF212529)
private val TextSecond   = Color(0xFF555555)
private val ReadonlyText = Color(0xFF888888)
private val DeleteRed    = Color(0xFFE74C3C)

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
                f2 = FunctionKeyAction("戻る", onNavigateBack),
                f3 = FunctionKeyAction("リスト", onNavigateToProductList),
                centerAligned = true
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
                text = "本日の入荷履歴",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGreen
            )

            when {
                state.isLoadingHistory -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentGreen)
                    }
                }
                state.historyItems.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("本日の入荷履歴はありません", color = ReadonlyText)
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
                                },
                                onPrint = {},
                                onDelete = {}
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
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("閉じる", color = AccentGreen)
                    }
                }
            ) { Text(message) }
        }
    }
}

private fun isEditable(workItem: IncomingWorkItem): Boolean {
    if (workItem.status == IncomingWorkStatus.CANCELLED) return false
    return workItem.status == IncomingWorkStatus.WORKING || workItem.status == IncomingWorkStatus.COMPLETED
}

@Composable
private fun HistoryCard(
    workItem: IncomingWorkItem,
    onClick: () -> Unit,
    onPrint: () -> Unit,
    onDelete: () -> Unit
) {
    val editable = isEditable(workItem)

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (editable) 1f else 0.6f)
            .then(if (editable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, HistCardBdr),
        colors = CardDefaults.outlinedCardColors(containerColor = HistCardBg),
        elevation = CardDefaults.outlinedCardElevation(
            defaultElevation = if (editable) 1.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val schedule = workItem.schedule
                    if (schedule != null) {
                        Text(
                            text = "JAN: -  Code: ${schedule.itemCode}",
                            fontSize = 11.sp,
                            color = TextSecond
                        )
                        Text(
                            text = schedule.itemName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Text(
                            text = schedule.warehouseName,
                            fontSize = 12.sp,
                            color = TextSecond
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "入荷: ${workItem.workArrivalDate}",
                            fontSize = 11.sp,
                            color = TextSecond
                        )
                        WorkStatusBadge(workItem.status)
                    }
                }

                // Quantity
                Text(
                    text = "${workItem.workQuantity}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrint,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    border = BorderStroke(1.dp, AccentGreen)
                ) {
                    Text("印刷", fontSize = 12.sp, color = AccentGreen)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onDelete,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeleteRed)
                ) {
                    Text("削除", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun WorkStatusBadge(status: IncomingWorkStatus) {
    val (text, color) = when (status) {
        IncomingWorkStatus.WORKING   -> "作業中" to AccentGreen
        IncomingWorkStatus.COMPLETED -> "完了" to DarkGreen
        IncomingWorkStatus.CANCELLED -> "キャンセル" to DeleteRed
    }
    Surface(
        color = color,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
