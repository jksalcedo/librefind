package com.jksalcedo.fossia.domain.usecase

import com.jksalcedo.fossia.domain.repository.KnowledgeGraphRepo
import javax.inject.Inject

/**
 * Use case: Submit a new FOSS alternative proposal
 * 
 * Part of the community governance system.
 * Users can propose new alternatives for review.
 */
class SubmitProposalUseCase @Inject constructor(
    private val knowledgeGraphRepo: KnowledgeGraphRepo
) {
    /**
     * Submit an alternative proposal
     * 
     * @param proprietaryPackage The proprietary app to replace
     * @param alternativeId The proposed FOSS alternative
     * @param userId User making the submission
     * @return True if submission successful, false otherwise
     */
    suspend operator fun invoke(
        proprietaryPackage: String,
        alternativeId: String,
        userId: String
    ): Boolean {
        // Validate inputs
        if (proprietaryPackage.isBlank() || alternativeId.isBlank() || userId.isBlank()) {
            return false
        }
        
        return knowledgeGraphRepo.submitAlternative(
            proprietaryPackage = proprietaryPackage,
            alternativeId = alternativeId,
            userId = userId
        )
    }
}
