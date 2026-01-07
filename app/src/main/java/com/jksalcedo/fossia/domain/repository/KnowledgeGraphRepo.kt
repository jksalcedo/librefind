package com.jksalcedo.fossia.domain.repository

import com.jksalcedo.fossia.domain.model.Alternative

/**
 * Repository interface for querying the semantic knowledge graph
 * 
 * Abstracts data from Wikidata SPARQL and Firebase Firestore.
 * Determines if apps are proprietary and provides FOSS alternatives.
 */
interface KnowledgeGraphRepo {
    /**
     * Checks if a package is identified as proprietary
     * 
     * Queries the knowledge graph (Firebase + Wikidata) to determine
     * if the given package is known proprietary software.
     * 
     * @param packageName Package to check
     * @return True if proprietary, false if FOSS or unknown
     */
    suspend fun isProprietary(packageName: String): Boolean
    
    /**
     * Fetches FOSS alternatives for a proprietary package
     * 
     * @param packageName Proprietary package to find alternatives for
     * @return List of Alternative objects, sorted by community score
     */
    suspend fun getAlternatives(packageName: String): List<Alternative>
    
    /**
     * Submits a new alternative proposal to the community database
     * 
     * @param proprietaryPackage The proprietary app to replace
     * @param alternativeId The FOSS alternative ID
     * @param userId User submitting the proposal
     * @return True if submission successful
     */
    suspend fun submitAlternative(
        proprietaryPackage: String,
        alternativeId: String,
        userId: String
    ): Boolean
    
    /**
     * Vote on an existing alternative
     * 
     * @param alternativeId The alternative to vote on
     * @param category Vote category ("privacy" or "usability")
     * @param userId User casting the vote
     * @return True if vote registered
     */
    suspend fun voteForAlternative(
        alternativeId: String,
        category: String,
        userId: String
    ): Boolean
}
