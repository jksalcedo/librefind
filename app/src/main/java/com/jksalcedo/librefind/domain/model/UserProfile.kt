package com.jksalcedo.librefind.domain.model

data class UserProfile(
    val uid: String,
    val username: String,
    val email: String,
    val joinedAt: Long = System.currentTimeMillis(),
    val submissionCount: Int = 0,
    val approvedCount: Int = 0,
    val rejectedCount: Int = 0
)
