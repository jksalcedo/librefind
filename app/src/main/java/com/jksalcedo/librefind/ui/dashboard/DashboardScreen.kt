package com.jksalcedo.librefind.ui.dashboard

import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.jksalcedo.librefind.domain.model.AppStatus
import com.jksalcedo.librefind.ui.auth.AuthViewModel
import com.jksalcedo.librefind.ui.dashboard.components.GaugeDetailsDialog
import com.jksalcedo.librefind.ui.dashboard.components.ScanList
import com.jksalcedo.librefind.ui.dashboard.components.SovereigntyGauge
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAppClick: (String, String) -> Unit,
    onSubmitClick: () -> Unit = {},
    onMySubmissionsClick: () -> Unit = {},
    onIgnoredAppsClick: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
//    var showFilter by remember { mutableStateOf(false) }

    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search apps...") },
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
                            text = authState.userProfile?.username ?: "LibreFind",
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
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
//                        IconButton(onClick = { showFilter = true }) {
//                            Icon(Icons.Default.FilterList, contentDescription = "Filter List")
//                            DropdownMenu(
//                                expanded = showFilter,
//                                onDismissRequest = { showFilter = false}
//                            ) {
//                                DropdownMenuItem(
//                                    text = { Text("") },
//                                    onClick = {
//                                        showFilter = false
//                                        ()
//                                    },
//                                    leadingIcon = {
//                                        Icon(Icons.Default.VisibilityOff, contentDescription = null)
//                                    }
//                                )
//                            }
//                        }
                        IconButton(onClick = { showProfileDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile"
                            )
                        }
                    }
                },
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSubmitClick) {
                Icon(Icons.Default.Add, contentDescription = "Submit App")
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
                            text = "Error",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.scan() }) {
                            Text("Retry")
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
                                    modifier = Modifier.padding(16.dp)
                                ) { showDialog = true }
                            }
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (state.searchQuery.isNotEmpty()) "No matching apps" else "No apps found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        ScanList(
                            apps = if (state.statusFilter == null) {
                                state.apps.filter { it.status != AppStatus.IGNORED }
                            } else state.apps,
                            onAppClick = onAppClick,
                            onIgnoreClick = { packageName -> viewModel.ignoreApp(packageName) },
                            onRefresh = { viewModel.scan() },
                            isRefreshing = state.isLoading,
                            modifier = Modifier.fillMaxSize(),
                            headerContent = {
                                state.sovereigntyScore?.let { score ->
                                    SovereigntyGauge(
                                        score = score,
                                        currentFilter = state.statusFilter,
                                        onFilterClick = { status -> viewModel.setStatusFilter(status) },
                                        modifier = Modifier.padding(bottom = 8.dp)
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
                        title = { Text("Profile & Settings") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val username = authState.userProfile?.username
                                val email = authState.userProfile?.email

                                Text(
                                    text = if (username.isNullOrBlank()) "Unknown Username" else username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (email.isNullOrBlank()) "No Email" else email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))


                                val context = LocalContext.current
                                val packageManager = context.packageManager
                                val packageName = context.packageName

                                val packageInfo =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        packageManager.getPackageInfo(
                                            packageName,
                                            PackageManager.PackageInfoFlags.of(0)
                                        )
                                    } else {
                                        @Suppress("DEPRECATION")
                                        packageManager.getPackageInfo(packageName, 0)
                                    }

                                val version = packageInfo.versionName ?: "Unknown"


                                Text(
                                    text = "LibreFind $version",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val uriHandler = LocalUriHandler.current

                                OutlinedButton(
                                    onClick = { uriHandler.openUri("https://github.com/jksalcedo/librefind/issues") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.BugReport, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Report a Bug")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = { uriHandler.openUri("https://t.me/librefind") },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Default.Group, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Join Community")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        showProfileDialog = false
                                        onIgnoredAppsClick()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ignored Apps")
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
                                    Text("My Submissions")
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
                                    Text("Sign Out")
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showProfileDialog = false }) {
                                Text("Close")
                            }
                        },
                        properties = DialogProperties(
                            dismissOnBackPress = false,
                            dismissOnClickOutside = false
                        ),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                } else {
                    val title = if (authState.isSignedIn) "Profile Missing" else "Not Signed In"
                    val message = if (authState.isSignedIn)
                        "Your profile could not be found. Please try signing out and back in, or contact support."
                    else "Please sign in to view your profile."

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
                                Text(if (authState.isSignedIn) "Fix Profile" else "Sign In")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showProfileDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
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