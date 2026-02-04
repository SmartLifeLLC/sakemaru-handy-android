package biz.smt_life.android.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

    // Listen for logout event
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
        is MainUiState.Loading -> {
            LoadingContent(modifier = modifier)
        }

        is MainUiState.Ready -> {
            ReadyContent(
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
        }

        is MainUiState.Error -> {
            ErrorContent(
                message = state.message,
                onRetry = onRetry,
                modifier = modifier
            )
        }
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

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("ログアウト") },
            text = { Text("ログアウトしますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text("ログアウト")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val commonWidth = 400.dp

        // Header with warehouse section and settings icon
        Box(
            modifier = Modifier.width(commonWidth),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = warehouse.name,
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(
                onClick = onNavigateToWarehouseSettings,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "倉庫設定"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main menu buttons - 2x2 grid + 1 centered
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: 入庫, 出庫, 移動
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                MenuButton(
                    label = "入庫",
                    topBorderColor = Color(0xFF2196F3), // Blue
                    onClick = onNavigateToInbound,
                    modifier = Modifier.size(200.dp, 100.dp)
                )

                MenuButton(
                    label = "出庫",
                    topBorderColor = Color(0xFFE91E63), // Pink/Red
                    onClick = onNavigateToOutbound,
                    modifier = Modifier.size(200.dp, 100.dp)
                )

                MenuButton(
                    label = "移動",
                    topBorderColor = Color(0xFF9C27B0), // Purple
                    onClick = onNavigateToMove,
                    modifier = Modifier.size(200.dp, 100.dp)
                )
            }

            // Row 2: 棚卸, ロケ検索
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                MenuButton(
                    label = "棚卸",
                    topBorderColor = Color(0xFFFF9800), // Orange
                    onClick = onNavigateToInventory,
                    modifier = Modifier.size(200.dp, 100.dp)
                )

                MenuButton(
                    label = "ロケ検索",
                    topBorderColor = Color(0xFF607D8B), // Blue Grey
                    onClick = onNavigateToLocationSearch,
                    modifier = Modifier.size(200.dp, 100.dp)
                )
            }
        }

        // Bottom info section
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text = "$pickerName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$appVersion ($hostUrl)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "ログアウト"
                    )
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
 * Menu button with colored top border accent.
 */
@Composable
private fun MenuButton(
    label: String,
    topBorderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .border(1.dp, Color.LightGray, shape),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = shape
    ) {
        Column {
            // Top colored border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(topBorderColor)
            )

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Preview(showBackground = true)
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

@Preview(showBackground = true)
@Composable
private fun MainScreenReadyPreview() {
    HandyTheme {
        MainScreen(
            state = MainUiState.Ready(
                pickerCode = "worker01",
                pickerName = "Warehouse Worker",
                warehouse = Warehouse("001", "東京倉庫"),
                pendingCounts = PendingCounts(5, 12, 3),
                currentDate = "2024/10/07 Mon",
                hostUrl = "https://handy.click",
                appVersion = "Ver.1.1.1"
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

@Preview(showBackground = true)
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
