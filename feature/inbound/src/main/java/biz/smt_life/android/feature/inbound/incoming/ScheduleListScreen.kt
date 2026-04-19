package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus

@Composable
fun ScheduleListScreen(
    viewModel: IncomingViewModel,
    onNavigateBack: () -> Unit,
    onScheduleSelected: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val product = state.selectedProduct

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
                        Text("商品一覧", color = IncomingTitleRed, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = product?.itemName ?: "入荷スケジュール",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomingTitleRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Filled.History, contentDescription = "履歴", tint = IncomingAccentOrange)
                        Text("履歴", color = IncomingAccentOrange, fontWeight = FontWeight.Bold)
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
                            text = "作業倉庫 ${warehouse.name}",
                            background = IncomingAmber50,
                            border = IncomingAmber200,
                            contentColor = IncomingAmber700
                        )
                    }
                    product?.let {
                        IncomingCompactChip(text = "スケジュール ${it.schedules.size}件")
                    }
                }
                HorizontalDivider(thickness = 2.dp, color = IncomingDividerGold)
            }
        }
    ) { padding ->
        if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("商品が選択されていません", color = IncomingReadonlyText)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ProductSummaryCard(product = product)
            }

            items(product.schedules, key = { it.id }) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    workingWarehouseName = state.selectedWarehouse?.name.orEmpty(),
                    isVirtualWarehouse = state.selectedWarehouse?.id != schedule.warehouseId,
                    onClick = {
                        if (schedule.status.canStartWork) {
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
private fun ProductSummaryCard(product: IncomingProduct) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, IncomingNeutral200)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = product.itemName,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = IncomingTextPrimary
            )
            Text(
                text = "JAN ${product.primaryJanCode ?: "-"} / 商品コード ${product.itemCode}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = IncomingNeutral500
            )
            val specLine = listOfNotNull(product.fullVolume, product.temperatureType).joinToString(" / ")
            if (specLine.isNotBlank()) {
                Text(
                    text = specLine,
                    fontSize = 13.sp,
                    color = IncomingNeutral500
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IncomingMetricBadge("予定", product.totalExpectedQuantity, IncomingAmber50, IncomingAmber700)
                IncomingMetricBadge("済", product.totalReceivedQuantity, IncomingBadgeGreen.copy(alpha = 0.14f), IncomingBadgeGreen)
                IncomingMetricBadge("残", product.totalRemainingQuantity, IncomingAccentOrange.copy(alpha = 0.14f), IncomingAccentOrange)
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: IncomingSchedule,
    workingWarehouseName: String,
    isVirtualWarehouse: Boolean,
    onClick: () -> Unit
) {
    val isSelectable = schedule.status.canStartWork
    val locationLabel = schedule.location?.displayName ?: schedule.location?.fullDisplayName

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isSelectable) 1f else 0.6f)
            .then(if (isSelectable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelectable) 2.dp else 1.dp,
            color = if (isSelectable) IncomingAccentOrange else IncomingNeutral200
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isVirtualWarehouse) {
                        IncomingCompactChip(
                            text = "作業倉庫 $workingWarehouseName",
                            background = IncomingWarningOrange.copy(alpha = 0.12f),
                            border = IncomingWarningOrange.copy(alpha = 0.35f),
                            contentColor = IncomingWarningOrange
                        )
                    }
                    Text(
                        text = "入荷対象倉庫 ${schedule.warehouseName.orEmpty()}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomingTextPrimary
                    )
                    schedule.expectedArrivalDate?.takeIf { it.isNotBlank() }?.let {
                        Text("入荷予定日 $it", fontSize = 12.sp, color = IncomingNeutral500)
                    }
                    locationLabel?.takeIf { it.isNotBlank() }?.let {
                        Text("既定ロケーション $it", fontSize = 12.sp, color = IncomingNeutral500)
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScheduleStatusBadge(schedule.status)
                    Surface(
                        color = if (isSelectable) IncomingAccentOrange.copy(alpha = 0.14f) else IncomingNeutral200,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "残 ${schedule.remainingQuantity}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelectable) IncomingAccentOrange else IncomingNeutral500
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IncomingMetricBadge("予定", schedule.expectedQuantity, IncomingAmber50, IncomingAmber700)
                IncomingMetricBadge("済", schedule.receivedQuantity, IncomingBadgeGreen.copy(alpha = 0.14f), IncomingBadgeGreen)
            }
        }
    }
}

@Composable
private fun ScheduleStatusBadge(status: IncomingScheduleStatus) {
    val (text, color) = when (status) {
        IncomingScheduleStatus.PENDING -> "未入荷" to IncomingAccentOrange
        IncomingScheduleStatus.PARTIAL -> "一部入荷" to IncomingWarningOrange
        IncomingScheduleStatus.CONFIRMED -> "確定済" to IncomingBadgeGreen
        IncomingScheduleStatus.TRANSMITTED -> "連携済" to IncomingNeutral500
        IncomingScheduleStatus.CANCELLED -> "キャンセル" to IncomingWarningRed
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
