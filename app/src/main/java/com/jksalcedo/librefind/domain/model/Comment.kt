package com.jksalcedo.librefind.domain.model

data class Comment(
    val id: String,
    val targetId: String,
    val userId: String,
    val username: String?,
    val avatarUrl: String?,
    val badge: String?,
    val content: String,
    val createdAt: Long
)
