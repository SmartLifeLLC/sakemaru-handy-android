package biz.smt_life.android.feature.outbound.proxyshipment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.ProxyShipmentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyShipmentResultScreen(
    allocationId: Int,
    onNavigateToList: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: ProxyShipmentResultViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(allocationId) {
        viewModel.initialize(allocationId)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = BodyBg,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderBg),
                    title = {
                        Text(
                            text = "横持ち出荷結果",
                            fontWeight = FontWeight.ExtraBold,
                            color = TitleRed
                        )
                    }
                )
                HorizontalDivider(thickness = 2.dp, color = DividerGold)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(color = AccentOrange)
                state.completion == null -> Text("完了結果を取得できませんでした")
                else -> {
                    val completion = requireNotNull(state.completion)
                    val statusPalette = proxyShipmentStatusPalette(completion.allocation.status)
                    val icon = if (completion.allocation.status == ProxyShipmentStatus.FULFILLED) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.Inventory2
                    }

                    OutlinedCard(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                        border = BorderStroke(2.dp, statusPalette.content.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = statusPalette.container,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = statusPalette.content,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            Text(
                                text = completion.allocation.status.label,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusPalette.content
                            )

                            Text(
                                text = completion.allocation.item.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "商品コード ${completion.allocation.item.code}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Neutral500
                            )

                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                color = Amber50,
                                border = BorderStroke(1.dp, Amber200)
                            ) {
                                Text(
                                    text = "実績数 ${completion.allocation.pickedQty} / ${completion.allocation.assignQty} ${completion.allocation.assignQtyType.label}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AccentOrange,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }

                            Text(
                                text = "${completion.allocation.pickupWarehouse.name} -> ${completion.allocation.destinationWarehouse.name}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF333333)
                            )

                            if (completion.stockTransferQueueId != null) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                    color = Color(0xFFE3F2FD),
                                    border = BorderStroke(1.dp, Color(0xFF90CAF9))
                                ) {
                                    Text(
                                        text = "移動伝票キュー ID ${completion.stockTransferQueueId}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1565C0),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "移動伝票は作成されていません",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ReadonlyText
                                )
                            }

                            Button(
                                onClick = onNavigateToList,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                            ) {
                                Text("一覧へ戻る", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            TextButton(
                                onClick = onNavigateToMain,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("メニューへ戻る", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TitleRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
