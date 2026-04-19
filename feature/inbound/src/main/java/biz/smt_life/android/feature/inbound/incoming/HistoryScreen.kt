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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import biz.smt_life.android.core.domain.model.IncomingWorkItem

@Composable
fun HistoryScreen(
    viewModel: IncomingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProductList: () -> Unit,
    onNavigateToInput: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
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
                        Text("戻る", color = IncomingTitleRed, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "本日の入荷履歴",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomingTitleRed
                    )
                    TextButton(onClick = onNavigateToProductList) {
                        Icon(Icons.Filled.History, contentDescription = "商品一覧", tint = IncomingAccentOrange)
                        Text("商品一覧", color = IncomingAccentOrange, fontWeight = FontWeight.Bold)
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
                    if (state.historyItems.isNotEmpty()) {
                        IncomingCompactChip(text = "履歴 ${state.historyItems.size}件")
                    }
                }
                HorizontalDivider(thickness = 2.dp, color = IncomingDividerGold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoadingHistory -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = IncomingAccentOrange)
                    }
                }

                state.historyItems.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("本日の入荷履歴はありません", color = IncomingReadonlyText)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.historyItems, key = { it.id }) { workItem ->
                            HistoryCard(
                                workItem = workItem,
                                onClick = {
                                    if (viewModel.selectHistoryItem(workItem)) {
                                        onNavigateToInput()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        state.errorMessage?.let { message ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = viewModel::clearError) {
                        Text("閉じる", color = IncomingAccentOrange)
                    }
                }
            ) {
                Text(message)
            }
        }
    }
}

@Composable
private fun HistoryCard(
    workItem: IncomingWorkItem,
    onClick: () -> Unit
) {
    val editable = workItem.canEditFromHistory
    val schedule = workItem.schedule

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (editable) 1f else 0.6f)
            .then(if (editable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (editable) 2.dp else 1.dp,
            color = if (editable) IncomingAccentOrange else IncomingNeutral200
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
                        text = schedule?.itemName.orEmpty(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomingTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "JAN ${schedule?.primaryJanCode ?: "-"} / 商品コード ${schedule?.itemCode.orEmpty()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomingNeutral500
                    )
                    Text(
                        text = "入荷対象倉庫 ${schedule?.warehouseName.orEmpty()}",
                        fontSize = 12.sp,
                        color = IncomingNeutral500
                    )
                    Text(
                        text = "入荷日 ${workItem.workArrivalDate.orEmpty()}",
                        fontSize = 12.sp,
                        color = IncomingNeutral500
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HistoryStatusBadge(workItem.status.name)
                    Surface(
                        color = IncomingAccentOrange.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${workItem.workQuantity}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IncomingAccentOrange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryStatusBadge(status: String) {
    val (text, color) = when (status.uppercase()) {
        "WORKING" -> "作業中" to IncomingWarningOrange
        "COMPLETED" -> "完了" to IncomingBadgeGreen
        "CANCELLED" -> "キャンセル" to IncomingWarningRed
        else -> status to IncomingNeutral500
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
