package com.jksalcedo.librefind.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.jksalcedo.librefind.R
import com.jksalcedo.librefind.data.local.PreferencesManager
import com.jksalcedo.librefind.ui.auth.AuthViewModel
import com.jksalcedo.librefind.ui.common.TargetArea
import com.jksalcedo.librefind.ui.common.TutorialOverlay
import com.jksalcedo.librefind.ui.common.TutorialStep
import com.jksalcedo.librefind.ui.dashboard.components.GaugeDetailsDialog
import com.jksalcedo.librefind.ui.dashboard.components.ScanList
import com.jksalcedo.librefind.ui.dashboard.components.SovereigntyGauge
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAppClick: (String, String) -> Unit,
    onSubmitClick: () -> Unit = {},
    onMySubmissionsClick: () -> Unit = {},
    onIgnoredAppsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    LocalContext.current
    val preferencesManager: PreferencesManager = koinInject()
    var showDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    var showTutorial by remember { mutableStateOf(!preferencesManager.hasSeenTutorial()) }
    var tutorialStepIndex by remember { mutableIntStateOf(0) }
    val currentTutorialStep = TutorialStep.allSteps.getOrNull(tutorialStepIndex)

    var gaugeRect by remember { mutableStateOf(Rect.Zero) }
    var filterRect by remember { mutableStateOf(Rect.Zero) }
    var searchRect by remember { mutableStateOf(Rect.Zero) }
    var profileRect by remember { mutableStateOf(Rect.Zero) }
    var fabRect by remember { mutableStateOf(Rect.Zero) }

    val currentHighlightRect = when (currentTutorialStep?.targetArea) {
        TargetArea.GAUGE -> gaugeRect
        TargetArea.FILTER_CHIPS -> filterRect
        TargetArea.SEARCH -> searchRect
        TargetArea.PROFILE -> profileRect
        TargetArea.FAB -> fabRect
        null -> Rect.Zero
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text(stringResource(R.string.dashboard_search_hint)) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = authState.userProfile?.username ?: stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        if (isSearchActive) {
                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.updateSearchQuery("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dashboard_close_search))
                            }
                        } else {
                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    searchRect = coords.boundsInRoot()
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.dashboard_search))
                            }
                            Box {
                                IconButton(
                                    onClick = { showFilterMenu = true },
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        filterRect = coords.boundsInRoot()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = stringResource(R.string.dashboard_filter),
                                        tint = if (state.appFilter != AppFilter.ALL)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                DropdownMenu(
                                    expanded = showFilterMenu,
                                    onDismissRequest = { showFilterMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.dashboard_filter_show_all)) },
                                        onClick = {
                                            viewModel.setAppFilter(AppFilter.ALL)
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            if (state.appFilter == AppFilter.ALL) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.dashboard_filter_all_proprietary)) },
                                        onClick = {
                                            viewModel.setAppFilter(AppFilter.PROP_ONLY)
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            if (state.appFilter == AppFilter.PROP_ONLY) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.dashboard_filter_with_alternatives)) },
                                        onClick = {
                                            viewModel.setAppFilter(AppFilter.PROP_WITH_ALTERNATIVES)
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            if (state.appFilter == AppFilter.PROP_WITH_ALTERNATIVES) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.dashboard_filter_no_alternatives)) },
                                        onClick = {
                                            viewModel.setAppFilter(AppFilter.PROP_NO_ALTERNATIVES)
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            if (state.appFilter == AppFilter.PROP_NO_ALTERNATIVES) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.dashboard_filter_foss_only)) },
                                        onClick = {
                                            viewModel.setAppFilter(AppFilter.FOSS_ONLY)
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            if (state.appFilter == AppFilter.FOSS_ONLY) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.dashboard_filter_unknown_only)) },
                                        onClick = {
                                            viewModel.setAppFilter(AppFilter.UNKNOWN_ONLY)
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            if (state.appFilter == AppFilter.UNKNOWN_ONLY) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.dashboard_filter_pending_only)) },
                                        onClick = {
                                            viewModel.setAppFilter(AppFilter.PENDING_ONLY)
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            if (state.appFilter == AppFilter.PENDING_ONLY) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.dashboard_filter_ignored_only)) },
                                        onClick = {
                                            viewModel.setAppFilter(AppFilter.IGNORED_ONLY)
                                            showFilterMenu = false
                                        },
                                        leadingIcon = {
                                            if (state.appFilter == AppFilter.IGNORED_ONLY) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showProfileDialog = true },
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    profileRect = coords.boundsInRoot()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = stringResource(R.string.dashboard_profile)
                                )
                            }
                            IconButton(
                                onClick = onSettingsClick
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.dashboard_settings)
                                )
                            }
                        }
                    },
                    scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onSubmitClick,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        fabRect = coords.boundsInRoot()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.dashboard_submit_app))
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    state.error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.dashboard_error_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.error ?: stringResource(R.string.dashboard_unknown_error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.scan() }) {
                                Text(stringResource(R.string.dashboard_retry))
                            }
                        }
                    }

                    else -> {
                        // App List
                        if (state.apps.isEmpty()) {
                            // Show gauge even when empty
                            Column(modifier = Modifier.fillMaxSize()) {
                                state.sovereigntyScore?.let { score ->
                                    SovereigntyGauge(
                                        score = score,
                                        currentFilter = state.statusFilter,
                                        onFilterClick = { status -> viewModel.setStatusFilter(status) },
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .onGloballyPositioned { coords ->
                                                gaugeRect = coords.boundsInRoot()
                                            }
                                    ) { showDialog = true }
                                }
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (state.searchQuery.isNotEmpty()) stringResource(R.string.dashboard_no_matching_apps) else stringResource(R.string.dashboard_no_apps_found),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            ScanList(
                                apps = state.apps,
                                onAppClick = onAppClick,
                                onIgnoreClick = { packageName -> viewModel.ignoreApp(packageName) },
                                onRestoreClick = { packageName -> viewModel.restoreApp(packageName)},
                                onRefresh = { viewModel.scan() },
                                isRefreshing = state.isLoading,
                                modifier = Modifier.fillMaxSize(),
                                headerContent = {
                                    state.sovereigntyScore?.let { score ->
                                        SovereigntyGauge(
                                            score = score,
                                            currentFilter = state.statusFilter,
                                            onFilterClick = { status ->
                                                viewModel.setStatusFilter(
                                                    status
                                                )
                                            },
                                            modifier = Modifier
                                                .padding(bottom = 8.dp)
                                                .onGloballyPositioned { coords ->
                                                    gaugeRect = coords.boundsInRoot()
                                                }
                                        ) { showDialog = true }
                                    }
                                }
                            )
                        }
                    }
                }

                if (showDialog) {
                    state.sovereigntyScore?.let { score ->
                        GaugeDetailsDialog(
                            score = score,
                            currentFilter = state.statusFilter,
                            onFilterClick = { status -> viewModel.setStatusFilter(status) },
                            onDismissRequest = { showDialog = false }
                        )
                    }
                }

                if (showProfileDialog) {
                    if (authState.isSignedIn && authState.userProfile != null) {
                        AlertDialog(
                            onDismissRequest = { showProfileDialog = false },
                            title = { Text(stringResource(R.string.profile_dialog_title)) },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val username = authState.userProfile?.username
                                    val email = authState.userProfile?.email

                                    Text(
                                        text = if (username.isNullOrBlank()) stringResource(R.string.profile_unknown_username) else username,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (email.isNullOrBlank()) stringResource(R.string.profile_no_email) else email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedButton(
                                        onClick = {
                                            showProfileDialog = false
                                            onIgnoredAppsClick()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.VisibilityOff, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.profile_ignored_apps))
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedButton(
                                        onClick = {
                                            showProfileDialog = false
                                            onMySubmissionsClick()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.profile_my_submissions))
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(16.dp))



                                    OutlinedButton(
                                        onClick = {
                                            showProfileDialog = false
                                            authViewModel.signOut()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.profile_sign_out))
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showProfileDialog = false }) {
                                    Text(stringResource(R.string.profile_close))
                                }
                            },
                            properties = DialogProperties(
                                dismissOnBackPress = false,
                                dismissOnClickOutside = false
                            ),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                    } else {
                        val title = if (authState.isSignedIn) stringResource(R.string.profile_missing_title) else stringResource(R.string.profile_not_signed_in_title)
                        val message = if (authState.isSignedIn)
                            stringResource(R.string.profile_missing_message)
                        else stringResource(R.string.profile_not_signed_in_message)

                        AlertDialog(
                            onDismissRequest = { showProfileDialog = false },
                            title = { Text(title) },
                            text = { Text(message) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showProfileDialog = false
                                        onSubmitClick() // Redirect to Auth/Submit flow which handles login
                                    }
                                ) {
                                    Text(if (authState.isSignedIn) stringResource(R.string.profile_fix_profile) else stringResource(R.string.profile_sign_in))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showProfileDialog = false }) {
                                    Text(stringResource(R.string.profile_close))
                                }
                            }
                        )
                    }
                }
            }


            if (showTutorial && currentTutorialStep != null && !state.isLoading) {
                TutorialOverlay(
                    currentStep = currentTutorialStep,
                    stepIndex = tutorialStepIndex,
                    totalSteps = TutorialStep.allSteps.size,
                    highlightRect = currentHighlightRect,
                    onNext = {
                        if (tutorialStepIndex < TutorialStep.allSteps.size - 1) {
                            tutorialStepIndex++
                        } else {
                            showTutorial = false
                            preferencesManager.setTutorialComplete()
                        }
                    },
                    onSkip = {
                        showTutorial = false
                        preferencesManager.setTutorialComplete()
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewDashboard() {
    DashboardScreen(
        onAppClick = { _, _ -> },
        viewModel = koinViewModel(),
        authViewModel = koinViewModel()
    )
}