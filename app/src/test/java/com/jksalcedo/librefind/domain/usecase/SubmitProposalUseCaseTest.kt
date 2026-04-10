package com.jksalcedo.librefind.domain.usecase

import com.jksalcedo.librefind.domain.model.SubmissionType
import com.jksalcedo.librefind.domain.repository.AppRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitProposalUseCaseTest {

    private val repository: AppRepository = mockk()
    private val useCase = SubmitProposalUseCase(repository)

    @Test
    fun `invoke returns failure when alternativeId is blank`() = runTest {
        val result = useCase(
            proprietaryPackage = "p",
            alternativeId = "",
            appName = "A",
            description = "D",
            repoUrl = "R",
            fdroidId = "F",
            license = "L",
            userId = "U",
            submissionType = SubmissionType.NEW_ALTERNATIVE
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invoke returns failure when userId is blank`() = runTest {
        val result = useCase(
            proprietaryPackage = "p",
            alternativeId = "A",
            appName = "A",
            description = "D",
            repoUrl = "R",
            fdroidId = "F",
            license = "L",
            userId = "",
            submissionType = SubmissionType.NEW_ALTERNATIVE
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invoke returns success when repository succeeds`() = runTest {
        coEvery {
            repository.submitAlternative(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Result.success(Unit)

        val result = useCase(
            proprietaryPackage = "p",
            alternativeId = "A",
            appName = "A",
            description = "D",
            repoUrl = "R",
            fdroidId = "F",
            license = "L",
            userId = "U",
            submissionType = SubmissionType.NEW_ALTERNATIVE
        )
        assertTrue(result.isSuccess)
    }
}
