package com.ynufe.data.api

import com.ynufe.data.model.CheckVersionResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface VersionApi {
    // 仓库地址
    @GET("https://gitee.com/api/v5/repos/weitool/YNUFE/releases")
    suspend fun checkVersion(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 1,
        @Query("direction") direction: String = "desc"
    ): Response<List<CheckVersionResponse>>
}