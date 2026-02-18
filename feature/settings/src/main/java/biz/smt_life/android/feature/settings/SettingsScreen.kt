package biz.smt_life.android.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import biz.smt_life.android.core.designsystem.component.HandyTextField

/**
 * Settings Screen for Host URL configuration.
 * Allows users to change the API server base URL.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success message as snackbar
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages()
        }
    }

    SettingsContent(
        state = state,
        onHostUrlChange = viewModel::onHostUrlChange,
        onPresetUrlSelected = viewModel::onPresetUrlSelected,
        onCustomUrlSelected = viewModel::onCustomUrlSelected,
        onSave = viewModel::saveHostUrl,
        onNavigateBack = onNavigateBack,
        focusManager = focusManager,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    state: SettingsState,
    onHostUrlChange: (String) -> Unit,
    onPresetUrlSelected: (String) -> Unit,
    onCustomUrlSelected: () -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
    focusManager: FocusManager = LocalFocusManager.current,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "APIサーバー設定",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "WMS APIサーバーのベースURLを設定します。すべてのAPI通信に使用されます。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Preset URL radio buttons
            Column(modifier = Modifier.selectableGroup()) {
                state.presetUrls.forEach { url ->
                    val isSelected = !state.isCustomUrl && state.hostUrl == url
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { onPresetUrlSelected(url) },
                                role = Role.RadioButton,
                                enabled = !state.isLoading
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            enabled = !state.isLoading
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Custom URL option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.isCustomUrl,
                            onClick = onCustomUrlSelected,
                            role = Role.RadioButton,
                            enabled = !state.isLoading
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.isCustomUrl,
                        onClick = null,
                        enabled = !state.isLoading
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "カスタムURL",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Custom URL text field (only shown when custom is selected)
            if (state.isCustomUrl) {
                Spacer(modifier = Modifier.height(8.dp))
                HandyTextField(
                    value = state.hostUrl,
                    onValueChange = onHostUrlChange,
                    label = "カスタムURL",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onSave()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSave,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("保存")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "注意事項",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• URLはhttp://またはhttps://で始まる必要があります\n" +
                                "• 変更は即座に反映されます\n" +
                                "• 保存前にサーバーへのアクセスを確認してください",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ========== Preview Section ==========

@Preview(
    name = "Settings Screen - Preset Selected",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun PreviewSettingsScreenPreset() {
    MaterialTheme {
        SettingsContent(
            state = SettingsState(
                hostUrl = "https://wms.lw-hana.net",
                isCustomUrl = false
            ),
            onHostUrlChange = {},
            onPresetUrlSelected = {},
            onCustomUrlSelected = {},
            onSave = {},
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "Settings Screen - Custom URL",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun PreviewSettingsScreenCustom() {
    MaterialTheme {
        SettingsContent(
            state = SettingsState(
                hostUrl = "https://custom-api.example.com",
                isCustomUrl = true
            ),
            onHostUrlChange = {},
            onPresetUrlSelected = {},
            onCustomUrlSelected = {},
            onSave = {},
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "Settings Screen - Loading State",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun PreviewSettingsScreenLoading() {
    MaterialTheme {
        SettingsContent(
            state = SettingsState(
                hostUrl = "https://wms.sakemaru.click",
                isLoading = true
            ),
            onHostUrlChange = {},
            onPresetUrlSelected = {},
            onCustomUrlSelected = {},
            onSave = {},
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "Settings Screen - Error State",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun PreviewSettingsScreenError() {
    MaterialTheme {
        SettingsContent(
            state = SettingsState(
                hostUrl = "invalid-url",
                isCustomUrl = true,
                errorMessage = "ホストURLはhttp://またはhttps://で始まる必要があります"
            ),
            onHostUrlChange = {},
            onPresetUrlSelected = {},
            onCustomUrlSelected = {},
            onSave = {},
            onNavigateBack = {}
        )
    }
}
