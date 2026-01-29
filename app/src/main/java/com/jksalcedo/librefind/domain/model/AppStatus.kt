package com.jksalcedo.librefind.domain.model

/**
 * Classification status for an installed application
 * 
 * - FOSS: Verified Free/Open Source Software
 * - PROP: Verified Proprietary Software
 * - UNKN: Analysis failed or not in database
 */
enum class AppStatus {
    FOSS,   // Verified Free/Open Source
    PROP,   // Verified Proprietary
    UNKN   // Analysis Failed / Not in DB
    ,
    IGNORED;

    /**
     * Helper for sorting priority
     * Proprietary apps appear first to encourage migration action
     */
    val sortWeight: Int
        get() = when(this) {
            PROP -> 1  // Highest priority - needs replacement
            UNKN -> 2  // Medium priority - needs investigation
            FOSS -> 3  // Lowest priority - already sovereign
            IGNORED -> 4
        }
}
