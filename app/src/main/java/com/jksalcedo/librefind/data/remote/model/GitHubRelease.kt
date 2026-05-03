package com.jksalcedo.librefind.data.remote.model

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String,
    @SerializedName("prerelease") val isPrerelease: Boolean,
    @SerializedName("assets") val assets: List<Asset>
) {
    data class Asset(
        @SerializedName("name") val name: String,
        @SerializedName("browser_download_url") val downloadUrl: String,
        @SerializedName("content_type") val contentType: String
    )
}
