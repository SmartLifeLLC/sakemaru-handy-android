package biz.smt_life.android.feature.outbound.proxyshipment

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.ProxyShipmentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyShipmentListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPicking: (shipmentDate: String, courseKey: String) -> Unit,
    viewModel: ProxyShipmentListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences(PROXY_PREF_NAME_ORIENTATION, Context.MODE_PRIVATE) }
    var isPortrait by remember { mutableStateOf(prefs.getBoolean(PROXY_PREF_KEY_IS_PORTRAIT, false)) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isPortrait) {
        activity?.requestedOrientation = if (isPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(state.selectedDateApi) {
            state.selectedDateApi?.let {
                java.time.LocalDate.parse(it)
                    .atStartOfDay(java.time.ZoneId.of("Asia/Tokyo"))
                    .toInstant()
                    .toEpochMilli()
            }
        }
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        ProxyShipmentDateFormatter.fromEpochMillis(datePickerState.selectedDateMillis)
                            ?.let(viewModel::selectDate)
                        showDatePicker = false
                    }
                ) {
                    Text("適用")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("キャンセル")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val reservedCount = state.groups.count { it.status == ProxyShipmentStatus.RESERVED }
    val pickingCount = state.groups.count { it.status == ProxyShipmentStatus.PICKING }

    Scaffold(
        containerColor = BackgroundCream,
        topBar = {
            Column {
                TopAppBar(
                    modifier = Modifier.height(60.dp),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderBg),
                    navigationIcon = {},
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = onNavigateBack,
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "戻る",
                                        tint = TitleRed,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("メニュー", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TitleRed)
                                }
                            }
                            Surface(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Amber50,
                                border = BorderStroke(1.dp, Amber200)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarToday,
                                        contentDescription = "出荷日選択",
                                        tint = AccentOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "横持ち出荷  ${state.selectedDateDisplay}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF333333),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                onClick = {
                                    isPortrait = !isPortrait
                                    prefs.edit().putBoolean(PROXY_PREF_KEY_IS_PORTRAIT, isPortrait).apply()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ScreenRotation,
                                        contentDescription = "画面回転",
                                        tint = AccentOrange,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("画面回転", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                                }
                            }
                        }
                    }
                )
                HorizontalDivider(thickness = 2.dp, color = DividerGold)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = if (state.selectedTab == ProxyShipmentTab.RESERVED) 0 else 1,
                containerColor = HeaderBg,
                contentColor = AccentOrange,
                indicator = { tabPositions ->
                    val selectedIndex = if (state.selectedTab == ProxyShipmentTab.RESERVED) 0 else 1
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        height = 3.dp,
                        color = AccentOrange
                    )
                }
            ) {
                Tab(
                    selected = state.selectedTab == ProxyShipmentTab.RESERVED,
                    onClick = { viewModel.selectTab(ProxyShipmentTab.RESERVED) },
                    text = {
                        Text(
                            text = "作業前（$reservedCount）",
                            fontWeight = if (state.selectedTab == ProxyShipmentTab.RESERVED) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    },
                    selectedContentColor = AccentOrange,
                    unselectedContentColor = Neutral500
                )
                Tab(
                    selected = state.selectedTab == ProxyShipmentTab.PICKING,
                    onClick = { viewModel.selectTab(ProxyShipmentTab.PICKING) },
                    text = {
                        Text(
                            text = "作業中（$pickingCount）",
                            fontWeight = if (state.selectedTab == ProxyShipmentTab.PICKING) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    },
                    selectedContentColor = AccentOrange,
                    unselectedContentColor = Neutral500
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading && !state.hasLoadedOnce -> ProxyShipmentLoadingContent()
                    state.errorMessage != null && state.visibleGroups.isEmpty() -> ProxyShipmentErrorContent(
                        message = state.errorMessage.orEmpty(),
                        onRetry = viewModel::refresh
                    )
                    !state.isLoading && state.visibleGroups.isEmpty() -> ProxyShipmentEmptyContent()
                    else -> ProxyShipmentListContent(
                        groups = state.visibleGroups,
                        isRefreshing = state.isRefreshing,
                        isPortrait = isPortrait,
                        onRefresh = viewModel::refresh,
                        onGroupClick = { group ->
                            viewModel.openGroup(
                                group = group,
                                onNavigateToPicking = onNavigateToPicking
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProxyShipmentLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFF8E1),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = AccentOrange,
                        strokeWidth = 4.dp
                    )
                }
            }
            Text(
                text = "横持ち出荷を読み込み中...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Neutral500
            )
        }
    }
}

@Composable
private fun ProxyShipmentEmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "該当データがありません",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProxyShipmentErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("再試行")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProxyShipmentListContent(
    groups: List<ProxyShipmentCourseGroup>,
    isRefreshing: Boolean,
    isPortrait: Boolean,
    onRefresh: () -> Unit,
    onGroupClick: (ProxyShipmentCourseGroup) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        indicator = {}
    ) {
        if (isPortrait) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { "${it.shipmentDate}:${it.courseKey}" }) { group ->
                    ProxyShipmentCourseGroupCard(
                        group = group,
                        onClick = { onGroupClick(group) }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groups, key = { "${it.shipmentDate}:${it.courseKey}" }) { group ->
                    ProxyShipmentCourseGroupCard(
                        group = group,
                        onClick = { onGroupClick(group) }
                    )
                }
            }
        }

        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = AccentOrange,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "更新中...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxyShipmentCourseGroupCard(
    group: ProxyShipmentCourseGroup,
    onClick: () -> Unit
) {
    val colors = proxyShipmentCardColors(group.status)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 164.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isPressed) colors.backgroundPressed else colors.background
        ),
        border = BorderStroke(2.dp, if (isPressed) colors.borderPressed else colors.border),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = null,
                    tint = colors.titleColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = group.deliveryCourseName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                ProxyShipmentStatusBadge(status = group.status, compact = true)
            }

            Text(
                text = "出荷日 ${ProxyShipmentDateFormatter.toDisplay(group.shipmentDate)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${group.totalCount}件 / 得意先 ${group.customerCount}件",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "コースコード ${group.deliveryCourseCode.ifBlank { "-" }}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Neutral500
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, Neutral200)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = Neutral500, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = group.destinationSummary.ifBlank { "送り先未設定" },
                        fontSize = 14.sp,
                        color = Neutral500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Amber50,
                    border = BorderStroke(1.dp, Amber200)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text("登録件数", fontSize = 12.sp, color = Neutral500)
                        Text(
                            text = "${group.registeredCount} / ${group.totalCount}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, Neutral200)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text("数量", fontSize = 12.sp, color = Neutral500)
                        Text(
                            text = "${group.totalPickedQty} / ${group.totalAssignQty}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
