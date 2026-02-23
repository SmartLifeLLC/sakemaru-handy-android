package biz.smt_life.android.feature.main

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.theme.HandyTheme
import biz.smt_life.android.core.domain.model.PendingCounts
import biz.smt_life.android.core.domain.model.Warehouse

@Composable
fun MainRoute(
    onNavigateToWarehouseSettings: () -> Unit,
    onNavigateToInbound: () -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect {
            onLogout()
        }
    }

    MainScreen(
        state = state,
        onNavigateToWarehouseSettings = onNavigateToWarehouseSettings,
        onNavigateToInbound = onNavigateToInbound,
        onNavigateToOutbound = onNavigateToOutbound,
        onNavigateToMove = onNavigateToMove,
        onNavigateToInventory = onNavigateToInventory,
        onNavigateToLocationSearch = onNavigateToLocationSearch,
        onLogoutClick = viewModel::logout,
        onRetry = viewModel::retry
    )
}

@Composable
fun MainScreen(
    state: MainUiState,
    onNavigateToWarehouseSettings: () -> Unit,
    onNavigateToInbound: () -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLogoutClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is MainUiState.Loading -> LoadingContent(modifier = modifier)

        is MainUiState.Ready -> ReadyContent(
            pickerCode = state.pickerCode,
            pickerName = state.pickerName,
            warehouse = state.warehouse,
            pendingCounts = state.pendingCounts,
            currentDate = state.currentDate,
            hostUrl = state.hostUrl,
            appVersion = state.appVersion,
            onNavigateToWarehouseSettings = onNavigateToWarehouseSettings,
            onNavigateToInbound = onNavigateToInbound,
            onNavigateToOutbound = onNavigateToOutbound,
            onNavigateToMove = onNavigateToMove,
            onNavigateToInventory = onNavigateToInventory,
            onNavigateToLocationSearch = onNavigateToLocationSearch,
            onLogoutClick = onLogoutClick,
            modifier = modifier
        )

        is MainUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = onRetry,
            modifier = modifier
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ReadyContent(
    pickerCode: String?,
    pickerName: String?,
    warehouse: Warehouse,
    pendingCounts: PendingCounts,
    currentDate: String,
    hostUrl: String,
    appVersion: String,
    onNavigateToWarehouseSettings: () -> Unit,
    onNavigateToInbound: () -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("ログアウト") },
            text = { Text("ログアウトしますか？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogoutClick()
                }) { Text("ログアウト") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("キャンセル") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ── ヘッダー (50dp) ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(WmsColor.HeaderBackground)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "メインメニュー",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "担当: ${pickerName ?: ""}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "ログアウト",
                        tint = Color.White
                    )
                }
            }
        }

        // ── ボディ ────────────────────────────────────────────────
        if (isPortrait) {
            // Portrait: 倉庫情報 + 1列メニュー
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // ── 倉庫情報バー ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 日付
                        Text(
                            text = currentDate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        // 倉庫名ボックス
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(WmsColor.WarehouseBoxBg)
                                .border(1.dp, WmsColor.WarehouseBoxBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = WmsColor.WarehouseBoxText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = warehouse.name,
                                color = WmsColor.WarehouseBoxText,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                        // 倉庫変更ボタン
                        OutlinedButton(
                            onClick = onNavigateToWarehouseSettings,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 10.dp, vertical = 4.dp
                            )
                        ) {
                            Text("倉庫変更", fontSize = 12.sp)
                        }
                    }
                    // システム情報
                    Text(
                        text = "Ver: $appVersion  |  URL: $hostUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = WmsColor.SystemInfoText,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // ── メニューカード ────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                MenuCard(
                    label = "入庫処理（00）",
                    bottomBorderColor = WmsColor.InboundBorder,
                    circleBg = WmsColor.InboundCircleBg,
                    iconColor = WmsColor.InboundIcon,
                    icon = Icons.Default.ArrowDownward,
                    onClick = onNavigateToInbound,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                MenuCard(
                    label = "出庫処理(00)",
                    bottomBorderColor = WmsColor.OutboundBorder,
                    circleBg = WmsColor.OutboundCircleBg,
                    iconColor = WmsColor.OutboundIcon,
                    icon = Icons.Default.ArrowUpward,
                    onClick = onNavigateToOutbound,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                MenuCard(
                    label = "移動処理",
                    bottomBorderColor = WmsColor.MoveBorder,
                    circleBg = WmsColor.MoveCircleBg,
                    iconColor = WmsColor.MoveIcon,
                    icon = Icons.Default.SwapHoriz,
                    onClick = onNavigateToMove,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                MenuCard(
                    label = "棚卸処理(00)",
                    bottomBorderColor = WmsColor.InventoryBorder,
                    circleBg = WmsColor.InventoryCircleBg,
                    iconColor = WmsColor.InventoryIcon,
                    icon = Icons.Default.ListAlt,
                    onClick = onNavigateToInventory,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                MenuCard(
                    label = "ロケ検索",
                    bottomBorderColor = WmsColor.LocationBorder,
                    circleBg = WmsColor.LocationCircleBg,
                    iconColor = WmsColor.LocationIcon,
                    icon = Icons.Default.LocationOn,
                    onClick = onNavigateToLocationSearch,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                }
            }
        } else {
            // Landscape: サイドバー + メニューグリッド
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // ── サイドバー (280dp) ──────────────────────────────
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // 日付
                        Text(
                            text = currentDate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 倉庫名ボックス
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(WmsColor.WarehouseBoxBg)
                                .border(1.dp, WmsColor.WarehouseBoxBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = WmsColor.WarehouseBoxText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = warehouse.name,
                                color = WmsColor.WarehouseBoxText,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // 倉庫変更ボタン
                        OutlinedButton(
                            onClick = onNavigateToWarehouseSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("倉庫変更")
                        }
                    }

                    // システム情報（下部）
                    Column {
                        Text(
                            text = "Ver: $appVersion",
                            style = MaterialTheme.typography.bodySmall,
                            color = WmsColor.SystemInfoText
                        )
                        Text(
                            text = "URL: $hostUrl",
                            style = MaterialTheme.typography.bodySmall,
                            color = WmsColor.SystemInfoText
                        )
                    }
                }

                // ── メニューグリッド ─────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: 入庫処理, 出庫処理
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MenuCard(
                            label = "入庫処理（00）",
                            bottomBorderColor = WmsColor.InboundBorder,
                            circleBg = WmsColor.InboundCircleBg,
                            iconColor = WmsColor.InboundIcon,
                            icon = Icons.Default.ArrowDownward,
                            onClick = onNavigateToInbound,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        MenuCard(
                            label = "出庫処理(00)",
                            bottomBorderColor = WmsColor.OutboundBorder,
                            circleBg = WmsColor.OutboundCircleBg,
                            iconColor = WmsColor.OutboundIcon,
                            icon = Icons.Default.ArrowUpward,
                            onClick = onNavigateToOutbound,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }

                    // Row 2: 移動処理, 棚卸処理, ロケ検索
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MenuCard(
                            label = "移動処理",
                            bottomBorderColor = WmsColor.MoveBorder,
                            circleBg = WmsColor.MoveCircleBg,
                            iconColor = WmsColor.MoveIcon,
                            icon = Icons.Default.SwapHoriz,
                            onClick = onNavigateToMove,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        MenuCard(
                            label = "棚卸処理(00)",
                            bottomBorderColor = WmsColor.InventoryBorder,
                            circleBg = WmsColor.InventoryCircleBg,
                            iconColor = WmsColor.InventoryIcon,
                            icon = Icons.Default.ListAlt,
                            onClick = onNavigateToInventory,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        MenuCard(
                            label = "ロケ検索",
                            bottomBorderColor = WmsColor.LocationBorder,
                            circleBg = WmsColor.LocationCircleBg,
                            iconColor = WmsColor.LocationIcon,
                            icon = Icons.Default.LocationOn,
                            onClick = onNavigateToLocationSearch,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "エラー: $message",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("再試行")
        }
    }
}

/**
 * メニューカード — 円形アイコン＋ラベル＋ボトムボーダーアクセント
 */
@Composable
private fun MenuCard(
    label: String,
    bottomBorderColor: Color,
    circleBg: Color,
    iconColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(15.dp)

    Surface(
        modifier = modifier.clip(shape),
        color = Color.White,
        shadowElevation = 4.dp,
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
        ) {
            // カードコンテンツ
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(circleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
            // ボトムボーダーアクセント
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(bottomBorderColor)
            )
        }
    }
}

// ========== Preview Section ==========

@Preview(
    name = "Main Screen - Loading",
    showBackground = true,
    widthDp = 400,
    heightDp = 700
)
@Composable
private fun MainScreenLoadingPreview() {
    HandyTheme {
        MainScreen(
            state = MainUiState.Loading,
            onNavigateToWarehouseSettings = {},
            onNavigateToInbound = {},
            onNavigateToOutbound = {},
            onNavigateToMove = {},
            onNavigateToInventory = {},
            onNavigateToLocationSearch = {},
            onLogoutClick = {},
            onRetry = {}
        )
    }
}

@Preview(
    name = "Main Screen - Ready",
    showBackground = true,
    widthDp = 400,
    heightDp = 700
)
@Composable
private fun MainScreenReadyPreview() {
    HandyTheme {
        MainScreen(
            state = MainUiState.Ready(
                pickerCode = "worker01",
                pickerName = "山田 太郎",
                warehouse = Warehouse("001", "酒丸本社倉庫"),
                pendingCounts = PendingCounts(5, 12, 3),
                currentDate = "2026/02/20(金)",
                hostUrl = "api.system-server.com",
                appVersion = "1.0.2"
            ),
            onNavigateToWarehouseSettings = {},
            onNavigateToInbound = {},
            onNavigateToOutbound = {},
            onNavigateToMove = {},
            onNavigateToInventory = {},
            onNavigateToLocationSearch = {},
            onLogoutClick = {},
            onRetry = {}
        )
    }
}

@Preview(
    name = "Main Screen - Error",
    showBackground = true,
    widthDp = 400,
    heightDp = 700
)
@Composable
private fun MainScreenErrorPreview() {
    HandyTheme {
        MainScreen(
            state = MainUiState.Error("Network connection failed"),
            onNavigateToWarehouseSettings = {},
            onNavigateToInbound = {},
            onNavigateToOutbound = {},
            onNavigateToMove = {},
            onNavigateToInventory = {},
            onNavigateToLocationSearch = {},
            onLogoutClick = {},
            onRetry = {}
        )
    }
}
