package com.jksalcedo.librefind.domain.model

import java.util.Locale

data class Alternative(
    val id: String,
    val name: String,
    val packageName: String,
    val license: String,
    val repoUrl: String,
    val fdroidId: String,
    val iconUrl: String? = null,
    val category: String = "Other",
    val ratingAvg: Float = 0f,
    val ratingCount: Int = 0,
    val usabilityRating: Float = 0f,
    val privacyRating: Float = 0f,
    val featuresRating: Float = 0f,
    val userRating: Int? = null,
    val userUsabilityRating: Int? = null,
    val userPrivacyRating: Int? = null,
    val userFeaturesRating: Int? = null,
    val description: String = "",
    val website: String = "",
    val features: List<String> = emptyList(),
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList()
) {
    val displayRating: String
        get() = if (ratingCount > 0) String.format(Locale.getDefault(), "%.1f", ratingAvg) else "—"

    val displayUsabilityRating: String
        get() = if (usabilityRating > 0) String.format(Locale.getDefault(), "%.1f", usabilityRating) else "—"

    val displayPrivacyRating: String
        get() = if (privacyRating > 0) String.format(Locale.getDefault(), "%.1f", privacyRating) else "—"

    val displayFeaturesRating: String
        get() = if (featuresRating > 0) String.format(Locale.getDefault(), "%.1f", featuresRating) else "—"
}
