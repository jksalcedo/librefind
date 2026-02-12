package com.jksalcedo.librefind.data.repository

import com.jksalcedo.librefind.data.remote.model.ProfileDto
import com.jksalcedo.librefind.domain.model.UserProfile
import com.jksalcedo.librefind.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class SupabaseAuthRepository(
    private val supabase: SupabaseClient
) : AuthRepository {

    private val auth: Auth = supabase.auth

    override val currentUser: Flow<UserProfile?> = auth.sessionStatus.map { status ->
        if (status is SessionStatus.Authenticated) {
            fetchUserProfile(status.session.user?.id)
        } else {
            null
        }
    }

    override suspend fun signUp(email: String, password: String, username: String): Result<Unit> =
        runCatching {
            auth.signUpWith(Email, redirectUrl = "librefind://login-callback") {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("username", username)
                }
            }

            val userId = auth.currentUserOrNull()?.id
            if (userId != null) {
                supabase.postgrest.from("profiles").upsert(
                    ProfileDto(id = userId, username = username)
                )
            }
        }.also { result ->
            result.exceptionOrNull()?.let {
                android.util.Log.e("SignUp", "SignUp failed: ${it.message}", it)
            }
        }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signInWithGithub(): Result<Unit> = runCatching {
        auth.signInWith(Github, redirectUrl = "librefind://login-callback")
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun getCurrentUser(): UserProfile? {
        val user = auth.currentUserOrNull() ?: return null
        return fetchUserProfile(user.id)
    }

    override suspend fun updateProfile(username: String): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not logged in")

        val profileUpdate = ProfileDto(
            id = userId,
            username = username
        )

        supabase.postgrest.from("profiles").update(profileUpdate) {
            filter {
                eq("id", userId)
            }
        }

    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        supabase.postgrest.rpc("delete_account")
        auth.currentUserOrNull()?.id.let {
            auth.admin.deleteUser(it!!)
        }
        auth.signOut()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun fetchUserProfile(userId: String?): UserProfile? {
        if (userId == null) return null
        return try {
            val profileDto = supabase.postgrest.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }.decodeSingleOrNull<ProfileDto>()

            profileDto?.let {
                UserProfile(
                    uid = it.id,
                    username = it.username ?: "",
                    email = auth.currentUserOrNull()?.email ?: "",
                    joinedAt = it.createdAt?.let { dateStr ->
                        try {
                            Instant.parse(dateStr).toEpochMilliseconds()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }
                    } ?: System.currentTimeMillis(),
                    submissionCount = it.submissionCount,
                    approvedCount = it.approvedCount
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
