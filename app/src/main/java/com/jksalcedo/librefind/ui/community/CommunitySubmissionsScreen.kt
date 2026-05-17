package com.jksalcedo.librefind.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jksalcedo.librefind.R
import com.jksalcedo.librefind.domain.model.Submission
import com.jksalcedo.librefind.domain.model.SubmissionType
import com.jksalcedo.librefind.ui.common.FullScreenLoading
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitySubmissionsScreen(
    onBackClick: () -> Unit,
    onSubmissionClick: (String) -> Unit,
    viewModel: CommunitySubmissionsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val filteredSubmissions by remember(state.submissions, state.searchQuery, state.filterType) {
        derivedStateOf {
            var result = state.submissions
            if (state.filterType != null) {
                result = result.filter { it.type == state.filterType }
            }
            if (state.searchQuery.isNotBlank()) {
                result = result.filter { submission ->
                    submission.submittedApp.name.contains(state.searchQuery, ignoreCase = true) ||
                    submission.submittedApp.packageName.contains(state.searchQuery, ignoreCase = true) ||
                    submission.proprietaryPackages.contains(state.searchQuery, ignoreCase = true) ||
                    submission.submitterUsername.contains(state.searchQuery, ignoreCase = true)
                }
            }
            result
        }
    }

    var submissionToReject by remember { mutableStateOf<Submission?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var submissionToDownvote by remember { mutableStateOf<Submission?>(null) }

    if (submissionToReject != null) {
        Dialog(onDismissRequest = { submissionToReject = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.reject_reason_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text(stringResource(R.string.reject_reason_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { submissionToReject = null }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                        Button(
                            onClick = {
                                submissionToReject?.let {
                                    viewModel.rejectSubmission(it, rejectionReason)
                                }
                                submissionToReject = null
                                rejectionReason = ""
                            },
                            enabled = rejectionReason.isNotBlank()
                        ) {
                            Text(stringResource(R.string.action_reject))
                        }
                    }
                }
            }
        }
    }

    submissionToDownvote?.let { submission ->
        DownvoteSheet(
            onDismiss = { submissionToDownvote = null },
            onConfirm = { reason, detail ->
                viewModel.castVote(
                    submission = submission,
                    vote = -1,
                    reason = reason,
                    reasonDetail = detail
                )
                submissionToDownvote = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.community_submissions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    placeholder = { Text(stringResource(R.string.dashboard_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
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
                    FullScreenLoading()
                }

                state.error != null -> {
                    Text(
                        text = state.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                filteredSubmissions.isEmpty() -> {
                    Text(
                        text = if (state.searchQuery.isEmpty())
                            stringResource(R.string.community_submissions_empty)
                        else
                            stringResource(R.string.submit_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.loadSubmissions(forceRefresh = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                top = 8.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    item {
                                        FilterChip(
                                            selected = state.filterType == null,
                                            onClick = { viewModel.setFilterType(null) },
                                            label = { Text("All") }
                                        )
                                    }
                                    items(SubmissionType.entries) { type ->
                                        FilterChip(
                                            selected = state.filterType == type,
                                            onClick = { viewModel.setFilterType(if (state.filterType == type) null else type) },
                                            label = { Text(type.name.replace("_", " ")) }
                                        )
                                    }
                                }
                            }
                            items(filteredSubmissions) { submission ->
                                CommunitySubmissionItem(
                                    submission = submission,
                                    onClick = { onSubmissionClick(submission.id) },
                                    onUpvote = { viewModel.castVote(submission, 1) },
                                    onDownvote = { submissionToDownvote = submission }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommunitySubmissionItem(
    submission: Submission,
    onClick: () -> Unit,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = submission.submittedApp.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Submitted by ${submission.submitterUsername}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = submission.type.name.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onUpvote, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (submission.userVote == 1) Icons.Filled.ThumbUp
                                         else Icons.Outlined.ThumbUp,
                            contentDescription = stringResource(R.string.submission_upvote),
                            tint = if (submission.userVote == 1) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "${submission.upvotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onDownvote, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (submission.userVote == -1) Icons.Filled.ThumbDown
                                          else Icons.Outlined.ThumbDown,
                            contentDescription = stringResource(R.string.submission_downvote),
                            tint = if (submission.userVote == -1) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "${submission.downvotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
