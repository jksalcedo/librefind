package com.jksalcedo.librefind.data.remote.model

import com.google.gson.annotations.SerializedName

data class CatalogIndexDto(
    @SerializedName("filename") val filename: String,
    @SerializedName("hash") val hash: String,
    @SerializedName("timestamp") val timestamp: Long
)

data class CatalogBundleDto(
    @SerializedName("targets") val targets: List<CatalogTargetDto> = emptyList(),
    @SerializedName("solutions") val solutions: List<CatalogSolutionDto> = emptyList(),
    @SerializedName("mappings") val mappings: List<CatalogMappingDto> = emptyList()
)

data class CatalogTargetDto(
    @SerializedName("id") val id: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("name") val name: String,
    @SerializedName("categories") val categories: List<String> = emptyList()
)

data class CatalogSolutionDto(
    @SerializedName("id") val id: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("license") val license: String? = null,
    @SerializedName("iconUrl") val iconUrl: String? = null,
    @SerializedName("websiteUrl") val websiteUrl: String? = null,
    @SerializedName("repositoryUrl") val repositoryUrl: String? = null,
    @SerializedName("fdroidUrl") val fdroidUrl: String? = null,
    @SerializedName("upvotes") val upvotes: Int = 0,
    @SerializedName("downvotes") val downvotes: Int = 0
)

data class CatalogMappingDto(
    @SerializedName("targetId") val targetId: String,
    @SerializedName("solutionId") val solutionId: String
)
