package com.jksalcedo.librefind.data.local.user

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_user_votes")
data class CachedUserVoteEntity(
    @PrimaryKey
    val solutionId: String,
    val voteType: Int, // 1 for upvote, -1 for downvote
    val synced: Boolean = false
)
