package com.jksalcedo.librefind.data.repository

import android.util.Log
import com.jksalcedo.librefind.data.remote.model.ProfileDto
import com.jksalcedo.librefind.data.remote.model.SolutionDto
import com.jksalcedo.librefind.data.remote.model.TargetDto
import com.jksalcedo.librefind.data.remote.model.UserSubmissionDto
import com.jksalcedo.librefind.data.remote.model.UserVoteDto
import com.jksalcedo.librefind.domain.model.Alternative
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

class SupabaseAppRepository(
    private val supabase: SupabaseClient
) : AppRepository {

    override suspend fun isProprietary(packageName: String): Boolean {
        return try {
            val count = supabase.postgrest.from("targets")
                .select {
                    count(Count.EXACT)
                    filter {
                        eq("package_name", packageName)
                    }
                }.countOrNull()
            (count ?: 0) > 0
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun isSolution(packageName: String): Boolean {
        return try {
            val count = supabase.postgrest.from("solutions")
                .select {
                    count(Count.EXACT)
                    filter {
                        eq("package_name", packageName)
                    }
                }.countOrNull()
            (count ?: 0) > 0
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getAlternatives(packageName: String): List<Alternative> {
        return try {
            //  Get the target to find alternative package names
            val target = supabase.postgrest.from("targets")
                .select {
                    filter {
                        eq("package_name", packageName)
                    }
                }.decodeSingleOrNull<TargetDto>() ?: return emptyList()

            if (target.alternatives.orEmpty().isEmpty()) return emptyList()

            val solutions = supabase.postgrest.from("solutions")
                .select {
                    filter {
                        isIn("package_name", target.alternatives.orEmpty())
                    }
                }.decodeList<SolutionDto>()

            solutions.map { dto ->
                val usabilityRating = dto.ratingUsability ?: 0f
                val privacyRating = dto.ratingPrivacy ?: 0f
                val featuresRating = dto.ratingFeatures ?: 0f
                val ratingCount = dto.voteCount ?: 0

                val ratingAvg = if (ratingCount > 0) {
                    val sum = usabilityRating + privacyRating + featuresRating
                    val nonZeroCount =
                        listOf(usabilityRating, privacyRating, featuresRating).count { it > 0 }
                    if (nonZeroCount > 0) sum / nonZeroCount else 0f
                } else 0f

                Alternative(
                    id = dto.packageName,
                    name = dto.name,
                    packageName = dto.packageName,
                    license = dto.license,
                    repoUrl = dto.repoUrl,
                    fdroidId = dto.fdroidId,
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
                .select {
                    filter {
                        eq("package_name", packageName)
                    }
                }.decodeSingleOrNull<SolutionDto>() ?: return null

            val usabilityRating = dto.ratingUsability ?: 0f
            val privacyRating = dto.ratingPrivacy ?: 0f
            val featuresRating = dto.ratingFeatures ?: 0f
            val ratingCount = dto.voteCount ?: 0

            val ratingAvg = if (ratingCount > 0) {
                val sum = usabilityRating + privacyRating + featuresRating
                val nonZeroCount =
                    listOf(usabilityRating, privacyRating, featuresRating).count { it > 0 }
                if (nonZeroCount > 0) sum / nonZeroCount else 0f
            } else 0f

            Alternative(
                id = dto.packageName,
                name = dto.name,
                packageName = dto.packageName,
                license = dto.license,
                repoUrl = dto.repoUrl,
                fdroidId = dto.fdroidId,
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

    override suspend fun submitAlternative(
        proprietaryPackage: String,
        alternativePackage: String,
        appName: String,
        description: String,
        repoUrl: String,
        fdroidId: String,
        license: String,
        userId: String,
        alternatives: List<String>
    ): Result<Unit> = runCatching {
        Log.d(
            "SupabaseAppRepo",
            "Submitting alternative: $appName ($alternativePackage) for $proprietaryPackage by $userId"
        )
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
                submitterId = userId
            )
            supabase.postgrest.from("user_submissions").insert(submission)
            Log.d("SupabaseAppRepo", "Submission successful")
        } catch (e: Exception) {
            Log.e("SupabaseAppRepo", "Submission failed", e)
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
        Log.d("SupabaseAppRepo", "Updating submission $id")
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
                Log.w(
                    "SupabaseAppRepo",
                    "Update returned 0 rows. RLS might be blocking update of REJECTED submission. Falling back to INSERT."
                )
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

        // Using user_submissions as a temporary store for feedback
        val submission = UserSubmissionDto(
            appName = "Feedback ($type)",
            appPackage = packageName,
            description = text,
            submitterId = userId,
            status = "PENDING"
        )
        supabase.postgrest.from("user_submissions").insert(submission)
    }

    override suspend fun getMySubmissions(userId: String): List<Submission> {
        return try {
            val result = supabase.postgrest.from("user_submissions")
                .select(columns = Columns.list("*, profiles(id, username)")) {
                    filter {
                        eq("submitter_id", userId)
                    }
                }

            val submissions = result.decodeList<UserSubmissionWithProfileDto>()

            submissions.map { dto ->
                Submission(
                    id = dto.id ?: "",
                    type = if (!dto.license.isNullOrBlank() || !dto.repoUrl.isNullOrBlank()) SubmissionType.NEW_ALTERNATIVE else SubmissionType.NEW_PROPRIETARY,
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
                    status = try {
                        SubmissionStatus.valueOf(dto.status)
                    } catch (_: Exception) {
                        SubmissionStatus.PENDING
                    },
                    rejectionReason = dto.rejectionReason
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getUserVote(packageName: String, userId: String): Map<String, Int?> {
        return try {
            val votes = supabase.postgrest.from("user_votes")
                .select {
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

    override suspend fun checkDuplicateApp(name: String, packageName: String): Boolean {
        val inSolutions = supabase.postgrest.from("solutions")
            .select { count(Count.EXACT); filter { eq("package_name", packageName) } }.countOrNull()
            ?: 0
        val inTargets = supabase.postgrest.from("targets")
            .select { count(Count.EXACT); filter { eq("package_name", packageName) } }.countOrNull()
            ?: 0
        return inSolutions > 0 || inTargets > 0
    }

    override suspend fun searchSolutions(query: String, limit: Int): List<Alternative> {
        if (query.isBlank()) return emptyList()

        return try {
            val solutions = supabase.postgrest.from("solutions")
                .select {
                    filter {
                        or {
                            ilike("name", "%$query%")
                            ilike("package_name", "%$query%")
                        }
                    }
                    limit(limit.toLong())
                }.decodeList<SolutionDto>()

            solutions.map { dto ->
                val usabilityRating = dto.ratingUsability ?: 0f
                val privacyRating = dto.ratingPrivacy ?: 0f
                val featuresRating = dto.ratingFeatures ?: 0f
                val ratingCount = dto.voteCount ?: 0

                val ratingAvg = if (ratingCount > 0) {
                    val sum = usabilityRating + privacyRating + featuresRating
                    val nonZeroCount =
                        listOf(usabilityRating, privacyRating, featuresRating).count { it > 0 }
                    if (nonZeroCount > 0) sum / nonZeroCount else 0f
                } else 0f

                Alternative(
                    id = dto.packageName,
                    name = dto.name,
                    packageName = dto.packageName,
                    license = dto.license,
                    repoUrl = dto.repoUrl,
                    fdroidId = dto.fdroidId,
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

    @Serializable
    private data class UserSubmissionWithProfileDto(
        val id: String? = null,
        @SerialName("app_name") val appName: String,
        @SerialName("app_package") val appPackage: String,
        val description: String,
        @SerialName("proprietary_package") val proprietaryPackage: String? = null,
        @SerialName("repo_url") val repoUrl: String? = null,
        @SerialName("fdroid_id") val fdroidId: String? = null,
        val license: String? = null,
        val status: String = "PENDING",
        @SerialName("submitter_id") val submitterId: String,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("rejection_reason") val rejectionReason: String? = null,
        @SerialName("profiles") val profile: ProfileDto? = null // Joined table
    )
}
