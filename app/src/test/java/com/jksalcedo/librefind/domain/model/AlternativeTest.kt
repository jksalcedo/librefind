package com.jksalcedo.librefind.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AlternativeTest {

    @Test
    fun `displayRating returns formatted rating when ratingCount is greater than 0`() {
        Locale.setDefault(Locale.US)
        val alternative = Alternative(
            id = "1", name = "Test", packageName = "p", license = "L", repoUrl = "R", fdroidId = "F",
            ratingAvg = 4.567f,
            ratingCount = 10
        )
        assertEquals("4.6", alternative.displayRating)
    }

    @Test
    fun `displayRating returns dash when ratingCount is 0`() {
        val alternative = Alternative(
            id = "1", name = "Test", packageName = "p", license = "L", repoUrl = "R", fdroidId = "F",
            ratingAvg = 0f,
            ratingCount = 0
        )
        assertEquals("—", alternative.displayRating)
    }

    @Test
    fun `displayUsabilityRating returns formatted rating when usabilityRating is greater than 0`() {
        Locale.setDefault(Locale.US)
        val alternative = Alternative(
            id = "1", name = "Test", packageName = "p", license = "L", repoUrl = "R", fdroidId = "F",
            usabilityRating = 3.21f
        )
        assertEquals("3.2", alternative.displayUsabilityRating)
    }

    @Test
    fun `displayPrivacyRating returns dash when privacyRating is 0`() {
        val alternative = Alternative(
            id = "1", name = "Test", packageName = "p", license = "L", repoUrl = "R", fdroidId = "F",
            privacyRating = 0f
        )
        assertEquals("—", alternative.displayPrivacyRating)
    }
}
