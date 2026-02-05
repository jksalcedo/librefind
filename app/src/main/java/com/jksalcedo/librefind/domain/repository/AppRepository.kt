package com.jksalcedo.librefind.domain.repository

import com.jksalcedo.librefind.domain.model.Alternative
import com.jksalcedo.librefind.domain.model.Report
import com.jksalcedo.librefind.domain.model.Submission

interface AppRepository {

    suspend fun areProprietary(packageNames: List<String>): Map<String, Boolean>
    suspend fun isProprietary(packageName: String): Boolean
    suspend fun isSolution(packageName: String): Boolean
    suspend fun getAlternatives(packageName: String): List<Alternative>
    suspend fun getAlternative(packageName: String): Alternative?
    suspend fun getProprietaryTargets(): List<String>

    suspend fun submitReport(
        title: String,
        description: String,
        type: String,
        priority: String,
        userId: String
    ): Result<Unit>

    suspend fun getMyReports(userId: String): List<Report>

    suspend fun submitAlternative(
        proprietaryPackage: String,
        alternativePackage: String,
        appName: String,
        description: String,
        repoUrl: String,
        fdroidId: String,
        license: String,
        userId: String,
        alternatives: List<String> = emptyList()
    ): Result<Unit>

    suspend fun updateSubmission(
        id: String,
        proprietaryPackage: String,
        alternativePackage: String,
        appName: String,
        description: String,
        repoUrl: String,
        fdroidId: String,
        license: String,
        alternatives: List<String> = emptyList()
    ): Result<Unit>

    suspend fun castVote(
        packageName: String,
        voteType: String, // 'privacy' or 'usability'
        value: Int // 1-5 star rating
    ): Result<Unit>

    suspend fun getMySubmissions(userId: String): List<Submission>

    suspend fun submitFeedback(
        packageName: String,
        type: String, // 'PRO' or 'CON'
        text: String
    ): Result<Unit>

    // Kept from KnowledgeGraphRepo if needed, or can be refactored
    suspend fun checkDuplicateApp(name: String, packageName: String): Boolean

    suspend fun getUserVote(packageName: String, userId: String): Map<String, Int?>

    suspend fun searchSolutions(query: String, limit: Int = 20): List<Alternative>

    suspend fun getAlternativesCount(packageName: String): Int

    suspend fun getPendingSubmissionPackages(): Set<String>
}
