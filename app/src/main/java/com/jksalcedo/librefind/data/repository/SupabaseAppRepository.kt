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
                val votesJson = dto.votes?.let { json ->
                    try {
                        json as? kotlinx.serialization.json.JsonObject
                    } catch (e: Exception) {
                        Log.e(
                            "SupabaseAppRepo",
                            "Failed to parse votes JSON for ${dto.packageName}",
                            e
                        )
                        null
                    }
                }

                val ratingAvg = votesJson?.get("average")?.let { avgElement ->
                    try {
                        when (avgElement) {
                            is kotlinx.serialization.json.JsonPrimitive -> avgElement.content.toFloatOrNull()
                            else -> null
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "SupabaseAppRepo",
                            "Failed to parse average for ${dto.packageName}",
                            e
                        )
                        null
                    }
                } ?: 0.0f

                val ratingCount = votesJson?.get("count")?.let { countElement ->
                    try {
                        when (countElement) {
                            is kotlinx.serialization.json.JsonPrimitive -> countElement.content.toIntOrNull()
                            else -> null
                        }
                    } catch (e: Exception) {
                        Log.e("SupabaseAppRepo", "Failed to parse count for ${dto.packageName}", e)
                        null
                    }
                } ?: 0

                Log.d(
                    "SupabaseAppRepo",
                    "Parsed ratings for ${dto.packageName}: avg=$ratingAvg, count=$ratingCount, votesJson=$votesJson"
                )

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

            val votesJson = dto.votes?.let { json ->
                try {
                    json as? kotlinx.serialization.json.JsonObject
                } catch (e: Exception) {
                    Log.e("SupabaseAppRepo", "Failed to parse votes JSON for ${dto.packageName}", e)
                    null
                }
            }

            val ratingAvg = votesJson?.get("average")?.let { avgElement ->
                try {
                    when (avgElement) {
                        is kotlinx.serialization.json.JsonPrimitive -> avgElement.content.toFloatOrNull()
                        else -> null
                    }
                } catch (e: Exception) {
                    Log.e("SupabaseAppRepo", "Failed to parse average for ${dto.packageName}", e)
                    null
                }
            } ?: 0.0f

            val ratingCount = votesJson?.get("count")?.let { countElement ->
                try {
                    when (countElement) {
                        is kotlinx.serialization.json.JsonPrimitive -> countElement.content.toIntOrNull()
                        else -> null
                    }
                } catch (e: Exception) {
                    Log.e("SupabaseAppRepo", "Failed to parse count for ${dto.packageName}", e)
                    null
                }
            } ?: 0

            Log.d(
                "SupabaseAppRepo",
                "Parsed ratings for ${dto.packageName}: avg=$ratingAvg, count=$ratingCount, votesJson=$votesJson"
            )

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
        userId: String
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
                submitterId = userId
            )
            supabase.postgrest.from("user_submissions").insert(submission)
            Log.d("SupabaseAppRepo", "Submission successful")
        } catch (e: Exception) {
            Log.e("SupabaseAppRepo", "Submission failed", e)
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
                    type = if (dto.proprietaryPackage != null) SubmissionType.NEW_ALTERNATIVE else SubmissionType.NEW_PROPRIETARY,
                    proprietaryPackages = dto.proprietaryPackage ?: "",
                    submittedApp = SubmittedApp(
                        name = dto.appName,
                        packageName = dto.appPackage,
                        description = dto.description
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

    override suspend fun getUserVote(packageName: String, userId: String): Int? {
        return try {
            supabase.postgrest.from("user_votes")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("package_name", packageName)
                        eq("vote_type", "usability")
                    }
                }.decodeSingleOrNull<UserVoteDto>()?.value
        } catch (e: Exception) {
            e.printStackTrace()
            null
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

    @Serializable
    private data class UserSubmissionWithProfileDto(
        val id: String? = null,
        @SerialName("app_name") val appName: String,
        @SerialName("app_package") val appPackage: String,
        val description: String,
        @SerialName("proprietary_package") val proprietaryPackage: String? = null,
        val status: String = "PENDING",
        @SerialName("submitter_id") val submitterId: String,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("rejection_reason") val rejectionReason: String? = null,
        @SerialName("profiles") val profile: ProfileDto? = null // Joined table
    )
}
