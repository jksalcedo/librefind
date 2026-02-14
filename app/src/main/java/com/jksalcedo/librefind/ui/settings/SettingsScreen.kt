package com.jksalcedo.librefind.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jksalcedo.librefind.R
import com.jksalcedo.librefind.data.local.PreferencesManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// ──────────────────────────────────
// Reusable section card composable
// ──────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsLinkButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clickable(
                interactionSource = null,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            textAlign = TextAlign.Start
        )
    }

}

// ───────────────────────
// Main Settings Screen
// ───────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onReportClick: () -> Unit = {},
    onMyReportsClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val preferencesManager: PreferencesManager = koinInject()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val version = remember {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        packageInfo.versionName ?: "Unknown"
    }

    SettingsContent(
        state = state,
        version = version,
        onBackClick = onBackClick,
        onReportClick = onReportClick,
        onMyReportsClick = onMyReportsClick,
        onPrivacyPolicyClick = onPrivacyPolicyClick,
        onResetTutorial = { preferencesManager.resetTutorial() },
        onOpenUri = { uriHandler.openUri(it) },
        onClearCacheRequest = { viewModel.showClearConfirmation() },
        onClearCacheConfirm = { viewModel.clearCache() },
        onClearCacheDismiss = { viewModel.hideClearConfirmation() },
        onDeleteAccountRequest = { viewModel.showDeleteAccountConfirmation() },
        onDeleteAccountConfirm = { viewModel.deleteAccount() },
        onDeleteAccountDismiss = { viewModel.hideDeleteAccountConfirmation() },
        onDeleteAccountErrorDismiss = { viewModel.clearDeleteAccountError() },
        onAccountDeletedDismiss = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsState,
    version: String,
    onBackClick: () -> Unit,
    onReportClick: () -> Unit,
    onMyReportsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onResetTutorial: () -> Unit,
    onOpenUri: (String) -> Unit,
    // Cache Actions
    onClearCacheRequest: () -> Unit,
    onClearCacheConfirm: () -> Unit,
    onClearCacheDismiss: () -> Unit,
    // Account Actions
    onDeleteAccountRequest: () -> Unit,
    onDeleteAccountConfirm: () -> Unit,
    onDeleteAccountDismiss: () -> Unit,
    onDeleteAccountErrorDismiss: () -> Unit,
    onAccountDeletedDismiss: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cache Management
            CacheManagementSection(state = state, onClearCacheRequest = onClearCacheRequest)

            //  Feedback & Community
            SettingsSection(title = stringResource(R.string.settings_feedback)) {
                SettingsLinkButton(
                    icon = Icons.Default.Feedback,
                    label = stringResource(R.string.settings_report_issue),
                    onClick = onReportClick
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.History,
                    label = stringResource(R.string.settings_my_reports),
                    onClick = onMyReportsClick
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.BugReport,
                    label = stringResource(R.string.settings_github_issues),
                    onClick = { onOpenUri("https://github.com/jksalcedo/librefind/issues") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.Group,
                    label = stringResource(R.string.settings_join_community),
                    onClick = { onOpenUri("https://t.me/librefind") }
                )
            }

            //  Help
            SettingsSection(title = stringResource(R.string.settings_help)) {
                SettingsLinkButton(
                    icon = Icons.Default.Refresh,
                    label = stringResource(R.string.settings_reset_tutorial),
                    onClick = onResetTutorial
                )
            }

            //  About
            SettingsSection(title = stringResource(R.string.settings_about)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.settings_version),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = version,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.Info,
                    label = stringResource(R.string.settings_view_github),
                    onClick = { onOpenUri("https://github.com/jksalcedo/librefind") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.VolunteerActivism,
                    label = stringResource(R.string.settings_donate),
                    onClick = { onOpenUri("https://ko-fi.com/jksalcedo") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.PrivacyTip,
                    label = stringResource(R.string.settings_privacy_policy),
                    onClick = onPrivacyPolicyClick
                )
            }

            // 5. Account
            AccountSection(onDeleteAccountRequest = onDeleteAccountRequest)
        }
    }

    // Dialogs
    ClearCacheDialog(
        state = state,
        onConfirm = onClearCacheConfirm,
        onDismiss = onClearCacheDismiss
    )
    DeleteAccountDialog(
        state = state,
        onConfirm = onDeleteAccountConfirm,
        onDismiss = onDeleteAccountDismiss
    )
    AccountDeletedDialog(state = state, onDismiss = onAccountDeletedDismiss)
    DeleteAccountErrorDialog(state = state, onDismiss = onDeleteAccountErrorDismiss)
}

// ─────────────────────────────────────────────
// Section composables
// ─────────────────────────────────────────────

@Composable
private fun CacheManagementSection(
    state: SettingsState,
    onClearCacheRequest: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_cache_management)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(R.string.settings_cache_size),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = state.cacheSizeMB,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onClearCacheRequest,
                enabled = !state.isClearing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (state.isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.settings_clear))
                }
            }
        }
    }
}

@Composable
private fun AccountSection(
    onDeleteAccountRequest: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_account)) {
        Button(
            onClick = onDeleteAccountRequest,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.settings_delete_account))
        }
    }
}

// ─────────────────────────────────────────────
// Dialog composables
// ─────────────────────────────────────────────

@Composable
private fun ClearCacheDialog(
    state: SettingsState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.showClearConfirmation) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_clear_cache_title)) },
        text = {
            Text(stringResource(R.string.settings_clear_cache_message))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.settings_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        }
    )
}

@Composable
private fun DeleteAccountDialog(
    state: SettingsState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.showDeleteAccountConfirmation) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_delete_account_title)) },
        text = {
            Text(stringResource(R.string.settings_delete_account_message))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                enabled = !state.isDeletingAccount
            ) {
                if (state.isDeletingAccount) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.settings_delete))
                }
            }
        },
        dismissButton = {
            if (!state.isDeletingAccount) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        }
    )
}

@Composable
private fun AccountDeletedDialog(
    state: SettingsState,
    onDismiss: () -> Unit
) {
    if (!state.isAccountDeleted) return

    AlertDialog(
        onDismissRequest = { /* Block dismiss — force user to tap OK */ },
        title = { Text(stringResource(R.string.settings_account_deleted_title)) },
        text = { Text(stringResource(R.string.settings_account_deleted_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_ok))
            }
        }
    )
}

@Composable
private fun DeleteAccountErrorDialog(
    state: SettingsState,
    onDismiss: () -> Unit
) {
    val error = state.deleteAccountError ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_error)) },
        text = { Text(error) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_ok))
            }
        }
    )
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsContent(
        state = SettingsState(),
        version = "1.0.0",
        onBackClick = {},
        onReportClick = {},
        onMyReportsClick = {},
        onPrivacyPolicyClick = {},
        onResetTutorial = {},
        onOpenUri = {},
        onClearCacheRequest = {},
        onClearCacheConfirm = {},
        onClearCacheDismiss = {},
        onDeleteAccountRequest = {},
        onDeleteAccountConfirm = {},
        onDeleteAccountDismiss = {},
        onDeleteAccountErrorDismiss = {},
        onAccountDeletedDismiss = {}
    )
}