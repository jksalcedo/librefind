package com.jksalcedo.fossia.domain.model

/**
 * Core domain model representing an installed application
 * with its classification status
 * 
 * @param packageName Unique identifier (e.g., "com.whatsapp")
 * @param label Human-readable name (e.g., "WhatsApp")
 * @param status Classification result
 * @param installerId Package name of installer (e.g., "com.android.vending", "org.fdroid.fdroid")
 * @param iconUri URI to app icon (can be null)
 * @param knownAlternatives Number of FOSS alternatives available
 */
data class AppItem(
    val packageName: String,
    val label: String,
    val status: AppStatus,
    val installerId: String?,
    val iconUri: String? = null,
    val knownAlternatives: Int = 0
)
