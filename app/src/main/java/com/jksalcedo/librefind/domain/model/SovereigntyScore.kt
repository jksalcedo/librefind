package com.jksalcedo.librefind.domain.model

/**
 * User's sovereignty score representing migration progress
 *
 * @param totalApps Total number of user apps on device
 * @param fossCount Number of verified FOSS apps
 * @param proprietaryCount Number of verified proprietary apps
 * @param unknownCount Number of unclassified apps
 * @param ignoredCount Number of ignored apps
 */
data class SovereigntyScore(
    val totalApps: Int,
    val fossCount: Int,
    val proprietaryCount: Int,
    val unknownCount: Int,
    val ignoredCount: Int
) {
    /**
     * Percentage of FOSS apps (0-100)
     */
    val fossPercentage: Float
        get() = if (totalApps > 0) (fossCount.toFloat() / totalApps) * 100 else 0f

    /**
     * Sovereignty level based on percentage
     * - SOVEREIGN: 80%+ FOSS
     * - TRANSITIONING: 40-79% FOSS
     * - CAPTURED: <40% FOSS
     */
    val level: SovereigntyLevel
        get() = when {
            fossPercentage >= 80f -> SovereigntyLevel.SOVEREIGN
            fossPercentage >= 40f -> SovereigntyLevel.TRANSITIONING
            else -> SovereigntyLevel.CAPTURED
        }
}

enum class SovereigntyLevel {
    SOVEREIGN,      // Digital freedom achieved
    TRANSITIONING,  // Making progress
    CAPTURED        // Still in walled gardens
}
