package com.jksalcedo.librefind.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SigningKeyVoteDto(
    val id: String? = null,
    @SerialName("package_name") val packageName: String,
    @SerialName("app_label") val appLabel: String? = null,
    @SerialName("sha256_digest") val sha256Digest: String,
    @SerialName("submitter_id") val submitterId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SigningKeyVoteWithProfileDto(
    val id: String? = null,
    @SerialName("package_name") val packageName: String,
    @SerialName("app_label") val appLabel: String? = null,
    @SerialName("sha256_digest") val sha256Digest: String,
    @SerialName("submitter_id") val submitterId: String,
    @SerialName("created_at") val createdAt: String? = null,
    val profile: SigningKeyVoteProfileDto? = null,
    @SerialName("endorse_count") val endorseCount: Int = 0,
    @SerialName("user_has_endorsed") val userHasEndorsed: Boolean = false
)

@Serializable
data class SigningKeyVoteProfileDto(
    val id: String,
    val username: String?,
    @SerialName("reputation_score") val reputationScore: Int = 0,
    val badge: String? = null
)
