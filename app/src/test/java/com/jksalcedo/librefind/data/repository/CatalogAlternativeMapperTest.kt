package com.jksalcedo.librefind.data.repository

import com.jksalcedo.librefind.data.local.catalog.CatalogSolutionEntity
import com.jksalcedo.librefind.data.local.catalog.CatalogTargetEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogAlternativeMapperTest {

    @Test
    fun `catalog solution maps to alternative with match score`() {
        val solution = CatalogSolutionEntity(
            id = "s1",
            packageName = "com.example.solution",
            name = "Solution",
            description = "Offline solution",
            license = "GPL-3.0",
            iconUrl = "https://example.com/icon.png",
            websiteUrl = "https://example.com",
            repositoryUrl = "https://example.com/repo",
            fdroidUrl = "https://f-droid.org/packages/com.example.solution",
            upvotes = 7,
            downvotes = 2
        )

        val alternative = solution.toAlternative()

        assertEquals("s1", alternative.id)
        assertEquals("com.example.solution", alternative.packageName)
        assertEquals("Solution", alternative.name)
        assertEquals("GPL-3.0", alternative.license)
        assertEquals("https://example.com", alternative.website)
        assertEquals("https://example.com/repo", alternative.repoUrl)
        assertEquals(7, alternative.matchUpvotes)
        assertEquals(2, alternative.matchDownvotes)
        assertEquals(5, alternative.matchScore)
    }

    @Test
    fun `catalog target maps to proprietary alternative and first category`() {
        val target = CatalogTargetEntity(
            id = "t1",
            packageName = "com.example.target",
            name = "Target",
            categories = "[\"Social\",\"Communication\"]"
        )

        val alternative = target.toAlternative()

        assertEquals("t1", alternative.id)
        assertEquals("com.example.target", alternative.packageName)
        assertEquals("Proprietary", alternative.license)
        assertEquals("Social", alternative.category)
    }

    @Test
    fun `catalog target with empty categories falls back to Other`() {
        val target = CatalogTargetEntity(
            id = "t1",
            packageName = "com.example.target",
            name = "Target",
            categories = "[]"
        )

        assertEquals("Other", target.toAlternative().category)
    }
}
