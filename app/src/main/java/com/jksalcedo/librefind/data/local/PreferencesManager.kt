package com.jksalcedo.librefind.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
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

    companion object {
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_LAST_VERSION = "last_seen_version"
    }
}
