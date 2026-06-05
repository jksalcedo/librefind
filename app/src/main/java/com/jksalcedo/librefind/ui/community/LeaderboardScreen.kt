package com.jksalcedo.librefind.ui.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jksalcedo.librefind.R
import com.jksalcedo.librefind.domain.model.UserProfile
import com.jksalcedo.librefind.ui.common.FullScreenLoading
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: LeaderboardViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.community_leaderboard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.topContributors.isEmpty() -> {
                    Text(
                        text = "No contributors yet",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(state.topContributors) { index, user ->
                            ContributorItem(
                                rank = index + 1,
                                user = user,
                                onClick = { onUserClick(user.uid) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContributorItem(
    rank: Int,
    user: UserProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                if (rank <= 3) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = when (rank) {
                            1 -> Color(0xFFFFD700) // Gold
                            2 -> Color(0xFFC0C0C0) // Silver
                            else -> Color(0xFFCD7F32) // Bronze
                        },
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                } else {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!user.badge.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = user.badge,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = user.reputationScore.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "points",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
