package com.jksalcedo.librefind.domain.model

/**
 * Core domain model representing an installed application
 * with its classification status
 *
 * @param packageName Unique identifier (e.g., "com.whatsapp")
 * @param label Human-readable name (e.g., "WhatsApp")
 * @param status Classification result
 * @param installerId Package name of installer (e.g., "com.android.vending", "org.fdroid.fdroid")
 * @param icon app icon (can be null)
 * @param knownAlternatives Number of FOSS alternatives available
 * @param isSystemPackage Flag indicating if the package is a system app (based on PackageManager flags)
 */
data class AppItem(
    val packageName: String,
    val label: String,
    val status: AppStatus,
    val installerId: String?,
    val icon: Int? = null,
    val knownAlternatives: Int = 0,
    val isUserReclassified: Boolean = false,
    val isSystemPackage: Boolean = false
)
