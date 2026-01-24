package com.jksalcedo.librefind.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SolutionDto(
    @SerialName("package_name") val packageName: String,
    val name: String,
    val description: String,
    val category: String? = null,
    @SerialName("icon_url") val iconUrl: String,
    @SerialName("fdroid_id") val fdroidId: String,
    @SerialName("repo_url") val repoUrl: String,
    val license: String,
    val pros: List<String>? = emptyList(),
    val cons: List<String>? = emptyList(),
    val features: List<String>? = emptyList(),
    val votes: JsonElement? = null, // JSONB
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class TargetDto(
    @SerialName("package_name") val packageName: String,
    val name: String,
    val category: String? = null,
    @SerialName("icon_url") val iconUrl: String,
    val alternatives: List<String>? = emptyList() // Array of package_names
)

@Serializable
data class UserSubmissionDto(
    val id: String? = null, // UUID, auto-generated
    @SerialName("app_name") val appName: String,
    @SerialName("app_package") val appPackage: String,
    val description: String,
    @SerialName("proprietary_package") val proprietaryPackage: String? = null,
    @SerialName("repo_url") val repoUrl: String? = null,
    @SerialName("fdroid_id") val fdroidId: String? = null,
    val license: String? = null,
    val status: String = "PENDING",
    @SerialName("submitter_id") val submitterId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null
)

@Serializable
data class ProfileDto(
    val id: String, // UUID
    val username: String?,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("submission_count") val submissionCount: Int = 0,
    @SerialName("approved_count") val approvedCount: Int = 0
)

@Serializable
data class UserVoteDto(
    @SerialName("user_id") val userId: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("vote_type") val voteType: String, // 'privacy' or 'usability'
    val value: Int // 1 or -1
)
