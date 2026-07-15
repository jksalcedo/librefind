package com.jksalcedo.librefind.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentDto(
    val id: String? = null,
    @SerialName("target_id") val targetId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CommentWithProfileDto(
    val id: String,
    @SerialName("target_id") val targetId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    val profile: ProfileDto? = null
)
