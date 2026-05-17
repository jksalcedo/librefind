package com.jksalcedo.librefind.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jksalcedo.librefind.domain.model.Submission
import com.jksalcedo.librefind.domain.model.SubmissionType
import com.jksalcedo.librefind.domain.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunitySubmissionsState(
    val submissions: List<Submission> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

class CommunitySubmissionsViewModel(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunitySubmissionsState())
    val uiState: StateFlow<CommunitySubmissionsState> = _uiState.asStateFlow()

    init {
        loadSubmissions()
    }

    fun loadSubmissions(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else if (_uiState.value.submissions.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            try {
                val submissions = appRepository.getAllPendingSubmissions(forceRefresh)
                val voteCounts = appRepository.getSubmissionVoteCounts(submissions.map { it.id })
                val enriched = submissions.map { s ->
                    val agg = voteCounts[s.id]
                    s.copy(
                        upvotes = agg?.upvotes ?: s.upvotes,
                        downvotes = agg?.downvotes ?: s.downvotes,
                        userVote = agg?.userVote ?: s.userVote
                    )
                }
                _uiState.update { it.copy(submissions = enriched, isLoading = false, isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = e.message ?: "Failed to load submissions")
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun approveSubmission(submission: Submission) {
        viewModelScope.launch {
            appRepository.approveSubmission(submission.id, submission.type)
                .onSuccess { loadSubmissions() }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to approve submission") }
                }
        }
    }

    fun rejectSubmission(submission: Submission, reason: String) {
        viewModelScope.launch {
            appRepository.rejectSubmission(submission.id, submission.type, reason)
                .onSuccess { loadSubmissions() }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to reject submission") }
                }
        }
    }

    fun castVote(
        submission: Submission,
        vote: Int,
        reason: String? = null,
        reasonDetail: String? = null
    ) {
        val newVote = if (submission.userVote == vote) 0 else vote

        _uiState.update { state ->
            state.copy(
                submissions = state.submissions.map { s ->
                    if (s.id != submission.id) return@map s
                    val wasUpvoted = s.userVote == 1
                    val wasDownvoted = s.userVote == -1
                    s.copy(
                        upvotes = when {
                            newVote == 1 -> s.upvotes + 1
                            wasUpvoted -> s.upvotes - 1
                            else -> s.upvotes
                        },
                        downvotes = when {
                            newVote == -1 -> s.downvotes + 1
                            wasDownvoted -> s.downvotes - 1
                            else -> s.downvotes
                        },
                        userVote = if (newVote == 0) null else newVote
                    )
                }
            )
        }

        val table = if (submission.type == SubmissionType.LINKING)
            "user_linking_submissions" else "user_submissions"

        viewModelScope.launch {
            appRepository.castSubmissionVote(
                submissionId = submission.id,
                submissionTable = table,
                vote = newVote,
                reason = if (newVote == -1) reason else null,
                reasonDetail = if (newVote == -1) reasonDetail else null
            ).onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to cast vote") }
                loadSubmissions()
            }
        }
    }
}
