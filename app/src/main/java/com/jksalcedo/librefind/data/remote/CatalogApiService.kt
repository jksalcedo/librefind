package com.jksalcedo.librefind.data.remote

import com.jksalcedo.librefind.data.remote.model.CatalogIndexDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface CatalogApiService {
    @GET
    suspend fun getCatalogIndex(@Url url: String): CatalogIndexDto

    @GET
    @Streaming
    suspend fun getCatalogBundle(@Url url: String): ResponseBody
}
