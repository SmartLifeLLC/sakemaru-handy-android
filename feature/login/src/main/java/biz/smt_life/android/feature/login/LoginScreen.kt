package biz.smt_life.android.feature.login

import android.content.res.Configuration
import android.os.Build
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import biz.smt_life.android.core.designsystem.component.HandyTextField
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Login Screen per Spec 2.1.0.
 * Shows staff code/password fields, version info, date, and host URL.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    appVersion: String = "1.0",
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val hostUrl by viewModel.hostUrl.collectAsState()
    val focusManager = LocalFocusManager.current

    // Get today's date in Asia/Tokyo timezone
    val today = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now(java.time.ZoneId.of("Asia/Tokyo"))
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.JAPAN))
        } else {
            java.text.SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Tokyo")
            }.format(java.util.Date())
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onLoginSuccess()
        }
    }

    LoginContent(
        state = state,
        appVersion = appVersion,
        hostUrl = hostUrl,
        today = today,
        onStaffCodeChange = viewModel::onStaffCodeChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogin = viewModel::login,
        onNavigateToSettings = onNavigateToSettings,
        onCheckConnection = viewModel::checkConnection,
        onClearConnectionResult = viewModel::clearConnectionResult,
        focusManager = focusManager
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginContent(
    state: LoginState,
    appVersion: String,
    hostUrl: String,
    today: String,
    onStaffCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onCheckConnection: () -> Unit = {},
    onClearConnectionResult: () -> Unit = {},
    focusManager: FocusManager = LocalFocusManager.current
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("倉庫管理システム") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                }
            )
        }
    ) { padding ->
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        // 5秒後に結果を自動消去
        LaunchedEffect(state.connectionResult) {
            if (state.connectionResult != null) {
                delay(5000)
                onClearConnectionResult()
            }
        }

        if (isLandscape) {
            // Landscape: 左に入力フォーム、右に大きなログインボタン
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左側: 入力フォーム + フッター
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "ログインしてください",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    HandyTextField(
                        value = state.staffCode,
                        onValueChange = onStaffCodeChange,
                        label = "スタッフコード",
                        enabled = !state.isLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HandyTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = "パスワード",
                        enabled = !state.isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onLogin()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // フッター
                    Text(
                        text = today,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ver.$appVersion ($hostUrl)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = onCheckConnection,
                            enabled = !state.isCheckingConnection
                        ) {
                            if (state.isCheckingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text("接続確認", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        state.connectionResult?.let { result ->
                            Text(
                                text = when (result) {
                                    ConnectionResult.SUCCESS -> "接続OK"
                                    ConnectionResult.FAILURE -> "接続NG"
                                },
                                color = when (result) {
                                    ConnectionResult.SUCCESS -> Color(0xFF4CAF50)
                                    ConnectionResult.FAILURE -> MaterialTheme.colorScheme.error
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // 右側: 大きなログインボタン
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onLogin,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                "ログイン",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait: 縦1カラムレイアウト（従来）
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val commonWidth = 400.dp

                Text(
                    text = "ログインしてください",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HandyTextField(
                    value = state.staffCode,
                    onValueChange = onStaffCodeChange,
                    label = "スタッフコード",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.width(commonWidth)
                )

                Spacer(modifier = Modifier.height(8.dp))

                HandyTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = "パスワード",
                    enabled = !state.isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onLogin()
                        }
                    ),
                    modifier = Modifier.width(commonWidth)
                )

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(commonWidth)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onLogin,
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .width(commonWidth)
                        .height(56.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("ログイン")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = today,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ver.$appVersion ($hostUrl)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = onCheckConnection,
                            enabled = !state.isCheckingConnection
                        ) {
                            if (state.isCheckingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text("接続確認", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        state.connectionResult?.let { result ->
                            Text(
                                text = when (result) {
                                    ConnectionResult.SUCCESS -> "接続OK"
                                    ConnectionResult.FAILURE -> "接続NG"
                                },
                                color = when (result) {
                                    ConnectionResult.SUCCESS -> Color(0xFF4CAF50)
                                    ConnectionResult.FAILURE -> MaterialTheme.colorScheme.error
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ========== Preview Section ==========

@Preview(
    name = "Login Screen - Empty State",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun PreviewLoginScreenEmpty() {
    MaterialTheme {
        LoginContent(
            state = LoginState(),
            appVersion = "1.0.0",
            hostUrl = "https://api.example.com",
            today = "2025/12/15",
            onStaffCodeChange = {},
            onPasswordChange = {},
            onLogin = {},
            onNavigateToSettings = {},
            onCheckConnection = {},
            onClearConnectionResult = {}
        )
    }
}

@Preview(
    name = "Login Screen - Filled State",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun PreviewLoginScreenFilled() {
    MaterialTheme {
        LoginContent(
            state = LoginState(
                staffCode = "STAFF001",
                password = "password123"
            ),
            appVersion = "1.0.0",
            hostUrl = "https://api.example.com",
            today = "2025/12/15",
            onStaffCodeChange = {},
            onPasswordChange = {},
            onLogin = {},
            onNavigateToSettings = {},
            onCheckConnection = {},
            onClearConnectionResult = {}
        )
    }
}

@Preview(
    name = "Login Screen - Loading State",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun PreviewLoginScreenLoading() {
    MaterialTheme {
        LoginContent(
            state = LoginState(
                staffCode = "STAFF001",
                password = "password123",
                isLoading = true
            ),
            appVersion = "1.0.0",
            hostUrl = "https://api.example.com",
            today = "2025/12/15",
            onStaffCodeChange = {},
            onPasswordChange = {},
            onLogin = {},
            onNavigateToSettings = {},
            onCheckConnection = {},
            onClearConnectionResult = {}
        )
    }
}

@Preview(
    name = "Login Screen - Landscape",
    showBackground = true,
    widthDp = 640,
    heightDp = 360
)
@Composable
private fun PreviewLoginScreenLandscape() {
    MaterialTheme {
        LoginContent(
            state = LoginState(
                staffCode = "STAFF001",
                password = "password123"
            ),
            appVersion = "1.0.0",
            hostUrl = "https://api.example.com",
            today = "2025/12/15",
            onStaffCodeChange = {},
            onPasswordChange = {},
            onLogin = {},
            onNavigateToSettings = {},
            onCheckConnection = {},
            onClearConnectionResult = {}
        )
    }
}

@Preview(
    name = "Login Screen - Error State",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun PreviewLoginScreenError() {
    MaterialTheme {
        LoginContent(
            state = LoginState(
                staffCode = "STAFF001",
                password = "wrongpass",
                errorMessage = "Invalid credentials"
            ),
            appVersion = "1.0.0",
            hostUrl = "https://api.example.com",
            today = "2025/12/15",
            onStaffCodeChange = {},
            onPasswordChange = {},
            onLogin = {},
            onNavigateToSettings = {},
            onCheckConnection = {},
            onClearConnectionResult = {}
        )
    }
}
