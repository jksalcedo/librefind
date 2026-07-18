package com.jksalcedo.librefind.domain.usecase

import android.content.pm.PackageManager
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.utils.SignerUtils

class SubmitSigningKeyVoteUseCase(
    private val appRepository: AppRepository,
    private val packageManager: PackageManager
) {
    suspend operator fun invoke(packageName: String, appLabel: String): Result<String> {
        val digests = SignerUtils.getSignerDigests(packageManager, packageName)
        if (digests.isEmpty()) {
            return Result.failure(IllegalStateException("No signing certificates found for $packageName"))
        }
        val digest = digests.first()
        return appRepository.submitSigningKeyVote(packageName, appLabel, digest)
            .map { digest }
    }
}
