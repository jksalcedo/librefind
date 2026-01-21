package com.jksalcedo.librefind.data.repository

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
            // 1. Get the target to find alternative package names
            val target = supabase.postgrest.from("targets")
                .select {
                    filter {
                        eq("package_name", packageName)
                    }
                }.decodeSingleOrNull<TargetDto>() ?: return emptyList()

            if (target.alternatives.isEmpty()) return emptyList()

            // 2. Fetch the solutions
            val solutions = supabase.postgrest.from("solutions")
                .select {
                    filter {
                        isIn("package_name", target.alternatives)
                    }
                }.decodeList<SolutionDto>()

            // 3. Map to Domain Model
            solutions.map { dto ->
                Alternative(
                    id = dto.packageName, // Using package name as ID
                    name = dto.name,
                    packageName = dto.packageName,
                    license = dto.license,
                    repoUrl = dto.repoUrl,
                    fdroidId = dto.fdroidId,
                    iconUrl = dto.iconUrl,
                    ratingAvg = 0.0f, // Needs calculation from votes JSON
                    description = dto.description,
                    features = dto.features
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

            Alternative(
                id = dto.packageName,
                name = dto.name,
                packageName = dto.packageName,
                license = dto.license,
                repoUrl = dto.repoUrl,
                fdroidId = dto.fdroidId,
                iconUrl = dto.iconUrl,
                ratingAvg = 0.0f,
                description = dto.description,
                features = dto.features
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
        userId: String
    ): Result<Unit> = runCatching {
        val submission = UserSubmissionDto(
            appName = "Unknown",
            appPackage = alternativePackage,
            description = "Suggested alternative",
            proprietaryPackage = proprietaryPackage,
            submitterId = userId
        )
        supabase.postgrest.from("user_submissions").insert(submission)
    }

    override suspend fun castVote(
        packageName: String,
        voteType: String,
        value: Int
    ): Result<Unit> = runCatching {
        val userId =
            supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not logged in")

        // Insert into user_votes
        val vote = UserVoteDto(
            userId = userId,
            packageName = packageName,
            voteType = voteType,
            value = value
        )
        supabase.postgrest.from("user_votes").insert(vote)

        val params = mapOf(
            "package_name" to packageName,
            "vote_type" to voteType,
            "value" to value
        )
        supabase.postgrest.rpc("vote_for_app", params)
    }

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
            // Fetch submissions and join profiles to get username
            val result = supabase.postgrest.from("user_submissions")
                .select(columns = Columns.list("*, profiles(username)")) {
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
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun checkDuplicateApp(name: String, packageName: String): Boolean {
        // Check if exists in solutions or targets
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
        @SerialName("profiles") val profile: ProfileDto? = null // Joined table
    )
}
