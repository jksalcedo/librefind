package com.jksalcedo.librefind.data.repository

import android.util.Log
import com.jksalcedo.librefind.data.remote.model.AppScanStatsDto
import com.jksalcedo.librefind.data.remote.model.ProfileDto
import com.jksalcedo.librefind.data.remote.model.SolutionDto
import com.jksalcedo.librefind.data.remote.model.UserLinkingSubmissionsDto
import com.jksalcedo.librefind.data.remote.model.UserReportDto
import com.jksalcedo.librefind.data.remote.model.UserSubmissionDto
import com.jksalcedo.librefind.data.remote.model.UserVoteDto
import com.jksalcedo.librefind.domain.model.Alternative
import com.jksalcedo.librefind.domain.model.Report
import com.jksalcedo.librefind.domain.model.ReportPriority
import com.jksalcedo.librefind.domain.model.ReportStatus
import com.jksalcedo.librefind.domain.model.ReportType
import com.jksalcedo.librefind.domain.model.Submission
import com.jksalcedo.librefind.domain.model.SubmissionStatus
import com.jksalcedo.librefind.domain.model.SubmissionType
import com.jksalcedo.librefind.domain.model.SubmittedApp
import com.jksalcedo.librefind.domain.repository.AppRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

class SupabaseAppRepository(
    private val supabase: SupabaseClient
) : AppRepository {

    override suspend fun areProprietary(packageNames: List<String>): Map<String, Boolean> {
        if (packageNames.isEmpty()) return emptyMap()

        return try {
            val uniquePackages = packageNames.distinct()
            val foundPackages = mutableSetOf<String>()
            val chunkSize = 200

            uniquePackages.chunked(chunkSize).forEach { chunk ->
                val found = supabase.postgrest.from("targets")
                    .select(columns = Columns.list("package_name")) {
                        filter { isIn("package_name", chunk) }
                    }
                    .decodeList<PackageNameDto>()
                    .map { it.packageName }

                foundPackages.addAll(found)
            }

            packageNames.associateWith { foundPackages.contains(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    override suspend fun isProprietary(packageName: String): Boolean {
        return try {
            val count = supabase.postgrest.from("targets")
                .select(columns = Columns.list("package_name")) {
                    count(Count.EXACT)
                    filter { eq("package_name", packageName) }
                    limit(1)
                }.countOrNull() ?: 0

            count > 0
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun isSolution(packageName: String): Boolean {
        return try {
            val count = supabase.postgrest.from("solutions")
                .select(columns = Columns.list("package_name")) {
                    count(Count.EXACT)
                    filter { eq("package_name", packageName) }
                    limit(1)
                }.countOrNull() ?: 0

            count > 0
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getAlternatives(packageName: String): List<Alternative> {
        return try {
            val target = supabase.postgrest.from("targets")
                .select(columns = Columns.list("alternatives")) {
                    filter {
                        eq("package_name", packageName)
                    }
                }.decodeSingleOrNull<TargetAlternativesDto>() ?: return emptyList()

            if (target.alternatives.isNullOrEmpty()) return emptyList()

            val solutions = supabase.postgrest.from("solutions")
                .select(
                    columns = Columns.list(
                        "package_name", "name", "license", "repo_url", "fdroid_id",
                        "icon_url", "description", "features", "pros", "cons",
                        "rating_usability", "rating_privacy", "rating_features", "vote_count"
                    )
                ) {
                    filter {
                        isIn("package_name", target.alternatives)
                    }
                }.decodeList<SolutionDto>()

            solutions.map { dto ->
                val usabilityRating = dto.ratingUsability ?: 0f
                val privacyRating = dto.ratingPrivacy ?: 0f
                val featuresRating = dto.ratingFeatures ?: 0f
                val rawRatingCount = dto.voteCount ?: 0

                val ratingAvg = if (rawRatingCount > 0) {
                    val sum = usabilityRating + privacyRating + featuresRating
                    val nonZeroCount =
                        listOf(usabilityRating, privacyRating, featuresRating).count { it > 0 }
                    if (nonZeroCount > 0) sum / nonZeroCount else 0f
                } else 0f

                val ratingCount = if (ratingAvg == 0f) 0 else rawRatingCount

                Alternative(
                    id = dto.packageName,
                    name = dto.name,
                    packageName = dto.packageName,
                    license = dto.license,
                    repoUrl = dto.repoUrl ?: "",
                    fdroidId = dto.fdroidId ?: "",
                    iconUrl = dto.iconUrl,
                    ratingAvg = ratingAvg,
                    ratingCount = ratingCount,
                    usabilityRating = usabilityRating,
                    privacyRating = privacyRating,
                    featuresRating = featuresRating,
                    description = dto.description,
                    features = dto.features.orEmpty(),
                    pros = dto.pros.orEmpty(),
                    cons = dto.cons.orEmpty()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getAlternative(packageName: String): Alternative? {
        return try {
            val dto = supabase.postgrest.from("solutions")
                .select(
                    columns = Columns.list(
                        "package_name", "name", "license", "repo_url", "fdroid_id",
                        "icon_url", "description", "features", "pros", "cons",
                        "rating_usability", "rating_privacy", "rating_features", "vote_count"
                    )
                ) {
                    filter {
                        eq("package_name", packageName)
                    }
                }.decodeSingleOrNull<SolutionDto>() ?: return null

            val usabilityRating = dto.ratingUsability ?: 0f
            val privacyRating = dto.ratingPrivacy ?: 0f
            val featuresRating = dto.ratingFeatures ?: 0f
            val rawRatingCount = dto.voteCount ?: 0

            val ratingAvg = if (rawRatingCount > 0) {
                val sum = usabilityRating + privacyRating + featuresRating
                val nonZeroCount =
                    listOf(usabilityRating, privacyRating, featuresRating).count { it > 0 }
                if (nonZeroCount > 0) sum / nonZeroCount else 0f
            } else 0f

            val ratingCount = if (ratingAvg == 0f) 0 else rawRatingCount

            Alternative(
                id = dto.packageName,
                name = dto.name,
                packageName = dto.packageName,
                license = dto.license,
                repoUrl = dto.repoUrl ?: "",
                fdroidId = dto.fdroidId ?: "",
                iconUrl = dto.iconUrl,
                ratingAvg = ratingAvg,
                ratingCount = ratingCount,
                usabilityRating = usabilityRating,
                privacyRating = privacyRating,
                featuresRating = featuresRating,
                description = dto.description,
                features = dto.features.orEmpty(),
                pros = dto.pros.orEmpty(),
                cons = dto.cons.orEmpty()
            )
        } catch (_: Exception) {
            null
        }
    }

    @Serializable
    private data class PackageNameDto(@SerialName("package_name") val packageName: String)

    @Serializable
    private data class TargetAlternativesDto(val alternatives: List<String>? = null)

    override suspend fun getProprietaryTargets(): List<String> {
        return try {
            supabase.postgrest.from("targets")
                .select(columns = Columns.list("package_name"))
                .decodeList<PackageNameDto>()
                .map { it.packageName }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getProprietaryTargetsWithAlternativesCount(): Map<String, Int> {
        return try {
            val targets = supabase.postgrest.from("targets")
                .select(columns = Columns.list("package_name", "alternatives"))
                .decodeList<TargetWithAlternativesDto>()

            targets.associate { it.packageName to (it.alternatives?.size ?: 0) }
        } catch (e: Exception) {
            Log.e("SupabaseAppRepo", "Failed to fetch targets with counts", e)
            emptyMap()
        }
    }

    override suspend fun getAllSolutionPackageNames(): List<String> {
        return try {
            supabase.postgrest.from("solutions")
                .select(columns = Columns.list("package_name"))
                .decodeList<PackageNameDto>()
                .map { it.packageName }
        } catch (e: Exception) {
            Log.e("SupabaseAppRepo", "Failed to fetch solution package names", e)
            emptyList()
        }
    }

    override suspend fun areSolutions(packageNames: List<String>): Set<String> {
        if (packageNames.isEmpty()) return emptySet()

        return try {
            val uniquePackages = packageNames.distinct()
            val foundPackages = mutableSetOf<String>()
            val chunkSize = 200

            uniquePackages.chunked(chunkSize).forEach { chunk ->
                val found = supabase.postgrest.from("solutions")
                    .select(columns = Columns.list("package_name")) {
                        filter { isIn("package_name", chunk) }
                    }
                    .decodeList<PackageNameDto>()
                    .map { it.packageName }

                foundPackages.addAll(found)
            }

            foundPackages
        } catch (e: Exception) {
            Log.e("SupabaseAppRepo", "Failed bulk solution lookup", e)
            emptySet()
        }
    }

    override suspend fun submitAlternative(
        proprietaryPackage: String,
        alternativePackage: String,
        appName: String,
        description: String,
        repoUrl: String,
        fdroidId: String,
        license: String,
        userId: String,
        alternatives: List<String>,
        submissionType: SubmissionType
    ): Result<Unit> = runCatching {
        try {
            val submission = UserSubmissionDto(
                appName = appName,
                appPackage = alternativePackage,
                description = description,
                proprietaryPackage = proprietaryPackage.ifBlank { null },
                repoUrl = repoUrl.ifBlank { null },
                fdroidId = fdroidId.ifBlank { null },
                license = license.ifBlank { null },
                alternatives = alternatives.ifEmpty { null },
                submissionType = submissionType.name,
                submitterId = userId
            )
            supabase.postgrest.from("user_submissions").insert(submission)
            Log.d("SupabaseAppRepo", "Submission successful")
        } catch (e: Exception) {
            Log.e("SupabaseAppRepo", "Submission failed", e)
            throw e
        }
    }

    override suspend fun submitLinkedAlternatives(
        proprietaryPackage: String,
        alternatives: List<String>,
        submitterId: String
    ): Result<Unit> = runCatching {
        try {
            val submission = UserLinkingSubmissionsDto(
                proprietaryPackage = proprietaryPackage,
                alternatives = alternatives,
                submitterId = submitterId,
                status = "PENDING",
                rejectionReason = null
            )
            supabase.postgrest.from("user_linking_submissions").insert(submission)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun updateSubmission(
        id: String,
        proprietaryPackage: String,
        alternativePackage: String,
        appName: String,
        description: String,
        repoUrl: String,
        fdroidId: String,
        license: String,
        alternatives: List<String>
    ): Result<Unit> = runCatching {
        try {
            val updateData = UserSubmissionDto(
                appName = appName,
                appPackage = alternativePackage,
                description = description,
                proprietaryPackage = proprietaryPackage.ifBlank { null },
                repoUrl = repoUrl.ifBlank { null },
                fdroidId = fdroidId.ifBlank { null },
                license = license.ifBlank { null },
                alternatives = alternatives.ifEmpty { null },
                submitterId = supabase.auth.currentUserOrNull()?.id
                    ?: throw IllegalStateException("Not logged in"),
                status = "PENDING", // Reset status to PENDING on update
                rejectionReason = null // Clear rejection reason
            )

            val result = supabase.postgrest.from("user_submissions").update(updateData) {
                filter {
                    eq("id", id)
                }
                select() // Return updated rows to check count
            }

            val updated = result.decodeList<UserSubmissionDto>()

            if (updated.isEmpty()) {
                // Fallback: Insert as new submission
                supabase.postgrest.from("user_submissions").insert(updateData)
                Log.d("SupabaseAppRepo", "Fallback insertion successful (0 rows updated)")
            } else {
                // Check if status was actually updated to PENDING
                val newStatus = updated.first().status
                if (newStatus != "PENDING") {
                    Log.w(
                        "SupabaseAppRepo",
                        "Update succeeded but status is still $newStatus. RLS/Trigger prevented status change. Falling back to INSERT."
                    )
                    // Fallback: Insert as new submission
                    supabase.postgrest.from("user_submissions").insert(updateData)
                    Log.d("SupabaseAppRepo", "Fallback insertion successful (Status check failed)")
                } else {
                    Log.d("SupabaseAppRepo", "Update successful and status is PENDING")
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseAppRepo", "Update failed", e)
            throw e
        }
    }

    override suspend fun castVote(
        packageName: String,
        voteType: String,
        value: Int
    ): Result<Unit> = runCatching {
        val userId =
            supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not logged in")

        val vote = UserVoteDto(
            userId = userId,
            packageName = packageName,
            voteType = voteType,
            value = value
        )

        Log.d("SupabaseAppRepo", "castVote: upserting vote")
        try {
            supabase.postgrest.from("user_votes").upsert(vote) {
                onConflict = "user_id,package_name,vote_type"
                defaultToNull = false
            }
            Log.d("SupabaseAppRepo", "castVote: upsert complete")
        } catch (e: Exception) {
            Log.e("SupabaseAppRepo", "castVote: upsert failed", e)
            throw e
        }

        Log.d("SupabaseAppRepo", "castVote: calling RPC")
        try {
            supabase.postgrest.rpc(
                "vote_for_app",
                VoteForAppParams(packageName, voteType, value)
            )
            Log.d("SupabaseAppRepo", "castVote: RPC complete")
        } catch (e: Exception) {
            Log.e("SupabaseAppRepo", "castVote: RPC failed", e)
            throw e
        }
    }

    @Serializable
    private data class VoteForAppParams(
        @SerialName("package_name") val packageName: String,
        @SerialName("vote_type") val voteType: String,
        val value: Int
    )

    override suspend fun submitFeedback(
        packageName: String,
        type: String,
        text: String
    ): Result<Unit> = runCatching {
        val userId =
            supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not logged in")

        val report = UserReportDto(
            title = "Feedback: $packageName ($type)",
            description = text,
            reportType = "FEEDBACK",
            priority = "LOW",
            submitterId = userId
        )
        supabase.postgrest.from("user_reports").insert(report)
    }

    override suspend fun getMySubmissions(userId: String): List<Submission> {
        return try {
            // 1. Fetch standard submissions
            val standardDtos =
                supabase.postgrest.from("user_submissions")
                    .select(
                        columns = Columns.list(
                            "id",
                            "app_name",
                            "app_package",
                            "description",
                            "proprietary_package",
                            "repo_url",
                            "fdroid_id",
                            "license",
                            "submission_type",
                            "status",
                            "submitter_id",
                            "rejection_reason",
                            "created_at",
                            "profiles(id, username)"
                        )
                    ) {
                        filter { eq("submitter_id", userId) }
                    }.decodeList<UserSubmissionWithProfileDto>()

            // 2. Fetch linking submissions
            val linkingDtos =
                supabase.postgrest.from("user_linking_submissions")
                    .select(
                        columns = Columns.list(
                            "id",
                            "proprietary_package",
                            "alternatives",
                            "status",
                            "submitter_id",
                            "rejection_reason",
                            "created_at",
                            "profiles(id, username)"
                        )
                    ) {
                        filter { eq("submitter_id", userId) }
                    }.decodeList<UserLinkingSubmissionWithProfileDto>()

            val standardList = standardDtos.map { dto ->
                Submission(
                    id = dto.id ?: "",
                    type = dto.submissionType?.let {
                        try {
                            SubmissionType.valueOf(it)
                        } catch (_: Exception) {
                            null
                        }
                    } ?: if (!dto.license.isNullOrBlank() || !dto.repoUrl.isNullOrBlank())
                        SubmissionType.NEW_ALTERNATIVE
                    else
                        SubmissionType.NEW_PROPRIETARY,
                    proprietaryPackages = dto.proprietaryPackage ?: "",
                    submittedApp = SubmittedApp(
                        name = dto.appName,
                        packageName = dto.appPackage,
                        description = dto.description,
                        repoUrl = dto.repoUrl ?: "",
                        fdroidId = dto.fdroidId ?: "",
                        license = dto.license ?: ""
                    ),
                    submitterUid = dto.submitterId,
                    submitterUsername = dto.profile?.username ?: "Unknown",
                    // Parse created_at timestamp or use current time if missing
                    submittedAt = dto.createdAt?.let { parseTimestamp(it) }
                        ?: System.currentTimeMillis(),
                    status = try {
                        SubmissionStatus.valueOf(dto.status)
                    } catch (_: Exception) {
                        SubmissionStatus.PENDING
                    },
                    rejectionReason = dto.rejectionReason
                )
            }

            val linkingList = linkingDtos.map { dto ->
                Submission(
                    id = dto.id ?: "",
                    type = SubmissionType.LINKING,
                    proprietaryPackages = dto.proprietaryPackage,
                    submittedApp = SubmittedApp(
                        // format the name in the UI based on the type.
                        name = "Link ${dto.alternatives.size} Alternatives",
                        packageName = dto.proprietaryPackage,
                        description = "Linking request for ${dto.proprietaryPackage}"
                    ),
                    submitterUid = dto.submitterId,
                    submitterUsername = dto.profile?.username ?: "Unknown",
                    submittedAt = dto.createdAt?.let { parseTimestamp(it) }
                        ?: System.currentTimeMillis(),
                    status = try {
                        SubmissionStatus.valueOf(dto.status)
                    } catch (_: Exception) {
                        SubmissionStatus.PENDING
                    },
                    rejectionReason = dto.rejectionReason,
                    linkedAlternatives = dto.alternatives
                )
            }

            (standardList + linkingList).sortedByDescending { it.submittedAt }

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseTimestamp(isoString: String): Long {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Instant.parse(isoString).toEpochMilli()
            } else {
                // Regex to truncate fractional seconds to 3 digits
                val truncated = isoString.replace(Regex("(\\.\\d{3})\\d+"), "$1")
                val format =
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", java.util.Locale.US)
                format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                format.parse(truncated)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            System.currentTimeMillis()
        }
    }

    override suspend fun getUserVote(packageName: String, userId: String): Map<String, Int?> {
        return try {
            val votes = supabase.postgrest.from("user_votes")
                .select(columns = Columns.list("vote_type", "value")) {
                    filter {
                        eq("user_id", userId)
                        eq("package_name", packageName)
                    }
                }.decodeList<UserVoteDto>()

            mapOf(
                "usability" to votes.find { it.voteType == "usability" }?.value,
                "privacy" to votes.find { it.voteType == "privacy" }?.value,
                "features" to votes.find { it.voteType == "features" }?.value
            )
        } catch (e: Exception) {
            e.printStackTrace()
            mapOf(
                "usability" to null,
                "privacy" to null,
                "features" to null
            )
        }
    }

    override suspend fun checkDuplicateApp(packageName: String): Boolean {
        if (packageName.isBlank()) return false

        return try {
            // Check approved FOSS alternatives (solutions table)
            val solutionsCount = supabase.postgrest.from("solutions")
                .select(columns = Columns.list("package_name")) {
                    count(Count.EXACT)
                    filter { eq("package_name", packageName) }
                    limit(1)
                }.countOrNull() ?: 0

            if (solutionsCount > 0) return true

            // Check approved proprietary targets (targets table)
            val targetsCount = supabase.postgrest.from("targets")
                .select(columns = Columns.list("package_name")) {
                    count(Count.EXACT)
                    filter { eq("package_name", packageName) }
                    limit(1)
                }.countOrNull() ?: 0

            if (targetsCount > 0) return true

            // Check pending submissions (user_submissions table)
            val pendingCount = supabase.postgrest.from("user_submissions")
                .select(columns = Columns.list("app_package")) {
                    count(Count.EXACT)
                    filter {
                        eq("app_package", packageName)
                        eq("status", "PENDING")
                    }
                    limit(1)
                }.countOrNull() ?: 0

            pendingCount > 0
        } catch (e: Exception) {
            Log.e("SupabaseAppRepo", "checkDuplicateApp failed for $packageName", e)
            false
        }
    }

    override suspend fun searchSolutions(query: String, limit: Int): List<Alternative> {
        if (query.isBlank()) return emptyList()

        return try {
            // Escape SQL LIKE wildcards in user input
            val sanitizedQuery = query
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")

            val solutions = supabase.postgrest.from("solutions")
                .select(
                    columns = Columns.list(
                        "package_name", "name", "license", "repo_url", "fdroid_id",
                        "icon_url", "description", "features", "pros", "cons",
                        "rating_usability", "rating_privacy", "rating_features", "vote_count"
                    )
                ) {
                    filter {
                        or {
                            ilike("name", "%$sanitizedQuery%")
                            ilike("package_name", "%$sanitizedQuery%")
                        }
                    }
                    limit(limit.toLong())
                }.decodeList<SolutionDto>()

            solutions.map { dto ->
                val usabilityRating = dto.ratingUsability ?: 0f
                val privacyRating = dto.ratingPrivacy ?: 0f
                val featuresRating = dto.ratingFeatures ?: 0f
                val rawRatingCount = dto.voteCount ?: 0

                val ratingAvg = if (rawRatingCount > 0) {
                    val sum = usabilityRating + privacyRating + featuresRating
                    val nonZeroCount =
                        listOf(usabilityRating, privacyRating, featuresRating).count { it > 0 }
                    if (nonZeroCount > 0) sum / nonZeroCount else 0f
                } else 0f

                val ratingCount = if (ratingAvg == 0f) 0 else rawRatingCount

                Alternative(
                    id = dto.packageName,
                    name = dto.name,
                    packageName = dto.packageName,
                    license = dto.license,
                    repoUrl = dto.repoUrl ?: "",
                    fdroidId = dto.fdroidId ?: "",
                    iconUrl = dto.iconUrl,
                    ratingAvg = ratingAvg,
                    ratingCount = ratingCount,
                    usabilityRating = usabilityRating,
                    privacyRating = privacyRating,
                    featuresRating = featuresRating,
                    description = dto.description,
                    features = dto.features.orEmpty(),
                    pros = dto.pros.orEmpty(),
                    cons = dto.cons.orEmpty()
                )
            }
        } catch (e: Exception) {
            Log.e("SupabaseAppRepo", "searchSolutions failed", e)
            emptyList()
        }
    }

    override suspend fun getAlternativesCount(packageName: String): Int {
        return try {
            val target = supabase.postgrest.from("targets")
                .select(columns = Columns.list("alternatives")) {
                    filter {
                        eq("package_name", packageName)
                    }
                }.decodeSingleOrNull<TargetAlternativesDto>() ?: return 0

            target.alternatives?.size ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    @Serializable
    private data class UserSubmissionWithProfileDto(
        val id: String? = null,
        @SerialName("app_name") val appName: String,
        @SerialName("app_package") val appPackage: String,
        val description: String = "",
        @SerialName("proprietary_package") val proprietaryPackage: String? = null,
        @SerialName("repo_url") val repoUrl: String? = null,
        @SerialName("fdroid_id") val fdroidId: String? = null,
        val license: String? = null,
        @SerialName("submission_type") val submissionType: String? = null,
        val status: String,
        @SerialName("submitter_id") val submitterId: String,
        @SerialName("rejection_reason") val rejectionReason: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("profiles") val profile: ProfileDto? = null
    )

    @Serializable
    private data class UserLinkingSubmissionWithProfileDto(
        val id: String? = null,
        @SerialName("proprietary_package") val proprietaryPackage: String,
        val alternatives: List<String> = emptyList(),
        val status: String,
        @SerialName("submitter_id") val submitterId: String,
        @SerialName("rejection_reason") val rejectionReason: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("profiles") val profile: ProfileDto? = null
    )

    override suspend fun submitReport(
        title: String,
        description: String,
        type: String,
        priority: String,
        userId: String
    ): Result<Unit> = runCatching {
        val report = UserReportDto(
            title = title,
            description = description,
            reportType = type,
            priority = priority,
            submitterId = userId
        )
        supabase.postgrest.from("user_reports").insert(report)
    }

    override suspend fun getMyReports(userId: String): List<Report> {
        return try {
            val result = supabase.postgrest.from("user_reports")
                .select(
                    columns = Columns.list(
                        "id", "title", "description", "report_type",
                        "status", "priority", "submitter_id",
                        "admin_response", "resolved_at", "created_at",
                        "profiles(id, username)"
                    )
                ) {
                    filter { eq("submitter_id", userId) }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }

            result.decodeList<UserReportWithProfileDto>().map { dto ->
                Report(
                    id = dto.id ?: "",
                    title = dto.title,
                    description = dto.description,
                    type = try {
                        ReportType.valueOf(dto.reportType)
                    } catch (_: Exception) {
                        ReportType.OTHER
                    },
                    status = try {
                        ReportStatus.valueOf(dto.status)
                    } catch (_: Exception) {
                        ReportStatus.OPEN
                    },
                    priority = try {
                        ReportPriority.valueOf(dto.priority)
                    } catch (_: Exception) {
                        ReportPriority.LOW
                    },
                    submitterUid = dto.submitterId,
                    submitterUsername = dto.profile?.username ?: "Unknown",
                    adminResponse = dto.adminResponse
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    @Serializable
    private data class UserReportWithProfileDto(
        val id: String? = null,
        val title: String,
        val description: String,
        @SerialName("report_type") val reportType: String,
        val status: String = "OPEN",
        val priority: String = "LOW",
        @SerialName("submitter_id") val submitterId: String,
        @SerialName("admin_response") val adminResponse: String? = null,
        @SerialName("resolved_at") val resolvedAt: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("profiles") val profile: ProfileDto? = null
    )

    override suspend fun getPendingSubmissionPackages(): Set<String> {
        return try {
            val result = supabase.postgrest.from("user_submissions")
                .select(columns = Columns.list("app_package")) {
                    filter { eq("status", "PENDING") }
                }
            result.decodeList<PackageNameOnlyDto>().map { it.appPackage }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    @Serializable
    private data class PackageNameOnlyDto(
        @SerialName("app_package") val appPackage: String
    )

    @Serializable
    private data class TargetWithAlternativesDto(
        @SerialName("package_name") val packageName: String,
        val alternatives: List<String>? = null
    )

    override suspend fun submitScanStats(
        deviceId: String,
        fossCount: Int,
        proprietaryCount: Int,
        unknownCount: Int,
        appVersion: String?
    ): Result<Unit> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id

        val stats = AppScanStatsDto(
            deviceId = deviceId,
            userId = userId,
            fossCount = fossCount,
            proprietaryCount = proprietaryCount,
            unknownCount = unknownCount,
            totalApps = fossCount + proprietaryCount + unknownCount,
            appVersion = appVersion
        )

        supabase.postgrest.from("app_scan_stats").upsert(stats) {
            onConflict = "device_id"
            defaultToNull = false
        }
        Log.d("SupabaseAppRepo", "Scan stats submitted for device: $deviceId")
    }
}

