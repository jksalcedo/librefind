package com.jksalcedo.librefind.domain.usecase

import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.model.SubmissionType


/**
 * Use case: Submit a new FOSS alternative proposal
 * 
 * Part of the community governance system.
 * Users can propose new alternatives for review.
 */
class SubmitProposalUseCase(
    private val appRepository: AppRepository
) {
    /**
     * Submit an alternative proposal
     * 
     * @param proprietaryPackage The proprietary app to replace
     * @param alternativeId The proposed FOSS alternative (package name)
     * @param userId User making the submission
     * @return True if submission successful, false otherwise
     */
    suspend operator fun invoke(
        proprietaryPackage: String,
        alternativeId: String,
        appName: String,
        description: String,
        repoUrl: String,
        fdroidId: String,
        license: String,
        userId: String,
        alternatives: List<String> = emptyList(),
        submissionType: SubmissionType,
        category: String = ""
    ): Result<Unit> {
        if (alternativeId.isBlank() || userId.isBlank()) {
            return Result.failure(IllegalArgumentException("Missing required fields"))
        }
        
        return appRepository.submitAlternative(
            proprietaryPackage = proprietaryPackage,
            alternativePackage = alternativeId,
            appName = appName,
            description = description,
            repoUrl = repoUrl,
            fdroidId = fdroidId,
            license = license,
            userId = userId,
            alternatives = alternatives,
            submissionType = submissionType,
            category = category
        )
    }
}
