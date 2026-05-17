package com.jksalcedo.librefind.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jksalcedo.librefind.R
import com.jksalcedo.librefind.domain.model.Submission
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionDetailScreen(
    submissionId: String,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: CommunitySubmissionsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val submission = state.submissions.find { it.id == submissionId }
    var showDownvoteSheet by remember { mutableStateOf(false) }

    if (showDownvoteSheet && submission != null) {
        DownvoteSheet(
            onDismiss = { showDownvoteSheet = false },
            onConfirm = { reason, detail ->
                viewModel.castVote(submission, -1, reason, detail)
                showDownvoteSheet = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Submission Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (submission == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Submission not found or already processed.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Type: ${submission.type.name.replace("_", " ")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = submission.submittedApp.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Original Submitter: ${submission.submitterUsername}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (submission.lastEditedBy != null) {
                        Text(
                            text = "Last Edited by User: ${submission.lastEditedBy}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // App Details
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Package Name", submission.submittedApp.packageName)
                    DetailRow("Category", submission.category ?: "N/A")
                    DetailRow("Description", submission.submittedApp.description)
                    DetailRow("Repo URL", submission.submittedApp.repoUrl.ifBlank { "N/A" })
                    DetailRow("F-Droid ID", submission.submittedApp.fdroidId.ifBlank { "N/A" })
                    DetailRow("License", submission.submittedApp.license.ifBlank { "N/A" })

                    if (submission.proprietaryPackages.isNotBlank()) {
                        DetailRow("Targets (Proprietary Apps)", submission.proprietaryPackages)
                    }
                    if (submission.linkedAlternatives.isNotEmpty()) {
                        DetailRow("Linked Solutions (FOSS Apps)", submission.linkedAlternatives.joinToString(", "))
                    }
                }
            }

            // Community Votes
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.submission_votes_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VoteButton(
                            icon = if (submission.userVote == 1) Icons.Filled.ThumbUp
                                   else Icons.Outlined.ThumbUp,
                            label = "${submission.upvotes}",
                            active = submission.userVote == 1,
                            activeColor = MaterialTheme.colorScheme.primary,
                            contentDescription = stringResource(R.string.submission_upvote),
                            onClick = { viewModel.castVote(submission, 1) }
                        )
                        VoteButton(
                            icon = if (submission.userVote == -1) Icons.Filled.ThumbDown
                                   else Icons.Outlined.ThumbDown,
                            label = "${submission.downvotes}",
                            active = submission.userVote == -1,
                            activeColor = MaterialTheme.colorScheme.error,
                            contentDescription = stringResource(R.string.submission_downvote),
                            onClick = {
                                if (submission.userVote == -1) {
                                    viewModel.castVote(submission, -1)
                                } else {
                                    showDownvoteSheet = true
                                }
                            }
                        )
                    }
                }
            }

            // Actions
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onEditClick(submission.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Proposal")
            }
        }
    }
}

@Composable
private fun VoteButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    val tint = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tint
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
    }
}
