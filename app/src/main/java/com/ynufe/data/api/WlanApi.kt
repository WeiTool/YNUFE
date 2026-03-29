package com.ynufe.data.api

import com.ynufe.data.model.LoginResponse
import com.ynufe.data.model.LogoutResponse
import com.ynufe.data.model.TokenResponse
import com.ynufe.data.model.WlanUserInfoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WlanApi {
    // 校园网token
    @GET("http://172.16.130.31/cgi-bin/get_challenge")
    suspend fun getToken(
        @Query("username") username: String,
        @Query("_") time: String,
        @Query("callback") callback: String = "api"
    ): Response<TokenResponse>

    // 校园网登陆
    @GET("http://172.16.130.31/cgi-bin/srun_portal")
    suspend fun loginWlan(
        @Query("action") action: String = "login",
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("os") os: String = "Windows 10",
        @Query("name") name: String = "Windows",
        @Query("double_stack") doubleStack: String = "0",
        @Query("chksum") chksum: String,
        @Query("info") info: String,
        @Query("ac_id") acId: String = "7",
        @Query("ip") ip: String,
        @Query("n") n: String = "200",
        @Query("type") type: String = "1",
        @Query("_") time: String,
        @Query("callback") callback: String = "api"
    ): Response<LoginResponse>

    // 校园网信息
    @GET("http://172.16.130.31/cgi-bin/rad_user_info")
    suspend fun wlanUserInfo(
        @Query("_") time: String,
        @Query("callback") callback: String = "api"
    ): Response<WlanUserInfoResponse>

    // 校园网退出
    @GET("http://172.16.130.31/cgi-bin/srun_portal")
    suspend fun logoutWlan(
        @Query("action") action: String = "logout",
        @Query("username") username: String,
        @Query("ip") ip: String,
        @Query("_") time: String,
        @Query("ac_id") acId: String = "7",
        @Query("callback") callback: String = "api"
    ): Response<LogoutResponse>
}