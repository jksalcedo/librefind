package com.jksalcedo.librefind.ui.settings

import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.PrivacyTip
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
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
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
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val preferencesManager: PreferencesManager = koinInject()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            // 1. Cache Management
            CacheManagementSection(state = state, viewModel = viewModel)

            //  2. Help
            SettingsSection(title = "Help") {
                SettingsLinkButton(
                    icon = Icons.Default.Refresh,
                    label = "Reset Tutorial",
                    onClick = { preferencesManager.resetTutorial() }
                )
            }

            //  3. Feedback & Community
            SettingsSection(title = "Feedback & Community") {
                SettingsLinkButton(
                    icon = Icons.Default.Feedback,
                    label = "Report Issue / Suggestion",
                    onClick = onReportClick
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.History,
                    label = "My Reports",
                    onClick = onMyReportsClick
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.BugReport,
                    label = "GitHub Issues",
                    onClick = { uriHandler.openUri("https://github.com/jksalcedo/librefind/issues") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.Group,
                    label = "Join Community",
                    onClick = { uriHandler.openUri("https://t.me/librefind") }
                )
            }

            //  About
            SettingsSection(title = "About") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Version", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = version,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.Info,
                    label = "View on GitHub",
                    onClick = { uriHandler.openUri("https://github.com/jksalcedo/librefind") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.VolunteerActivism,
                    label = "Donate to LibreFind",
                    onClick = { uriHandler.openUri("https://ko-fi.com/jksalcedo") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsLinkButton(
                    icon = Icons.Default.PrivacyTip,
                    label = "Privacy Policy",
                    onClick = onPrivacyPolicyClick
                )
            }

            // 5. Account
            AccountSection(state = state, viewModel = viewModel)
        }
    }

    // Dialogs
    ClearCacheDialog(state = state, viewModel = viewModel)
    DeleteAccountDialog(state = state, viewModel = viewModel)
    AccountDeletedDialog(state = state, onDismiss = onBackClick)
    DeleteAccountErrorDialog(state = state, viewModel = viewModel)
}

// ─────────────────────────────────────────────
// Section composables
// ─────────────────────────────────────────────

@Composable
private fun CacheManagementSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    SettingsSection(title = "Cache Management") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Icon Cache Size", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = state.cacheSizeMB,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { viewModel.showClearConfirmation() },
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
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
private fun AccountSection(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    SettingsSection(title = "Account") {
        Button(
            onClick = { viewModel.showDeleteAccountConfirmation() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Account")
        }
    }
}

// ─────────────────────────────────────────────
// Dialog composables
// ─────────────────────────────────────────────

@Composable
private fun ClearCacheDialog(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    if (!state.showClearConfirmation) return

    AlertDialog(
        onDismissRequest = { viewModel.hideClearConfirmation() },
        title = { Text("Clear Icon Cache?") },
        text = {
            Text("This will remove all cached app icons. They will be reloaded when you scroll through the dashboard.")
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.clearCache() },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.hideClearConfirmation() }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteAccountDialog(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    if (!state.showDeleteAccountConfirmation) return

    AlertDialog(
        onDismissRequest = { viewModel.hideDeleteAccountConfirmation() },
        title = { Text("Delete Account?") },
        text = {
            Text("Are you sure you want to delete your account? This action cannot be undone and will permanently remove all your data.")
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.deleteAccount() },
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
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            if (!state.isDeletingAccount) {
                TextButton(onClick = { viewModel.hideDeleteAccountConfirmation() }) {
                    Text("Cancel")
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
        title = { Text("Account Deleted") },
        text = { Text("Your account has been successfully deleted.") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun DeleteAccountErrorDialog(
    state: SettingsState,
    viewModel: SettingsViewModel
) {
    val error = state.deleteAccountError ?: return

    AlertDialog(
        onDismissRequest = { viewModel.clearDeleteAccountError() },
        title = { Text("Error") },
        text = { Text(error) },
        confirmButton = {
            TextButton(onClick = { viewModel.clearDeleteAccountError() }) {
                Text("OK")
            }
        }
    )
}