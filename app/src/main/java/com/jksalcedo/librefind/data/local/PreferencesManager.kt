package com.jksalcedo.librefind.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class PreferencesManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("librefind_prefs", Context.MODE_PRIVATE)

    fun hasSeenTutorial(): Boolean {
        return prefs.getBoolean(KEY_TUTORIAL_COMPLETE, false)
    }

    fun setTutorialComplete() {
        prefs.edit { putBoolean(KEY_TUTORIAL_COMPLETE, true) }
    }

    fun resetTutorial() {
        prefs.edit { putBoolean(KEY_TUTORIAL_COMPLETE, false) }
    }

    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        prefs.edit { putString(KEY_DEVICE_ID, newId) }
        return newId
    }

    fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    // --- Network Consent Preferences ---
    fun hasAskedNetworkConsent(): Boolean {
        return prefs.getBoolean(KEY_HAS_ASKED_NETWORK_CONSENT, false)
    }

    fun setHasAskedNetworkConsent() {
        prefs.edit { putBoolean(KEY_HAS_ASKED_NETWORK_CONSENT, true) }
    }

    fun getNetworkConsentGranted(): Boolean {
        return prefs.getBoolean(KEY_NETWORK_CONSENT_GRANTED, false) // Default strictly to false
    }

    fun setNetworkConsentGranted(granted: Boolean) {
        prefs.edit { putBoolean(KEY_NETWORK_CONSENT_GRANTED, granted) }
    }

    fun observeNetworkConsentGranted(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_NETWORK_CONSENT_GRANTED) {
                trySend(getNetworkConsentGranted())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getNetworkConsentGranted())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun getAutoUpdateEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_UPDATE_ENABLED, true) // Default to true, but UI will depend on network consent
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_UPDATE_ENABLED, enabled) }
    }

    fun observeAutoUpdateEnabled(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_AUTO_UPDATE_ENABLED) {
                trySend(getAutoUpdateEnabled())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getAutoUpdateEnabled())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // --- System package filtering preferences ---
    /**
     * Returns whether system/vendor packages should be hidden from the UI.
     *
     * Default: true (on)
     */
    fun shouldHideSystemPackages(): Boolean {
        return prefs.getBoolean(KEY_HIDE_SYSTEM_PACKAGES, true)
    }

    /**
     * Persist user preference for hiding system/vendor packages.
     */
    fun setHideSystemPackages(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_HIDE_SYSTEM_PACKAGES, enabled) }
    }

    fun observeHideSystemPackages(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_HIDE_SYSTEM_PACKAGES) {
                trySend(shouldHideSystemPackages())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(shouldHideSystemPackages())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }



    /**
     * Returns whether pre-release updates should be included in update checks.
     *
     * Default: false (off)
     */
    fun shouldIncludePrereleases(): Boolean {
        return prefs.getBoolean(KEY_INCLUDE_PRERELEASES, false)
    }

    /**
     * Persist user preference for including pre-release updates.
     */
    fun setIncludePrereleases(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_INCLUDE_PRERELEASES, enabled) }
    }

    fun observeIncludePrereleases(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_INCLUDE_PRERELEASES) {
                trySend(shouldIncludePrereleases())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(shouldIncludePrereleases())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }



    fun getLogsLocation(): String? {
        return prefs.getString(KEY_LOGS_LOCATION, null)
    }

    fun setLogsLocation(uri: String?) {
        prefs.edit {
            putString(KEY_LOGS_LOCATION, uri)
        }
    }

    fun observeLogsLocation(): Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LOGS_LOCATION) {
                trySend(getLogsLocation())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getLogsLocation())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        private const val KEY_TUTORIAL_COMPLETE = "tutorial_complete"
        private const val KEY_DEVICE_ID = "device_id"

        // New keys for system package filtering
        private const val KEY_HIDE_SYSTEM_PACKAGES = "hide_system_packages"

        // New key for pre-release updates
        private const val KEY_INCLUDE_PRERELEASES = "include_prereleases"
        
        // Keys for explicit network consent
        private const val KEY_HAS_ASKED_NETWORK_CONSENT = "has_asked_network_consent"
        private const val KEY_NETWORK_CONSENT_GRANTED = "network_consent_granted"
        private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"

        // Logs location
        private const val KEY_LOGS_LOCATION = "logs_location"
    }
}
