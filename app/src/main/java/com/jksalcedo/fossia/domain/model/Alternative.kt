package com.jksalcedo.fossia.domain.model

/**
 * Represents a FOSS alternative to proprietary software
 * 
 * @param id Sanitized package name used as document ID
 * @param name Human-readable name
 * @param packageName F-Droid package identifier
 * @param license FOSS license type (e.g., "GPLv3", "Apache-2.0")
 * @param repoUrl Source code repository URL
 * @param fdroidId Official F-Droid package ID
 * @param iconUrl Remote icon URL
 * @param privacyVotes Community votes for privacy
 * @param usabilityVotes Community votes for usability
 * @param description Short description of the app
 */
data class Alternative(
    val id: String,
    val name: String,
    val packageName: String,
    val license: String,
    val repoUrl: String,
    val fdroidId: String,
    val iconUrl: String? = null,
    val privacyVotes: Int = 0,
    val usabilityVotes: Int = 0,
    val description: String = ""
) {
    /**
     * Total community score
     */
    val totalScore: Int
        get() = privacyVotes + usabilityVotes
}
