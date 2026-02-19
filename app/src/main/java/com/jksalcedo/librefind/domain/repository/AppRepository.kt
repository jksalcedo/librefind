package com.jksalcedo.librefind.domain.repository

import com.jksalcedo.librefind.domain.model.Alternative
import com.jksalcedo.librefind.domain.model.Report
import com.jksalcedo.librefind.domain.model.Submission
import com.jksalcedo.librefind.domain.model.SubmissionType

interface AppRepository {

    suspend fun areProprietary(packageNames: List<String>): Map<String, Boolean>
    suspend fun isProprietary(packageName: String): Boolean
    suspend fun isSolution(packageName: String): Boolean
    suspend fun getAlternatives(packageName: String): List<Alternative>
    suspend fun getAlternative(packageName: String): Alternative?
    suspend fun getProprietaryTargets(): List<String>


    /**
     * Bulk fetch: returns all targets with their alternatives count in a single query.
     */
    suspend fun getProprietaryTargetsWithAlternativesCount(): Map<String, Int>

    /**
     * Bulk fetch: returns all solution package names in a single query.
     * Used during cache refresh to avoid per-app isSolution() calls.
     */
    suspend fun getAllSolutionPackageNames(): List<String>

    /**
     * Bulk check: returns a set of package names that are known solutions.
     * Used during classification to avoid per-app network calls.
     */
    suspend fun areSolutions(packageNames: List<String>): Set<String>

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
        alternatives: List<String> = emptyList(),
        submissionType: SubmissionType,
        category: String = ""
    ): Result<Unit>

    suspend fun submitLinkedAlternatives(
        proprietaryPackage: String,
        alternatives: List<String>,
        submitterId: String
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
        alternatives: List<String> = emptyList(),
        category: String = ""
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
    suspend fun checkDuplicateApp(packageName: String): Boolean

    suspend fun getUserVote(packageName: String, userId: String): Map<String, Int?>

    suspend fun searchSolutions(query: String, limit: Int = 20): List<Alternative>

    suspend fun getAlternativesCount(packageName: String): Int

    suspend fun getPendingSubmissionPackages(): Set<String>

    suspend fun submitScanStats(
        deviceId: String,
        fossCount: Int,
        proprietaryCount: Int,
        unknownCount: Int,
        appVersion: String? = null
    ): Result<Unit>

    suspend fun submitAppReport(
        packageName: String,
        issueType: String,
        description: String
    ): Result<Unit>
}