package biz.smt_life.android.feature.main

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun MainRoute(
    onNavigateToWarehouseSettings: () -> Unit,
    onNavigateToInbound: () -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToProxyShipment: () -> Unit,
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
        onNavigateToProxyShipment = onNavigateToProxyShipment,
        onNavigateToMove = onNavigateToMove,
        onNavigateToInventory = onNavigateToInventory,
        onNavigateToLocationSearch = onNavigateToLocationSearch,
        onLogoutClick = viewModel::logout,
        onRetry = viewModel::retry,
        onOpenWarehouseDialog = viewModel::openWarehouseDialog,
        onDismissWarehouseDialog = viewModel::dismissWarehouseDialog,
        onSelectWarehouse = viewModel::selectWarehouse,
        onOpenDatePicker = viewModel::openDatePicker,
        onDismissDatePicker = viewModel::dismissDatePicker,
        onSelectShippingDate = viewModel::selectShippingDate
    )
}

@Composable
fun MainScreen(
    state: MainUiState,
    onNavigateToWarehouseSettings: () -> Unit,
    onNavigateToInbound: () -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToProxyShipment: () -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLogoutClick: () -> Unit,
    onRetry: () -> Unit,
    onOpenWarehouseDialog: () -> Unit = {},
    onDismissWarehouseDialog: () -> Unit = {},
    onSelectWarehouse: (biz.smt_life.android.core.domain.model.IncomingWarehouse) -> Unit = {},
    onOpenDatePicker: () -> Unit = {},
    onDismissDatePicker: () -> Unit = {},
    onSelectShippingDate: (String) -> Unit = {},
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
            shippingDate = state.shippingDate,
            hostUrl = state.hostUrl,
            appVersion = state.appVersion,
            showWarehouseDialog = state.showWarehouseDialog,
            availableWarehouses = state.availableWarehouses,
            isLoadingWarehouses = state.isLoadingWarehouses,
            showDatePicker = state.showDatePicker,
            onOpenWarehouseDialog = onOpenWarehouseDialog,
            onDismissWarehouseDialog = onDismissWarehouseDialog,
            onSelectWarehouse = onSelectWarehouse,
            onOpenDatePicker = onOpenDatePicker,
            onDismissDatePicker = onDismissDatePicker,
            onSelectShippingDate = onSelectShippingDate,
            onNavigateToInbound = onNavigateToInbound,
            onNavigateToOutbound = onNavigateToOutbound,
            onNavigateToProxyShipment = onNavigateToProxyShipment,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyContent(
    pickerCode: String?,
    pickerName: String?,
    warehouse: Warehouse,
    pendingCounts: PendingCounts,
    currentDate: String,
    shippingDate: String,
    hostUrl: String,
    appVersion: String,
    showWarehouseDialog: Boolean = false,
    availableWarehouses: List<biz.smt_life.android.core.domain.model.IncomingWarehouse> = emptyList(),
    isLoadingWarehouses: Boolean = false,
    showDatePicker: Boolean = false,
    onOpenWarehouseDialog: () -> Unit = {},
    onDismissWarehouseDialog: () -> Unit = {},
    onSelectWarehouse: (biz.smt_life.android.core.domain.model.IncomingWarehouse) -> Unit = {},
    onOpenDatePicker: () -> Unit = {},
    onDismissDatePicker: () -> Unit = {},
    onSelectShippingDate: (String) -> Unit = {},
    onNavigateToInbound: () -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToProxyShipment: () -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    // ===== Orientation control (shared with P21) =====
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences("p21_orientation_prefs", Context.MODE_PRIVATE) }
    var isPortrait by remember { mutableStateOf(prefs.getBoolean("p21_is_portrait", false)) }

    LaunchedEffect(isPortrait) {
        activity?.requestedOrientation = if (isPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    fun toggleOrientation() {
        isPortrait = !isPortrait
        prefs.edit().putBoolean("p21_is_portrait", isPortrait).apply()
    }

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

    if (showWarehouseDialog) {
        AlertDialog(
            onDismissRequest = onDismissWarehouseDialog,
            title = { Text("倉庫変更") },
            text = {
                if (isLoadingWarehouses) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (availableWarehouses.isEmpty()) {
                    Text("倉庫が見つかりません")
                } else {
                    val useTwoColumns = !isPortrait && availableWarehouses.size > 1

                    if (useTwoColumns) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            contentPadding = PaddingValues(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableWarehouses, key = { it.id }) { wh ->
                                WarehouseDialogItem(
                                    warehouse = wh,
                                    isSelected = wh.id.toString() == warehouse.id,
                                    onClick = { onSelectWarehouse(wh) }
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableWarehouses.forEach { wh ->
                                WarehouseDialogItem(
                                    warehouse = wh,
                                    isSelected = wh.id.toString() == warehouse.id,
                                    onClick = { onSelectWarehouse(wh) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissWarehouseDialog) { Text("閉じる") }
            }
        )
    }

    if (showDatePicker) {
        val initialMillis = try {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
            sdf.parse(shippingDate)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = onDismissDatePicker,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        onSelectShippingDate(sdf.format(Date(millis)))
                    }
                }) { Text("決定") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDatePicker) { Text("キャンセル") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ── ボディ ────────────────────────────────────────────────
        if (isPortrait) {
            // Portrait: ヘッダーなし、倉庫情報 + ボタン行 + 1列メニュー
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
                    // 出荷日
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = WmsColor.HeaderBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "出荷日:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                        Text(
                            text = shippingDate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = WmsColor.HeaderBackground
                        )
                        OutlinedButton(
                            onClick = onOpenDatePicker,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 10.dp, vertical = 4.dp
                            )
                        ) {
                            Text("日付変更", fontSize = 12.sp)
                        }
                    }
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
                            onClick = onOpenWarehouseDialog,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 10.dp, vertical = 4.dp
                            )
                        ) {
                            Text("倉庫変更", fontSize = 12.sp)
                        }
                    }
                    // 担当者 + システム情報
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "担当: ${pickerName ?: ""}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Ver: $appVersion  |  URL: $hostUrl",
                            style = MaterialTheme.typography.bodySmall,
                            color = WmsColor.SystemInfoText
                        )
                    }
                }

                // ── 画面回転 | ログアウト ボタン行（固定） ────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { toggleOrientation() },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ScreenRotation,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("画面回転", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ログアウト", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
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
                    label = "入荷処理",
                    bottomBorderColor = WmsColor.InboundBorder,
                    circleBg = WmsColor.InboundCircleBg,
                    iconColor = WmsColor.InboundIcon,
                    icon = Icons.Default.ArrowDownward,
                    onClick = onNavigateToInbound,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    showIcon = false
                )
                MenuCard(
                    label = "出荷処理",
                    bottomBorderColor = WmsColor.OutboundBorder,
                    circleBg = WmsColor.OutboundCircleBg,
                    iconColor = WmsColor.OutboundIcon,
                    icon = Icons.Default.ArrowUpward,
                    onClick = onNavigateToOutbound,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    showIcon = false
                )
                MenuCard(
                    label = "横持出荷",
                    bottomBorderColor = WmsColor.ProxyShipmentBorder,
                    circleBg = WmsColor.ProxyShipmentCircleBg,
                    iconColor = WmsColor.ProxyShipmentIcon,
                    icon = Icons.Default.LocalShipping,
                    onClick = onNavigateToProxyShipment,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    showIcon = false
                )
                MenuCard(
                    label = "移動処理",
                    bottomBorderColor = WmsColor.MoveBorder,
                    circleBg = WmsColor.MoveCircleBg,
                    iconColor = WmsColor.MoveIcon,
                    icon = Icons.Default.SwapHoriz,
                    onClick = onNavigateToMove,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    showIcon = false
                )
                MenuCard(
                    label = "棚卸処理",
                    bottomBorderColor = WmsColor.InventoryBorder,
                    circleBg = WmsColor.InventoryCircleBg,
                    iconColor = WmsColor.InventoryIcon,
                    icon = Icons.Default.ListAlt,
                    onClick = onNavigateToInventory,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    showIcon = false
                )
                MenuCard(
                    label = "ロケ検索",
                    bottomBorderColor = WmsColor.LocationBorder,
                    circleBg = WmsColor.LocationCircleBg,
                    iconColor = WmsColor.LocationIcon,
                    icon = Icons.Default.LocationOn,
                    onClick = onNavigateToLocationSearch,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    showIcon = false
                )
                }
            }
        } else {
            // Landscape: ヘッダー（担当・画面回転・ログアウト中央大きく） + サイドバー + メニューグリッド

            // ── ヘッダー ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(WmsColor.HeaderBackground)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "担当: ${pickerName ?: ""}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(24.dp))
                Surface(
                    onClick = { toggleOrientation() },
                    color = Color.Transparent,
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ScreenRotation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "画面回転",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Surface(
                    onClick = { showLogoutDialog = true },
                    color = Color.Transparent,
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ログアウト",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

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
                        Spacer(modifier = Modifier.height(8.dp))

                        // 出荷日
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = WmsColor.HeaderBackground,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "出荷日:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            Text(
                                text = shippingDate,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = WmsColor.HeaderBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onOpenDatePicker,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("日付変更")
                        }
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
                            onClick = onOpenWarehouseDialog,
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
                    // Row 1: 入荷処理, 出荷処理, 横持出荷
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MenuCard(
                            label = "入荷処理",
                            bottomBorderColor = WmsColor.InboundBorder,
                            circleBg = WmsColor.InboundCircleBg,
                            iconColor = WmsColor.InboundIcon,
                            icon = Icons.Default.ArrowDownward,
                            onClick = onNavigateToInbound,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        MenuCard(
                            label = "出荷処理",
                            bottomBorderColor = WmsColor.OutboundBorder,
                            circleBg = WmsColor.OutboundCircleBg,
                            iconColor = WmsColor.OutboundIcon,
                            icon = Icons.Default.ArrowUpward,
                            onClick = onNavigateToOutbound,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        MenuCard(
                            label = "横持出荷",
                            bottomBorderColor = WmsColor.ProxyShipmentBorder,
                            circleBg = WmsColor.ProxyShipmentCircleBg,
                            iconColor = WmsColor.ProxyShipmentIcon,
                            icon = Icons.Default.LocalShipping,
                            onClick = onNavigateToProxyShipment,
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
                            label = "棚卸処理",
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
private fun WarehouseDialogItem(
    warehouse: biz.smt_life.android.core.domain.model.IncomingWarehouse,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = warehouse.name,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "コード: ${warehouse.code}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
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
                if (showIcon) {
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
                }
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
            onNavigateToProxyShipment = {},
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
                currentDate = "2026/02/20",
                shippingDate = "2026/02/20",
                hostUrl = "api.system-server.com",
                appVersion = "1.0.2"
            ),
            onNavigateToWarehouseSettings = {},
            onNavigateToInbound = {},
            onNavigateToOutbound = {},
            onNavigateToProxyShipment = {},
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
            onNavigateToProxyShipment = {},
            onNavigateToMove = {},
            onNavigateToInventory = {},
            onNavigateToLocationSearch = {},
            onLogoutClick = {},
            onRetry = {}
        )
    }
}
