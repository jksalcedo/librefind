package com.jksalcedo.librefind.domain.usecase

import com.jksalcedo.librefind.domain.model.Alternative
import com.jksalcedo.librefind.domain.repository.AppRepository

class GetAlternativeUseCase(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(packageName: String): List<Alternative> {
        return appRepository.getAlternatives(packageName)
            .sortedWith(compareByDescending<Alternative> { it.matchScore }.thenByDescending { it.ratingAvg })
    }
}

