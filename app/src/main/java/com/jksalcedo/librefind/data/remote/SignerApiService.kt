package com.jksalcedo.librefind.data.remote

import com.jksalcedo.librefind.data.remote.model.RemoteSignerFeed
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface SignerApiService {
    @GET
    suspend fun getSignerFeed(
        @Url url: String,
        @Header("If-None-Match") etag: String? = null
    ): Response<RemoteSignerFeed>
}
