package com.jksalcedo.librefind.data.remote

import com.jksalcedo.librefind.data.remote.model.GitHubRelease
import retrofit2.http.GET

interface UpdateApiService {
    @GET("repos/jksalcedo/librefind/releases")
    suspend fun getReleases(): List<GitHubRelease>
}
