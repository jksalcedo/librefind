package com.jksalcedo.fossia.data.remote.firebase.dto

import com.google.gson.annotations.SerializedName
import com.jksalcedo.fossia.domain.model.Alternative

/**
 * Data Transfer Object for Firestore "foss_solutions" collection
 * 
 * Firestore Schema:
 * - doc_id: Sanitized package name
 * - name: Human-readable name
 * - license: FOSS license type
 * - repo_url: Source code repository
 * - fdroid_id: F-Droid package ID
 * - votes: Map of vote categories to counts
 */
data class FossSolutionDto(
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("license")
    val license: String = "",
    
    @SerializedName("repo_url")
    val repoUrl: String = "",
    
    @SerializedName("fdroid_id")
    val fdroidId: String = "",
    
    @SerializedName("icon_url")
    val iconUrl: String? = null,
    
    @SerializedName("package_name")
    val packageName: String = "",
    
    @SerializedName("description")
    val description: String = "",
    
    @SerializedName("votes")
    val votes: Map<String, Int> = emptyMap()
) {
    /**
     * Convert DTO to domain model
     */
    fun toDomain(id: String): Alternative {
        return Alternative(
            id = id,
            name = name,
            packageName = packageName,
            license = license,
            repoUrl = repoUrl,
            fdroidId = fdroidId,
            iconUrl = iconUrl,
            privacyVotes = votes["privacy"] ?: 0,
            usabilityVotes = votes["usability"] ?: 0,
            description = description
        )
    }
}
