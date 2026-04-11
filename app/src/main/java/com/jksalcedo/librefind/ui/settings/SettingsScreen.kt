package com.jksalcedo.librefind.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.jksalcedo.librefind.R
import com.jksalcedo.librefind.data.local.PreferencesManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
private fun SettingsGroupTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Row(
        modifier = clickableModifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onToggle
        )
    }
}

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
        onAccountDeletedDismiss = onBackClick,
        onCheckForUpdates = { viewModel.checkForUpdates() },
        onDownloadUpdate = { viewModel.downloadUpdate() },
        onResetUpdateStatus = { viewModel.resetUpdateStatus() }
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
    onClearCacheRequest: () -> Unit,
    onClearCacheConfirm: () -> Unit,
    onClearCacheDismiss: () -> Unit,
    onDeleteAccountRequest: () -> Unit,
    onDeleteAccountConfirm: () -> Unit,
    onDeleteAccountDismiss: () -> Unit,
    onDeleteAccountErrorDismiss: () -> Unit,
    onAccountDeletedDismiss: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onResetUpdateStatus: () -> Unit
) {
    val preferencesManager: PreferencesManager = koinInject()
    var hideSystem by remember { mutableStateOf(preferencesManager.shouldHideSystemPackages()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Language
            SettingsGroupTitle(stringResource(R.string.settings_language))
            LanguageRowModern()
            HorizontalDivider()

            // Cache
            SettingsGroupTitle(stringResource(R.string.settings_cache_management))
            CacheRowModern(
                state = state,
                onClearCacheRequest = onClearCacheRequest
            )
            HorizontalDivider()

            // Feedback & Community
            SettingsGroupTitle(stringResource(R.string.settings_feedback))
            SettingsRow(
                icon = Icons.Default.Feedback,
                title = stringResource(R.string.settings_report_issue),
                onClick = onReportClick
            )
            SettingsRow(
                icon = Icons.Default.History,
                title = stringResource(R.string.settings_my_reports),
                onClick = onMyReportsClick
            )
            SettingsRow(
                icon = Icons.Default.BugReport,
                title = stringResource(R.string.settings_github_issues),
                onClick = { onOpenUri("https://github.com/jksalcedo/librefind/issues") }
            )
            SettingsRow(
                icon = Icons.Default.Group,
                title = stringResource(R.string.settings_join_community),
                onClick = { onOpenUri("https://t.me/librefind") }
            )
            HorizontalDivider()

            // Help
            SettingsGroupTitle(stringResource(R.string.settings_help))
            SettingsRow(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.settings_reset_tutorial),
                onClick = onResetTutorial
            )
            HorizontalDivider()

            // About
            SettingsGroupTitle(stringResource(R.string.settings_about))
            SettingsRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_version),
                trailingText = version,
                showChevron = false,
                onClick = null
            )
            SettingsRow(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.settings_check_updates),
                onClick = onCheckForUpdates
            )
            SettingsRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_view_github),
                onClick = { onOpenUri("https://github.com/jksalcedo/librefind") }
            )
            SettingsRow(
                icon = Icons.Default.VolunteerActivism,
                title = stringResource(R.string.settings_donate),
                onClick = { onOpenUri("https://ko-fi.com/jksalcedo") }
            )
            SettingsRow(
                icon = Icons.Default.PrivacyTip,
                title = stringResource(R.string.settings_privacy_policy),
                onClick = onPrivacyPolicyClick
            )
            HorizontalDivider()

            // System packages toggle
            SettingsGroupTitle(stringResource(R.string.settings_hide_system_packages_title))
            SettingsToggleRow(
                title = stringResource(R.string.settings_hide_system_packages_label),
                checked = hideSystem,
                onToggle = { newValue ->
                    hideSystem = newValue
                    preferencesManager.setHideSystemPackages(newValue)
                }
            )
            HorizontalDivider()

            // Account
            if (state.isLoggedIn) {
                SettingsGroupTitle(stringResource(R.string.settings_account))
                SettingsRow(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.settings_delete_account),
                    onClick = onDeleteAccountRequest
                )
            }
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

    UpdateDialogs(
        state = state,
        onDownload = onDownloadUpdate,
        onDismiss = onResetUpdateStatus
    )
}

