package com.jksalcedo.librefind.ui.signingkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.repository.AppRepository
import com.jksalcedo.librefind.domain.usecase.SubmitSigningKeyVoteUseCase
import com.jksalcedo.librefind.util.SignerUtils
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SigningKeyVoteState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val sha256Digest: String = "",
    val voteCount: Int = 0,
    val hasUserSubmitted: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

class SigningKeyVoteViewModel(
    private val submitSigningKeyVoteUseCase: SubmitSigningKeyVoteUseCase,
    private val appRepository: AppRepository,
    private val packageManager: PackageManager
) : ViewModel() {

    private val _state = MutableStateFlow(SigningKeyVoteState())
    val state: StateFlow<SigningKeyVoteState> = _state.asStateFlow()

    fun loadDigest(packageName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val digests = SignerUtils.getSignerDigests(packageManager, packageName)
            val digest = digests.firstOrNull() ?: ""
            val alreadySubmitted = if (digest.isNotEmpty()) {
                try { appRepository.hasUserSubmittedKeyVote(packageName) } catch (_: Exception) { false }
            } else false
            val count = if (digest.isNotEmpty()) {
                try { appRepository.getSigningKeyVoteCount(packageName, digest) } catch (_: Exception) { 0 }
            } else 0
            _state.update {
                it.copy(
                    isLoading = false,
                    sha256Digest = digest,
                    voteCount = count,
                    hasUserSubmitted = alreadySubmitted,
                    error = if (digest.isEmpty()) "Could not extract signing certificate for this app." else null
                )
            }
        }
    }

    fun loadExistingVote(packageName: String, sha256Digest: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, sha256Digest = sha256Digest) }
            val alreadySubmitted = try { appRepository.hasUserSubmittedKeyVote(packageName) } catch (_: Exception) { false }
            val count = try { appRepository.getSigningKeyVoteCount(packageName, sha256Digest) } catch (_: Exception) { 0 }
            _state.update {
                it.copy(isLoading = false, voteCount = count, hasUserSubmitted = alreadySubmitted)
            }
        }
    }

    fun submit(packageName: String, appLabel: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            submitSigningKeyVoteUseCase(packageName, appLabel)
                .onSuccess { digest ->
                    val newCount = try { appRepository.getSigningKeyVoteCount(packageName, digest) } catch (_: Exception) { 1 }
                    _state.update {
                        it.copy(isSubmitting = false, success = true, hasUserSubmitted = true, voteCount = newCount)
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSubmitting = false, error = e.message ?: "Submission failed") }
                }
        }
    }

    fun endorse(packageName: String) {
        val digest = _state.value.sha256Digest
        if (digest.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            appRepository.endorseSigningKeyVote(packageName, digest)
                .onSuccess {
                    val newCount = try { appRepository.getSigningKeyVoteCount(packageName, digest) } catch (_: Exception) { _state.value.voteCount + 1 }
                    _state.update { it.copy(isSubmitting = false, success = true, hasUserSubmitted = true, voteCount = newCount) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSubmitting = false, error = e.message ?: "Endorsement failed") }
                }
        }
    }
}
