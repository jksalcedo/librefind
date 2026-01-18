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
    val ratingAvg: Float = 0f,
    val ratingCount: Int = 0,
    val userRating: Int? = null,
    val description: String = "",
    val website: String = "",
    val features: List<String> = emptyList(),
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList()
) {
    val displayRating: String
        get() = if (ratingCount > 0) String.format(Locale.getDefault(), "%.1f", ratingAvg) else "—"
}
