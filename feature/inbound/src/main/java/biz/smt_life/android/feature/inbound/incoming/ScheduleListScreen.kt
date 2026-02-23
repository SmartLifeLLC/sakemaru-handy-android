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
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus

// ─── Color definitions ────────────────────────────────────────────────────────
private val AccentGreen  = Color(0xFF27AE60)
private val DarkGreen    = Color(0xFF1A7A4A)
private val BodyBg       = Color.White
private val HeaderBg     = Color(0xFFF0FFF4)
private val DividerGreen = Color(0xFFD5F5E3)
private val CardBorder   = Color(0xFFB2DFDB)
private val TextPrimary  = Color(0xFF212529)
private val TextSecond   = Color(0xFF555555)
private val DeleteRed    = Color(0xFFE74C3C)

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
                                text = "入庫処理 ｜ ${state.selectedWarehouse?.name ?: ""}",
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
                f3 = FunctionKeyAction("履歴", onNavigateToHistory),
                centerAligned = true
            )
        }
    ) { padding ->
        if (product == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("商品が選択されていません", color = TextSecond)
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
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = product.itemName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "JAN: ${product.janCodes.firstOrNull() ?: "-"}",
                            fontSize = 12.sp,
                            color = TextSecond
                        )
                        Text(
                            text = listOfNotNull(product.volume, product.temperatureType).joinToString(" / "),
                            fontSize = 12.sp,
                            color = TextSecond
                        )
                    }
                }
            }

            // Total quantities
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("合計予定", fontSize = 12.sp, color = TextSecond)
                            Text("${product.totalExpectedQuantity}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("入庫済", fontSize = 12.sp, color = TextSecond)
                            Text("${product.totalReceivedQuantity}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("残", fontSize = 12.sp, color = TextSecond)
                            Text(
                                "${product.totalRemainingQuantity}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
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

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isSelectable) 1f else 0.5f)
            .then(if (isSelectable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelectable) 2.dp else 1.dp,
            color = if (isSelectable) AccentGreen else CardBorder
        ),
        elevation = CardDefaults.outlinedCardElevation(
            defaultElevation = if (isSelectable) 2.dp else 0.dp
        )
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
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    if (!isSelectable) {
                        ScheduleStatusBadge(schedule.status)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${schedule.expectedArrivalDate}",
                    fontSize = 12.sp,
                    color = TextSecond
                )
            }

            // Remaining quantity
            Surface(
                color = if (isSelectable) AccentGreen else Color(0xFFEEEEEE),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${schedule.remainingQuantity}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelectable) Color.White else TextSecond
                )
            }
        }
    }
}

@Composable
private fun ScheduleStatusBadge(status: IncomingScheduleStatus) {
    val (text, color) = when (status) {
        IncomingScheduleStatus.CONFIRMED    -> "確定済" to AccentGreen
        IncomingScheduleStatus.TRANSMITTED  -> "連携済" to DarkGreen
        IncomingScheduleStatus.CANCELLED    -> "キャンセル" to DeleteRed
        else -> return
    }
    Surface(
        color = color,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
