package com.jksalcedo.librefind.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SolutionDto(
    @SerialName("package_name") val packageName: String,
    val name: String,
    val description: String,
    val category: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("fdroid_id") val fdroidId: String? = null,
    @SerialName("repo_url") val repoUrl: String? = null,
    val license: String,
    val pros: List<String>? = emptyList(),
    val cons: List<String>? = emptyList(),
    val features: List<String>? = emptyList(),
    @SerialName("rating_privacy") val ratingPrivacy: Float? = 0f,
    @SerialName("rating_usability") val ratingUsability: Float? = 0f,
    @SerialName("rating_features") val ratingFeatures: Float? = 0f,
    @SerialName("vote_count") val voteCount: Int? = 0,
    @SerialName("created_at") val createdAt: String? = null
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
    val alternatives: List<String>? = null,
    @SerialName("submission_type") val submissionType: String? = null,
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
    @SerialName("user_id") val userId: String = "",
    @SerialName("package_name") val packageName: String = "",
    @SerialName("vote_type") val voteType: String,
    val value: Int
)

@Serializable
data class UserReportDto(
    val id: String? = null,
    val title: String,
    val description: String,
    @SerialName("report_type") val reportType: String,
    val status: String = "OPEN",
    val priority: String = "LOW",
    @SerialName("submitter_id") val submitterId: String,
    @SerialName("admin_response") val adminResponse: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AppScanStatsDto(
    @SerialName("device_id") val deviceId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("foss_count") val fossCount: Int,
    @SerialName("proprietary_count") val proprietaryCount: Int,
    @SerialName("unknown_count") val unknownCount: Int,
    @SerialName("total_apps") val totalApps: Int,
    @SerialName("app_version") val appVersion: String? = null
)

@Serializable
data class UserLinkingSubmissionsDto(
    val id: String? = null,
    @SerialName("proprietary_package") val proprietaryPackage: String,
    val alternatives: List<String>,
    @SerialName("submitter_id") val submitterId: String,
    val status: String = "PENDING",
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AppReport(
    @SerialName("user_id") val userId: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("issue_type") val issueType: String,
    @SerialName("description") val description: String,
    @SerialName("status") val status: String = "pending"
)