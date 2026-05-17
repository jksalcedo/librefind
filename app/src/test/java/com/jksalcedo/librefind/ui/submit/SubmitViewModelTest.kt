package com.jksalcedo.librefind.ui.submit

import com.jksalcedo.librefind.data.local.InventorySource
import com.jksalcedo.librefind.domain.model.DuplicateStatus
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.repository.AuthRepository
import com.jksalcedo.librefind.domain.repository.CacheRepository
import com.jksalcedo.librefind.domain.usecase.SubmitProposalUseCase
import com.jksalcedo.librefind.domain.usecase.UpdateSubmissionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubmitViewModelTest {

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val appRepository = mockk<AppRepository>(relaxed = true)
    private val submitProposalUseCase = mockk<SubmitProposalUseCase>(relaxed = true)
    private val updateSubmissionUseCase = mockk<UpdateSubmissionUseCase>(relaxed = true)
    private val cacheRepository = mockk<CacheRepository>(relaxed = true)
    private val inventorySource = mockk<InventorySource>(relaxed = true)

    private lateinit var viewModel: SubmitViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        viewModel = SubmitViewModel(
            authRepository,
            appRepository,
            submitProposalUseCase,
            updateSubmissionUseCase,
            cacheRepository,
            inventorySource
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun checkDuplicate_setsWarningForApprovedSolution() = runTest(testDispatcher) {
        val packageName = "com.test.app"
        coEvery { appRepository.checkDuplicateApp(packageName) } returns DuplicateStatus.APPROVED_SOLUTION

        viewModel.checkDuplicate(packageName)
        
        // Wait for debounce delay
        advanceTimeBy(600)
        
        assertEquals(
            "This app is already an approved FOSS app. Use 'Link App' to associate it with a proprietary app.",
            viewModel.uiState.value.duplicateWarning
        )
    }
}
