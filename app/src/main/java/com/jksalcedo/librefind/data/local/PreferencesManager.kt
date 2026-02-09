package com.jksalcedo.librefind.data.local

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class PreferencesManager(private val context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("librefind_prefs", Context.MODE_PRIVATE)

    fun hasSeenOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun setOnboardingComplete() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }

    fun getLastSeenVersion(): Int {
        return prefs.getInt(KEY_LAST_VERSION, 0)
    }

    fun setLastSeenVersion(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_VERSION, versionCode).apply()
    }

    fun shouldShowChangelog(currentVersion: Int): Boolean {
        return getLastSeenVersion() < currentVersion
    }

    fun hasSeenTutorial(): Boolean {
        return prefs.getBoolean(KEY_TUTORIAL_COMPLETE, false)
    }

    fun setTutorialComplete() {
        prefs.edit().putBoolean(KEY_TUTORIAL_COMPLETE, true).apply()
    }

    fun resetTutorial() {
        prefs.edit().putBoolean(KEY_TUTORIAL_COMPLETE, false).apply()
    }

    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing
        
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_LAST_VERSION = "last_seen_version"
        private const val KEY_TUTORIAL_COMPLETE = "tutorial_complete"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
