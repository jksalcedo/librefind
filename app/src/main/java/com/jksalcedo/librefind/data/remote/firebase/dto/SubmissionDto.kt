package com.jksalcedo.librefind.data.remote.firebase.dto

import com.google.firebase.firestore.PropertyName
import com.jksalcedo.librefind.domain.model.Submission
import com.jksalcedo.librefind.domain.model.SubmissionStatus
import com.jksalcedo.librefind.domain.model.SubmissionType
import com.jksalcedo.librefind.domain.model.SubmittedApp

data class SubmissionDto(
    @get:PropertyName("type") @set:PropertyName("type")
    var type: String = "",
    @get:PropertyName("proprietary_package") @set:PropertyName("proprietary_package")
    var proprietaryPackage: String = "",
    @get:PropertyName("submitter_uid") @set:PropertyName("submitter_uid")
    var submitterUid: String = "",
    @get:PropertyName("submitter_username") @set:PropertyName("submitter_username")
    var submitterUsername: String = "",
    @get:PropertyName("submitted_at") @set:PropertyName("submitted_at")
    var submittedAt: Long = 0,
    @get:PropertyName("status") @set:PropertyName("status")
    var status: String = "PENDING",
    @get:PropertyName("app_name") @set:PropertyName("app_name")
    var appName: String = "",
    @get:PropertyName("app_package") @set:PropertyName("app_package")
    var appPackage: String = "",
    @get:PropertyName("repo_url") @set:PropertyName("repo_url")
    var repoUrl: String = "",
    @get:PropertyName("fdroid_id") @set:PropertyName("fdroid_id")
    var fdroidId: String = "",
    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",
    @get:PropertyName("license") @set:PropertyName("license")
    var license: String = ""
) {
    fun toDomain(id: String) = Submission(
        id = id,
        type = SubmissionType.valueOf(type),
        proprietaryPackages = proprietaryPackage,
        submitterUid = submitterUid,
        submitterUsername = submitterUsername,
        submittedAt = submittedAt,
        status = SubmissionStatus.valueOf(status),
        submittedApp = SubmittedApp(
            name = appName,
            packageName = appPackage,
            repoUrl = repoUrl,
            fdroidId = fdroidId,
            description = description,
            license = license
        )
    )

    companion object {
        fun fromDomain(submission: Submission) = SubmissionDto(
            type = submission.type.name,
            proprietaryPackage = submission.proprietaryPackages,
            submitterUid = submission.submitterUid,
            submitterUsername = submission.submitterUsername,
            submittedAt = submission.submittedAt,
            status = submission.status.name,
            appName = submission.submittedApp.name,
            appPackage = submission.submittedApp.packageName,
            repoUrl = submission.submittedApp.repoUrl,
            fdroidId = submission.submittedApp.fdroidId,
            description = submission.submittedApp.description,
            license = submission.submittedApp.license
        )
    }
}
