package com.jksalcedo.librefind.domain.usecase

import com.jksalcedo.librefind.domain.repository.AppRepository

/**
 * Use case: Update an existing submission
 *
 * Allows users to edit their pending or rejected submissions.
 * Resets status to PENDING upon update.
 */
class UpdateSubmissionUseCase(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(
        id: String,
        proprietaryPackage: String,
        alternativeId: String,
        appName: String,
        description: String,
        repoUrl: String,
        fdroidId: String,
        license: String,
        alternatives: List<String> = emptyList(),
        category: String = ""
    ): Result<Unit> {
        if (id.isBlank() || alternativeId.isBlank()) {
            return Result.failure(IllegalArgumentException("Missing required fields"))
        }

        return appRepository.updateSubmission(
            id = id,
            proprietaryPackage = proprietaryPackage,
            alternativePackage = alternativeId,
            appName = appName,
            description = description,
            repoUrl = repoUrl,
            fdroidId = fdroidId,
            license = license,
            alternatives = alternatives,
            category = category
        )
    }
}
