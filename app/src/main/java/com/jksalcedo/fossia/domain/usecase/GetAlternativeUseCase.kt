package com.jksalcedo.fossia.domain.usecase

import com.jksalcedo.fossia.domain.model.Alternative
import com.jksalcedo.fossia.domain.repository.KnowledgeGraphRepo
import javax.inject.Inject

/**
 * Use case: Get FOSS alternatives for a proprietary app
 * 
 * Fetches and returns alternatives sorted by community score.
 */
class GetAlternativeUseCase @Inject constructor(
    private val knowledgeGraphRepo: KnowledgeGraphRepo
) {
    /**
     * Get alternatives for a package
     * 
     * @param packageName Proprietary package to find alternatives for
     * @return List of alternatives sorted by score (highest first)
     */
    suspend operator fun invoke(packageName: String): List<Alternative> {
        return knowledgeGraphRepo.getAlternatives(packageName)
            .sortedByDescending { it.totalScore }
    }
}
