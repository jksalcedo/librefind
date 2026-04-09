package com.jksalcedo.librefind.data.local

import com.jksalcedo.librefind.data.remote.SignerApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrustedRomSignerDb(
    private val dataStore: SignerFeedDataStore,
    private val apiService: SignerApiService
) {
    /**
     * Bundled platform signer digests:
     * Used to validate AOSP-name system packages (com.android.*).
     */
    private val bundledPlatformSigners = setOf(
        "c45d15cc0ebf9b91fe03246ad16377fe494a2448802aadc7af632c3197c7e0dc",
        "04a4b3425f57c3669a73cd2710c7cbf00d222b6d1202dc474e475e5eb47d5c4c",
        "d70e106dfa5f730bf098f4570e7ec80b803e31f33e5678cb5de45d0605d4f692",
        "a8318efeeb2f37bd9020aebb576dd952654792f12adc3e5b54fad268ed719825"
    )

    /**
     * Bundled ROM app signer digests:
     * Used for ROM-specific system apps (e.g. lineage/voltage/graphene apps).
     */
    private val bundledRomAppSigners = setOf(
        "035d404701c6d5648fd32fa9f199be75be76c819fab149e40996c87d015f57c9",
        "13eb13912637fce93694905057cdc06f7232c8fea6f42d0782d03a10935ea325"
    )

    /**
     * Bundled ROM app prefixes.
     */
    private val bundledRomPrefixes = listOf(
        "com.lineageos.",
        "org.lineageos.",
        "org.lineageos.updater",
        "org.voltageos.",
        "com.crdroid.",
        "org.evolutionx.",
        "org.aicp.",
        "org.pixelexperience.",
        "org.derpfest.",
        "org.projectelixir.",
        "com.risingos.",
        "org.havoc.",
        "org.arrowos.",
        "org.superioros.",
        "org.blissroms."
    )

    val platformSigners: Flow<Set<String>> = dataStore.feedFlow.map { remote ->
        bundledPlatformSigners + (remote?.platformSigners?.map { it.lowercase().trim() }?.toSet()
            ?: emptySet())
    }

    val romAppSigners: Flow<Set<String>> = dataStore.feedFlow.map { remote ->
        bundledRomAppSigners + (remote?.romAppSigners?.map { it.lowercase().trim() }?.toSet()
            ?: emptySet())
    }

    val romPrefixes: Flow<List<String>> = dataStore.feedFlow.map { remote ->
        (bundledRomPrefixes + (remote?.romPrefixes ?: emptyList())).distinct()
    }

    suspend fun refreshFeed() {
        try {
            val url = "https://raw.githubusercontent.com/jksalcedo/librefind/main/signers.json"
            val feed = apiService.getSignerFeed(url)
            dataStore.saveFeed(feed)
        } catch (e: Exception) {
            // Fallback to bundled is automatic via Flows
            e.printStackTrace()
        }
    }
}
