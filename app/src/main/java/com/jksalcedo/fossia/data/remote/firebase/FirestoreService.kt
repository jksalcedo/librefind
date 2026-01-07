package com.jksalcedo.fossia.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.jksalcedo.fossia.data.remote.firebase.dto.FossSolutionDto
import com.jksalcedo.fossia.data.remote.firebase.dto.ProprietaryTargetDto
import com.jksalcedo.fossia.domain.model.Alternative
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Firestore service for knowledge graph operations
 * 
 * Handles all Firestore queries and mutations.
 */
@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_PROPRIETARY = "proprietary_targets"
        private const val COLLECTION_FOSS = "foss_solutions"
        private const val COLLECTION_PROPOSALS = "alternative_proposals"
    }
    
    /**
     * Checks if a package is in the proprietary database
     * 
     * @param packageName Package to check
     * @return True if found in proprietary collection
     */
    suspend fun isProprietaryPackage(packageName: String): Boolean {
        return try {
            val sanitized = sanitizePackageName(packageName)
            val doc = firestore.collection(COLLECTION_PROPRIETARY)
                .document(sanitized)
                .get()
                .await()
            
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets alternatives for a proprietary package
     * 
     * @param packageName Proprietary package
     * @return List of Alternative objects
     */
    suspend fun getAlternatives(packageName: String): List<Alternative> {
        return try {
            val sanitized = sanitizePackageName(packageName)
            val doc = firestore.collection(COLLECTION_PROPRIETARY)
                .document(sanitized)
                .get()
                .await()
            
            if (!doc.exists()) return emptyList()
            
            val target = doc.toObject(ProprietaryTargetDto::class.java) ?: return emptyList()
            
            // Fetch each alternative
            target.alternatives.mapNotNull { altId ->
                fetchFossSolution(altId)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Fetches a FOSS solution by ID
     */
    private suspend fun fetchFossSolution(id: String): Alternative? {
        return try {
            val doc = firestore.collection(COLLECTION_FOSS)
                .document(id)
                .get()
                .await()
            
            if (!doc.exists()) return null
            
            val dto = doc.toObject(FossSolutionDto::class.java) ?: return null
            dto.toDomain(id)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Submits a new alternative proposal
     * 
     * @return True if successful
     */
    suspend fun submitProposal(
        proprietaryPackage: String,
        alternativeId: String,
        userId: String
    ): Boolean {
        return try {
            val proposal = hashMapOf(
                "proprietary_package" to proprietaryPackage,
                "alternative_id" to alternativeId,
                "user_id" to userId,
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            
            firestore.collection(COLLECTION_PROPOSALS)
                .add(proposal)
                .await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Votes for an alternative
     * 
     * @param alternativeId Alternative to vote for
     * @param category "privacy" or "usability"
     * @param userId User casting vote
     * @return True if successful
     */
    suspend fun voteForAlternative(
        alternativeId: String,
        category: String,
        userId: String
    ): Boolean {
        return try {
            val docRef = firestore.collection(COLLECTION_FOSS)
                .document(alternativeId)
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val votes = snapshot.get("votes") as? Map<String, Int> ?: emptyMap()
                val currentVotes = votes[category] ?: 0
                
                val updatedVotes = votes.toMutableMap()
                updatedVotes[category] = currentVotes + 1
                
                transaction.update(docRef, "votes", updatedVotes)
            }.await()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Sanitizes package name for use as Firestore document ID
     * Replaces dots with underscores
     */
    private fun sanitizePackageName(packageName: String): String {
        return packageName.replace(".", "_")
    }
}
