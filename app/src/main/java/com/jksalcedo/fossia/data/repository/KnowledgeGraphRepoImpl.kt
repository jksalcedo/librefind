package com.jksalcedo.fossia.data.repository

import com.jksalcedo.fossia.data.remote.firebase.FirestoreService
import com.jksalcedo.fossia.domain.model.Alternative
import com.jksalcedo.fossia.domain.repository.KnowledgeGraphRepo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of KnowledgeGraphRepo
 * 
 * Currently uses Firebase Firestore as the primary knowledge source.
 * In future iterations, this could integrate Wikidata SPARQL queries.
 */
@Singleton
class KnowledgeGraphRepoImpl @Inject constructor(
    private val firestoreService: FirestoreService
) : KnowledgeGraphRepo {

    override suspend fun isProprietary(packageName: String): Boolean {
        return firestoreService.isProprietaryPackage(packageName)
    }

    override suspend fun getAlternatives(packageName: String): List<Alternative> {
        return firestoreService.getAlternatives(packageName)
    }

    override suspend fun submitAlternative(
        proprietaryPackage: String,
        alternativeId: String,
        userId: String
    ): Boolean {
        return firestoreService.submitProposal(
            proprietaryPackage = proprietaryPackage,
            alternativeId = alternativeId,
            userId = userId
        )
    }

    override suspend fun voteForAlternative(
        alternativeId: String,
        category: String,
        userId: String
    ): Boolean {
        return firestoreService.voteForAlternative(
            alternativeId = alternativeId,
            category = category,
            userId = userId
        )
    }
}
