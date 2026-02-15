package com.jksalcedo.librefind.domain.model

data class Submission(
    val id: String = "",
    val type: SubmissionType,
    val proprietaryPackages: String,
    val submittedApp: SubmittedApp,
    val submitterUid: String,
    val submitterUsername: String,
    val submittedAt: Long = System.currentTimeMillis(),
    val status: SubmissionStatus = SubmissionStatus.PENDING,
    val rejectionReason: String? = null,
    val linkedAlternatives: List<String> = emptyList()
)

enum class SubmissionType { NEW_ALTERNATIVE, NEW_PROPRIETARY, LINKING }
enum class SubmissionStatus { PENDING, APPROVED, REJECTED }

data class SubmittedApp(
    val name: String,
    val packageName: String,
    val repoUrl: String = "",
    val fdroidId: String = "",
    val description: String = "",
    val license: String = ""
)
