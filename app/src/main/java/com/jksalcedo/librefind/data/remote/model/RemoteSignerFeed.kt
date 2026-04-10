package com.jksalcedo.librefind.data.remote.model

import com.google.gson.annotations.SerializedName

data class RemoteSignerFeed(
    val version: Int,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("platform_signers") val platformSigners: List<String>,
    @SerializedName("rom_app_signers") val romAppSigners: List<String>,
    @SerializedName("rom_prefixes") val romPrefixes: List<String>
)
