package com.jksalcedo.librefind.domain.model

/**
 * User's sovereignty score representing migration progress
 *
 * @param totalApps Total number of user apps on device
 * @param fossCount Number of verified FOSS apps
 * @param proprietaryCount Number of verified proprietary apps
 * @param unknownCount Number of unclassified apps
 * @param ignoredCount Number of ignored apps
 * @param pwaCount Number of PWA apps
 */
data class SovereigntyScore(
    val totalApps: Int,
    val fossCount: Int,
    val proprietaryCount: Int,
    val unknownCount: Int,
    val ignoredCount: Int,
    val pendingCount: Int = 0,
    val pwaCount: Int = 0
) {
    /**
     * Percentage of FOSS apps (0-100)
     */
    val fossPercentage: Float
        get() = if (totalApps - ignoredCount > 0) (fossCount.toFloat() / (totalApps - ignoredCount)) * 100 else 0f

    /**
     * Percentage of proprietary apps (0-100)
     */
    val proprietaryPercentage: Float
        get() = if (totalApps - ignoredCount > 0) (proprietaryCount.toFloat() / (totalApps - ignoredCount)) * 100 else 0f

    /**
     * Percentage of unknown apps (0-100)
     */
    val unknownPercentage: Float
        get() = if (totalApps - ignoredCount > 0) (unknownCount.toFloat() / (totalApps - ignoredCount)) * 100 else 0f

    /**
     * Percentage of pending apps (0-100)
     */
    val pendingPercentage: Float
        get() = if (totalApps - ignoredCount > 0) (pendingCount.toFloat() / (totalApps - ignoredCount)) * 100 else 0f

    /**
     * Percentage of PWA apps (0-100)
     */
    val pwaPercentage: Float
        get() = if (totalApps - ignoredCount > 0) (pwaCount.toFloat() / (totalApps - ignoredCount)) * 100 else 0f

    /**
     * Percentage of ignored apps (0 because ignored apps get ignored)
     */
    val ignoredPercentage: Float
        get() = 0f

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