@Composable
private fun LanguageRowModern() {
    var showDialog by remember { mutableStateOf(false) }

    val currentLocale = remember {
        AppCompatDelegate.getApplicationLocales()
            .getFirstMatch(
                arrayOf(
                    "en",
                    "ar",
                    "de",
                    "el",
                    "es",
                    "et",
                    "fr",
                    "it",
                    "pl",
                    "tr",
                    "zh-rCN"
                )
            )
            ?.language ?: ""
    }

    val currentLanguageLabel = when (currentLocale) {
        "ar" -> stringResource(R.string.settings_language_arabic)
        "de" -> stringResource(R.string.settings_language_german)
        "el" -> stringResource(R.string.settings_language_greek)
        "es" -> stringResource(R.string.settings_language_spanish)
        "et" -> stringResource(R.string.settings_language_estonian)
        "fr" -> stringResource(R.string.settings_language_french)
        "it" -> stringResource(R.string.settings_language_italian)
        "pl" -> stringResource(R.string.settings_language_polish)
        "tr" -> stringResource(R.string.settings_language_turkish)
        "zh" -> stringResource(R.string.settings_language_chinese_simplified)
        "en" -> stringResource(R.string.settings_language_english)
        else -> stringResource(R.string.settings_language_system)
    }

    SettingsRow(
        icon = Icons.Default.Info,
        title = stringResource(R.string.settings_language),
        trailingText = currentLanguageLabel,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        LanguageSelectionDialog(
            currentLocale = currentLocale,
            onDismiss = { showDialog = false },
            onLanguageSelected = { tag ->
                showDialog = false
                val localeList = if (tag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(tag)
                }
                AppCompatDelegate.setApplicationLocales(localeList)
            }
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLocale: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf(
        "" to stringResource(R.string.settings_language_system),
        "en" to stringResource(R.string.settings_language_english),
        "ar" to stringResource(R.string.settings_language_arabic),
        "de" to stringResource(R.string.settings_language_german),
        "el" to stringResource(R.string.settings_language_greek),
        "es" to stringResource(R.string.settings_language_spanish),
        "et" to stringResource(R.string.settings_language_estonian),
        "fr" to stringResource(R.string.settings_language_french),
        "it" to stringResource(R.string.settings_language_italian),
        "pl" to stringResource(R.string.settings_language_polish),
        "tr" to stringResource(R.string.settings_language_turkish),
        "zh-rCN" to stringResource(R.string.settings_language_chinese_simplified)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                languages.forEach { (tag, label) ->
                    val isSelected =
                        tag == currentLocale || (tag.isEmpty() && currentLocale.isEmpty())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(tag) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onLanguageSelected(tag) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        }
    )
}

@Composable
private fun CacheRowModern(
    state: SettingsState,
    onClearCacheRequest: () -> Unit
) {
    SettingsRow(
        icon = Icons.Default.Delete,
        title = stringResource(R.string.settings_cache_size),
        subtitle = state.cacheSizeMB,
        trailingText = if (state.isClearing) stringResource(R.string.settings_clearing) else null,
        onClick = {
            if (!state.isClearing) onClearCacheRequest()
        }
    )
}

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
        text = { Text(stringResource(R.string.settings_clear_cache_message)) },
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
        text = { Text(stringResource(R.string.settings_delete_account_message)) },
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
        onDismissRequest = {},
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

@Composable
private fun UpdateDialogs(
    state: SettingsState,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state.updateCheckStatus) {
        UpdateCheckStatus.CHECKING -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.settings_checking_updates)) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {}
            )
        }

        UpdateCheckStatus.UPDATE_AVAILABLE -> {
            val update = state.latestUpdate ?: return
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.settings_update_available_title)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = stringResource(
                                R.string.settings_update_available_message,
                                update.version
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (update.changelog.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.settings_changelog_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = update.changelog,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = onDownload) {
                        Text(stringResource(R.string.settings_download_install))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            )
        }

        UpdateCheckStatus.UP_TO_DATE -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.settings_up_to_date)) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.settings_ok))
                    }
                }
            )
        }

        UpdateCheckStatus.ERROR -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.settings_error)) },
                text = {
                    Text(
                        state.updateError
                            ?: stringResource(R.string.settings_update_error, "Unknown error")
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.settings_ok))
                    }
                }
            )
        }

        else -> Unit
    }
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
        onAccountDeletedDismiss = {},
        onCheckForUpdates = {},
        onDownloadUpdate = {},
        onResetUpdateStatus = {}
    )
}