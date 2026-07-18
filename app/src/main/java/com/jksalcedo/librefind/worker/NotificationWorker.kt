package com.jksalcedo.librefind.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jksalcedo.librefind.data.local.NotificationPrefsDataStore
import com.jksalcedo.librefind.utils.NotificationHelper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Instant.Companion.fromEpochMilliseconds

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val prefs: NotificationPrefsDataStore by inject()
    private val supabase: SupabaseClient by inject()

    @Serializable
    private data class AppCheckDto(
        val packageName: String,
        val name: String,
        val createdAt: String? = null
    )

    @Serializable
    private data class SubmissionCheckDto(
        val id: String,
        val appName: String? = null,
        val proprietaryPackage: String? = null,
        val status: String,
        val lastEditedAt: String
    )

    override suspend fun doWork(): Result {
        return try {
            val enabled = prefs.notificationsEnabledFlow.first()
            if (!enabled) {
                return Result.success()
            }

            NotificationHelper.createNotificationChannel(applicationContext)

            val currentUserId = supabase.auth.currentUserOrNull()?.id
            val now = System.currentTimeMillis()

            // Check for new apps in the main directory
            val lastAppCheck = prefs.getLastAppCheckTime()
            if (lastAppCheck > 0) {
                val lastAppInstant = fromEpochMilliseconds(lastAppCheck)
                val newApps = try {
                    supabase.postgrest.from("solutions")
                        .select {
                            filter { gt("created_at", lastAppInstant.toString()) }
                        }.decodeList<AppCheckDto>()
                } catch (_: Exception) {
                    emptyList() // Fallback if created_at doesn't exist
                }

                if (newApps.isNotEmpty()) {
                    val appNames = newApps.joinToString(", ") { it.name }
                    NotificationHelper.showNotification(
                        context = applicationContext,
                        notificationId = 1001,
                        title = "New FOSS Apps Added!",
                        content = "Check out the newly added apps: $appNames"
                    )
                }
            }
            prefs.setLastAppCheckTime(now)

            // Check for submission status updates (if logged in)
            if (currentUserId != null) {
                val lastSubCheck = prefs.getLastSubmissionCheckTime()
                if (lastSubCheck > 0) {
                    val lastSubInstant = fromEpochMilliseconds(lastSubCheck)

                    // Check standard submissions
                    val updatedSubs = supabase.postgrest.from("user_submissions")
                        .select {
                            filter {
                                eq("submitter_id", currentUserId)
                                gt("last_edited_at", lastSubInstant.toString())
                                isIn("status", listOf("APPROVED", "REJECTED"))
                            }
                        }.decodeList<SubmissionCheckDto>()

                    // Check linking submissions
                    val updatedLinks = supabase.postgrest.from("user_linking_submissions")
                        .select {
                            filter {
                                eq("submitter_id", currentUserId)
                                gt("last_edited_at", lastSubInstant.toString())
                                isIn("status", listOf("APPROVED", "REJECTED"))
                            }
                        }.decodeList<SubmissionCheckDto>()

                    val allUpdates = updatedSubs + updatedLinks
                    allUpdates.forEachIndexed { index, sub ->
                        val name = sub.appName ?: sub.proprietaryPackage ?: "Your submission"
                        NotificationHelper.showNotification(
                            context = applicationContext,
                            notificationId = 2000 + index,
                            title = "Submission Updated",
                            content = "$name was ${sub.status.lowercase()}."
                        )
                    }
                }
                prefs.setLastSubmissionCheckTime(now)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
