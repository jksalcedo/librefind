package com.jksalcedo.librefind.data.repository

import com.jksalcedo.librefind.data.local.catalog.CatalogSolutionEntity
import com.jksalcedo.librefind.data.local.catalog.CatalogTargetEntity
import com.jksalcedo.librefind.domain.model.Alternative

/**
 * Maps catalog rows to the domain [Alternative] model so the details screen can
 * render the same UI whether the data came from Supabase or the offline catalog.
 */
internal fun CatalogSolutionEntity.toAlternative(): Alternative = Alternative(
    id = id,
    name = name,
    packageName = packageName,
    license = license.orEmpty(),
    repoUrl = repositoryUrl.orEmpty(),
    fdroidId = fdroidUrl.orEmpty(),
    iconUrl = iconUrl,
    matchUpvotes = upvotes,
    matchDownvotes = downvotes,
    matchScore = upvotes - downvotes,
    description = description.orEmpty(),
    website = websiteUrl.orEmpty()
)

internal fun CatalogTargetEntity.toAlternative(): Alternative = Alternative(
    id = id,
    name = name,
    packageName = packageName,
    license = "Proprietary",
    repoUrl = "",
    fdroidId = "",
    category = categories.firstJsonArrayEntry()
)

private fun String.firstJsonArrayEntry(): String {
    return trim()
        .removePrefix("[")
        .removeSuffix("]")
        .split(',')
        .map { it.trim().trim('"') }
        .firstOrNull { it.isNotBlank() }
        ?: "Other"
}
