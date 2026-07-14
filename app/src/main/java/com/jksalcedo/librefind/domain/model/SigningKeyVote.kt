package com.jksalcedo.librefind.domain.model

data class SigningKeyVote(
    val id: String,
    val packageName: String,
    val appLabel: String,
    val sha256Digest: String,
    val submitterUid: String,
    val submitterUsername: String,
    val submitterReputation: Int = 0,
    val submitterBadge: String? = null,
    val endorseCount: Int = 0,
    val hasUserEndorsed: Boolean = false,
    val submittedAt: Long = 0L
)
