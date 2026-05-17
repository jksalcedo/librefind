package com.jksalcedo.librefind.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_prefs")

class NotificationPrefsDataStore(private val context: Context) {

    companion object {
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val LAST_APP_CHECK_TIME_KEY = longPreferencesKey("last_app_check_time")
        private val LAST_SUBMISSION_CHECK_TIME_KEY = longPreferencesKey("last_submission_check_time")
        private val NOTIFICATION_INTERVAL_MINS_KEY = longPreferencesKey("notification_interval_mins")
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.notificationDataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: true // Default to true
    }

    val notificationIntervalFlow: Flow<Long> = context.notificationDataStore.data.map { preferences ->
        preferences[NOTIFICATION_INTERVAL_MINS_KEY] ?: 60L // Default to 1 hour
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setNotificationInterval(minutes: Long) {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATION_INTERVAL_MINS_KEY] = minutes
        }
    }

    suspend fun getLastAppCheckTime(): Long {
        return context.notificationDataStore.data.map { preferences ->
            preferences[LAST_APP_CHECK_TIME_KEY] ?: 0L
        }.first()
    }

    suspend fun setLastAppCheckTime(timeMs: Long) {
        context.notificationDataStore.edit { preferences ->
            preferences[LAST_APP_CHECK_TIME_KEY] = timeMs
        }
    }

    suspend fun getLastSubmissionCheckTime(): Long {
        return context.notificationDataStore.data.map { preferences ->
            preferences[LAST_SUBMISSION_CHECK_TIME_KEY] ?: 0L
        }.first()
    }

    suspend fun setLastSubmissionCheckTime(timeMs: Long) {
        context.notificationDataStore.edit { preferences ->
            preferences[LAST_SUBMISSION_CHECK_TIME_KEY] = timeMs
        }
    }
}
