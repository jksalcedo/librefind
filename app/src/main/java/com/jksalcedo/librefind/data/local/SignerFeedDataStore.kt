package com.jksalcedo.librefind.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.jksalcedo.librefind.data.remote.model.RemoteSignerFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "signer_feed")

class SignerFeedDataStore(private val context: Context, private val gson: Gson) {

    companion object {
        private val FEED_JSON_KEY = stringPreferencesKey("feed_json")
        private val ETAG_KEY = stringPreferencesKey("etag")

    }

    val feedFlow: Flow<RemoteSignerFeed?> = context.dataStore.data.map { preferences ->
        val json = preferences[FEED_JSON_KEY]
        if (json != null) {
            try {
                gson.fromJson(json, RemoteSignerFeed::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun saveFeed(feed: RemoteSignerFeed, etag: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[FEED_JSON_KEY] = gson.toJson(feed)
            if (etag != null) {
                preferences[ETAG_KEY] = etag
            }
        }
    }

    suspend fun getEtag(): String? = context.dataStore.data.map { preferences ->
        preferences[ETAG_KEY]
    }.firstOrNull()
}
