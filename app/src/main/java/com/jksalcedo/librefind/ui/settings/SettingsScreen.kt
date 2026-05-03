package com.jksalcedo.librefind.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.jksalcedo.librefind.ui.common.LibreFindLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.jksalcedo.librefind.R
import com.jksalcedo.librefind.data.local.PreferencesManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// ─────────────────────────────────────────────────────────────────────────
// Reusable Modern Preference Composables
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun PreferenceCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun PreferenceItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } else {
            // Indent items without icons to align with icon-bearing items
            Spacer(modifier = Modifier.width(40.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@Composable
fun PreferenceSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null
) {
    PreferenceItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
fun PreferenceAction(
    title: String,
    actionLabel: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    subtitle: String? = null,
    icon: ImageVector? = null,
    isDestructive: Boolean = false
) {
    PreferenceItem(
        title = title,
        subtitle = subtitle,
        icon = icon,
        trailingContent = {
            Button(
                onClick = onClick,
                enabled = !isLoading,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                if (isLoading) {
                    LibreFindLoadingIndicator(size = 32)
                } else {
                    Text(actionLabel)
                }
            }
        }
    )
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
    onCommunityClick: () -> Unit = {},
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
        onCommunityClick = onCommunityClick,
        onPrivacyPolicyClick = onPrivacyPolicyClick,
        onResetTutorial = { preferencesManager.resetTutorial() },
        onOpenUri = { uriHandler.openUri(it) },
        onClearCacheRequest = { viewModel.showClearConfirmation() },
        onClearCacheConfirm = { viewModel.clearCache() },
        onClearCacheDismiss = { viewModel.hideClearConfirmation() },
        onClearClassificationRequest = { viewModel.showClearClassificationConfirmation() },
        onClearClassificationConfirm = { viewModel.clearClassificationCache() },
        onClearClassificationDismiss = { viewModel.hideClearClassificationConfirmation() },
        onDeleteAccountRequest = { viewModel.showDeleteAccountConfirmation() },
        onDeleteAccountConfirm = { viewModel.deleteAccount() },
        onDeleteAccountDismiss = { viewModel.hideDeleteAccountConfirmation() },
        onDeleteAccountErrorDismiss = { viewModel.clearDeleteAccountError() },
        onAccountDeletedDismiss = onBackClick,
        onCheckForUpdates = { viewModel.checkForUpdates() },
        onDownloadUpdate = { viewModel.downloadUpdate() },
        onResetUpdateStatus = { viewModel.resetUpdateStatus() },
        onSetIncludePrereleases = { viewModel.setIncludePrereleases(it) }
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
    onCommunityClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onResetTutorial: () -> Unit,
    onOpenUri: (String) -> Unit,
    // Cache Actions
    onClearCacheRequest: () -> Unit,
    onClearCacheConfirm: () -> Unit,
    onClearCacheDismiss: () -> Unit,
    onClearClassificationRequest: () -> Unit,
    onClearClassificationConfirm: () -> Unit,
    onClearClassificationDismiss: () -> Unit,
    // Account Actions
    onDeleteAccountRequest: () -> Unit,
    onDeleteAccountConfirm: () -> Unit,
    onDeleteAccountDismiss: () -> Unit,
    onDeleteAccountErrorDismiss: () -> Unit,
    onAccountDeletedDismiss: () -> Unit,
    // Update Actions
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onResetUpdateStatus: () -> Unit,
    onSetIncludePrereleases: (Boolean) -> Unit
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
        ) {
            // Language Selection
            LanguageSection()
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Appearance & Behavior
            PreferenceCategory(stringResource(R.string.settings_hide_system_packages_title))
            val preferencesManager: PreferencesManager = koinInject()
            var hideSystem by remember { mutableStateOf(preferencesManager.shouldHideSystemPackages()) }

            PreferenceSwitch(
                title = stringResource(R.string.settings_hide_system_packages_label),
                checked = hideSystem,
                onCheckedChange = {
                    hideSystem = it
                    preferencesManager.setHideSystemPackages(it)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Cache Management
            PreferenceCategory(stringResource(R.string.settings_cache_management))
            PreferenceAction(
                title = stringResource(R.string.settings_cache_size),
                subtitle = state.cacheSizeMB,
                actionLabel = stringResource(R.string.settings_clear),
                onClick = onClearCacheRequest,
                isLoading = state.isClearing,
                isDestructive = true,
                icon = Icons.Default.Delete
            )
            PreferenceAction(
                title = "Classification Cache",
                subtitle = "${state.classificationCacheCount} apps cached",
                actionLabel = stringResource(R.string.settings_clear),
                onClick = onClearClassificationRequest,
                isLoading = state.isClearingClassification,
                isDestructive = true,
                icon = Icons.Default.Delete
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Feedback & Community
            PreferenceCategory(stringResource(R.string.settings_feedback))
            PreferenceItem(
                title = stringResource(R.string.settings_report_issue),
                icon = Icons.Default.Feedback,
                onClick = onReportClick
            )
            PreferenceItem(
                title = stringResource(R.string.settings_my_reports),
                icon = Icons.Default.History,
                onClick = onMyReportsClick
            )
            PreferenceItem(
                title = stringResource(R.string.settings_github_issues),
                icon = Icons.Default.BugReport,
                onClick = { onOpenUri("https://github.com/jksalcedo/librefind/issues") }
            )
            PreferenceItem(
                title = stringResource(R.string.settings_join_community),
                icon = Icons.Default.Group,
                onClick = { onOpenUri("https://t.me/librefind") }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Community Moderation
            PreferenceCategory(stringResource(R.string.settings_community_title))
            PreferenceItem(
                title = stringResource(R.string.settings_browse_pending_title),
                subtitle = stringResource(R.string.settings_browse_pending_desc),
                icon = Icons.Default.Edit,
                onClick = onCommunityClick
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Help
            PreferenceCategory(stringResource(R.string.settings_help))
            PreferenceItem(
                title = stringResource(R.string.settings_reset_tutorial),
                icon = Icons.Default.Refresh,
                onClick = onResetTutorial
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // About
            PreferenceCategory(stringResource(R.string.settings_about))
            PreferenceItem(
                title = stringResource(R.string.settings_version),
                subtitle = version,
                icon = Icons.Default.Info
            )
            PreferenceItem(
                title = stringResource(R.string.settings_check_updates),
                icon = Icons.Default.Refresh,
                onClick = onCheckForUpdates
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_include_prereleases_label),
                subtitle = stringResource(R.string.settings_include_prereleases_desc),
                checked = state.includePrereleases,
                onCheckedChange = onSetIncludePrereleases
            )
            PreferenceItem(
                title = stringResource(R.string.settings_view_github),
                icon = Icons.Default.Info,
                onClick = { onOpenUri("https://github.com/jksalcedo/librefind") }
            )
            PreferenceItem(
                title = stringResource(R.string.settings_donate),
                icon = Icons.Default.VolunteerActivism,
                onClick = { onOpenUri("https://ko-fi.com/jksalcedo") }
            )
            PreferenceItem(
                title = stringResource(R.string.settings_privacy_policy),
                icon = Icons.Default.PrivacyTip,
                onClick = onPrivacyPolicyClick
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Account
            if (state.isLoggedIn) {
                PreferenceCategory(stringResource(R.string.settings_account))
                PreferenceItem(
                    title = stringResource(R.string.settings_delete_account),
                    icon = Icons.Default.Delete,
                    onClick = onDeleteAccountRequest,
                    modifier = Modifier.padding(bottom = 32.dp)
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
    ClearClassificationCacheDialog(
        state = state,
        onConfirm = onClearClassificationConfirm,
        onDismiss = onClearClassificationDismiss
    )
    DeleteAccountDialog(
        state = state,
        onConfirm = onDeleteAccountConfirm,
        onDismiss = onDeleteAccountDismiss
    )
    AccountDeletedDialog(state = state, onDismiss = onAccountDeletedDismiss)
    DeleteAccountErrorDialog(state = state, onDismiss = onDeleteAccountErrorDismiss)

    // Update Dialogs
    UpdateDialogs(
        state = state,
        onDownload = onDownloadUpdate,
        onDismiss = onResetUpdateStatus
    )
}

// ─────────────────────────────────────────────
// Section composables
// ─────────────────────────────────────────────

@Composable
private fun LanguageSection() {
    var showDialog by remember { mutableStateOf(false) }

    val currentLocale = remember {
        androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().getFirstMatch(
            arrayOf("en", "ar", "de", "el", "es", "et", "fr", "it", "pl", "tr", "zh-rCN")
        )?.language ?: ""
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

    PreferenceItem(
        title = stringResource(R.string.settings_language),
        subtitle = currentLanguageLabel,
        icon = Icons.Default.Language,
        onClick = { showDialog = true },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    )

    if (showDialog) {
        LanguageSelectionDialog(
            currentLocale = currentLocale,
            onDismiss = { showDialog = false },
            onLanguageSelected = { tag ->
                showDialog = false
                val localeList = if (tag.isEmpty()) {
                    androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                } else {
                    androidx.core.os.LocaleListCompat.forLanguageTags(tag)
                }
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
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
                        androidx.compose.material3.RadioButton(
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
private fun ClearClassificationCacheDialog(
    state: SettingsState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.showClearClassificationConfirmation) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear Classification Cache?") },
        text = { Text("This will clear cached classification results. They will be re-fetched on the next scan.") },
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
                    LibreFindLoadingIndicator(size = 40)
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

@Composable
private fun UpdateDialogs(
    state: SettingsState,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state.updateCheckStatus) {
        UpdateCheckStatus.CHECKING -> {
            AlertDialog(
                onDismissRequest = { /* Don't dismiss while checking */ },
                title = { Text(stringResource(R.string.settings_checking_updates)) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LibreFindLoadingIndicator()
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
                        state.updateError ?: stringResource(
                            R.string.settings_update_error,
                            "Unknown error"
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.settings_ok))
                    }
                }
            )
        }

        else -> {}
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
        onCommunityClick = {},
        onPrivacyPolicyClick = {},
        onResetTutorial = {},
        onOpenUri = {},
        onClearCacheRequest = {},
        onClearCacheConfirm = {},
        onClearCacheDismiss = {},
        onClearClassificationRequest = {},
        onClearClassificationConfirm = {},
        onClearClassificationDismiss = {},
        onDeleteAccountRequest = {},
        onDeleteAccountConfirm = {},
        onDeleteAccountDismiss = {},
        onDeleteAccountErrorDismiss = {},
        onAccountDeletedDismiss = {},
        onCheckForUpdates = {},
        onDownloadUpdate = {},
        onResetUpdateStatus = {},
        onSetIncludePrereleases = {}
    )
}
