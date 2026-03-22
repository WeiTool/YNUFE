package com.ynufe.data.api

import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiServices {
    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    // 第一套进入网页
    @GET("https://xjwis.ynufe.edu.cn/")
    suspend fun firstInterWeb(
        @Header("User-Agent") ua: String = USER_AGENT,
    ): Response<ResponseBody>

    // 第二套进入网页
    @GET("https://xjwis.ynufe.edu.cn/jsxsd/")
    suspend fun secondeInterWeb(
        @Header("User-Agent") ua: String = USER_AGENT,
    ): Response<ResponseBody>

    // 验证码
    @GET("verifycode.servlet")
    suspend fun getVerifyCode(
        @Header("User-Agent") ua: String = USER_AGENT,
    ): Response<ResponseBody>

    // 获取Session代码
    @POST("Logon.do")
    suspend fun initSession(
        @Header("User-Agent") ua: String = USER_AGENT,
        @Body body: RequestBody = "".toRequestBody(null),
        @Query("method") method: String = "logon",
        @Query("flag") flag: String = "sess"
    ): Response<String>

    // 发送用户信息
    @FormUrlEncoded
    @POST("Logon.do")
    suspend fun firstPostUser(
        @Header("Referer") referer: String = "https://xjwis.ynufe.edu.cn/",
        @Header("User-Agent") ua: String = USER_AGENT,
        @Query("method") method: String = "logonLdap",
        @FieldMap fields: Map<String, String>
    ): Response<String>

    // 发送用户信息第二套
    @FormUrlEncoded
    @POST("jsxsd/xk/LoginToXkLdap")
    suspend fun secondePostUser(
        @Header("Referer") referer: String = "https://xjwis.ynufe.edu.cn/jsxsd/",
        @Header("User-Agent") ua: String = USER_AGENT,
        @FieldMap fields: Map<String, String>
    ): Response<String>

    // 获取主要的网页信息
    @GET("jsxsd/framework/xsMain_new.jsp?t1=1")
    suspend fun getMainPage(
        @Header("User-Agent") ua: String = USER_AGENT,
    ): Response<String>

    // 获取课表
    @GET("jsxsd/xskb/xskb_list.do")
    suspend fun getCourseTable(
        @Header("User-Agent") ua: String = USER_AGENT,
    ): Response<String>

    // 获取成绩
    @FormUrlEncoded
    @POST("jsxsd/kscj/cjcx_list")
    suspend fun getGradeTable(
        @Header("User-Agent") ua: String = USER_AGENT,
        @FieldMap fields: Map<String, String>
    ): Response<String>

    // 退出系统
    @GET("jsxsd/xk/LoginToXk")
    suspend fun logoutSystem(
        @Query("method") method: String = "exit",
        @Query("tktime") tktime: Long // 不要在这里赋默认值，在调用时传
    ): Response<ResponseBody>
}