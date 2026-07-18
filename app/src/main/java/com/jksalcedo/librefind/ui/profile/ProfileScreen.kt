package com.jksalcedo.librefind.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jksalcedo.librefind.R
import com.jksalcedo.librefind.domain.model.Submission
import com.jksalcedo.librefind.domain.model.UserProfile
import com.jksalcedo.librefind.ui.common.FullScreenLoading
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String? = null,
    onBackClick: () -> Unit,
    onSubmissionClick: (String) -> Unit,
    onSignOutSuccess: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isOwnProfile) stringResource(R.string.profile_my_profile) else stringResource(
                            R.string.profile_user_profile
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (state.isOwnProfile) {
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.profile_sign_out)) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.signOut()
                                        onSignOutSuccess()
                                    },
                                    leadingIcon = { Icon(Icons.Default.ExitToApp, null) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> FullScreenLoading()
                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadProfile(userId) }) {
                            Text(stringResource(R.string.dashboard_retry))
                        }
                    }
                }

                state.profile != null -> {
                    ProfileContent(
                        profile = state.profile!!,
                        submissions = state.submissions,
                        isOwnProfile = state.isOwnProfile,
                        onSubmissionClick = onSubmissionClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    submissions: List<Submission>,
    isOwnProfile: Boolean,
    onSubmissionClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileHeader(profile)
        }

        item {
            ProfileStats(profile)
        }

        item {
            Text(
                text = if (isOwnProfile) stringResource(R.string.profile_my_submissions) else stringResource(
                    R.string.profile_user_submissions
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (submissions.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.my_submissions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(submissions) { submission ->
                SubmissionItem(submission, onClick = { onSubmissionClick(submission.id) })
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Column {
            Text(
                text = profile.username,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (!profile.badge.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = profile.badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Text(
                text = stringResource(R.string.profile_reputation, profile.reputationScore),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileStats(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = stringResource(R.string.submitted),
                value = profile.submissionCount.toString(),
                Icons.Default.FileUpload
            )
            StatItem(
                label = stringResource(R.string.approved),
                value = profile.approvedCount.toString(),
                Icons.Default.CheckCircle
            )
            StatItem(
                label = stringResource(R.string.rejected),
                value = profile.rejectedCount.toString(),
                Icons.Default.Cancel
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SubmissionItem(submission: Submission, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = submission.submittedApp.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = submission.type.name.replace("_", " "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = when (submission.status.name) {
                    "APPROVED" -> MaterialTheme.colorScheme.primaryContainer
                    "REJECTED" -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = submission.status.name,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
