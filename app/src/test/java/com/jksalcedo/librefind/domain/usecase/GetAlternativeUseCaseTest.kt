package com.jksalcedo.librefind.domain.usecase

import com.jksalcedo.librefind.domain.model.Alternative
import com.jksalcedo.librefind.domain.repository.AppRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAlternativeUseCaseTest {

    private val repository: AppRepository = mockk()
    private val useCase = GetAlternativeUseCase(repository)

    @Test
    fun `invoke returns alternatives sorted by matchScore then by ratingAvg`() = runTest {
        // Arrange
        val alt1 = Alternative(id = "1", name = "App 1", packageName = "p1", license = "L", repoUrl = "R", fdroidId = "F", matchScore = 10, ratingAvg = 4.0f)
        val alt2 = Alternative(id = "2", name = "App 2", packageName = "p2", license = "L", repoUrl = "R", fdroidId = "F", matchScore = 20, ratingAvg = 3.0f)
        val alt3 = Alternative(id = "3", name = "App 3", packageName = "p3", license = "L", repoUrl = "R", fdroidId = "F", matchScore = 10, ratingAvg = 4.5f)
        
        val alternatives = listOf(alt1, alt2, alt3)
        coEvery { repository.getAlternatives(any()) } returns alternatives

        // Act
        val result = useCase("test.pkg")

        // Assert
        // Expected order: alt2 (score 20), alt3 (score 10, rating 4.5), alt1 (score 10, rating 4.0)
        assertEquals(3, result.size)
        assertEquals("2", result[0].id)
        assertEquals("3", result[1].id)
        assertEquals("1", result[2].id)
    }

    @Test
    fun `invoke returns empty list when repository returns empty list`() = runTest {
        // Arrange
        coEvery { repository.getAlternatives(any()) } returns emptyList()

        // Act
        val result = useCase("test.pkg")

        // Assert
        assertEquals(0, result.size)
    }
}
