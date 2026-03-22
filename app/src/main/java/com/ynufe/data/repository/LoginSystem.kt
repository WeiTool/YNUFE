package com.ynufe.data.repository

import com.ynufe.data.api.ApiServices
import com.ynufe.di.MemoryCookieJar
import com.ynufe.utils.EncodeUtils
import com.ynufe.utils.LoginResult
import retrofit2.Response
import javax.inject.Inject
import android.util.Log // 建议加上日志以便调试

class LoginSystem @Inject constructor(
    private val apiServices: ApiServices,
    private val encoder: EncodeUtils,
    private val parser: ParseJsp,
    private val cookieJar: MemoryCookieJar
) {
    private var sessData: String = ""
    private var useSecondFlow: Boolean = false

    // ----------------------------------------------------------------
    // 准备阶段
    // ----------------------------------------------------------------

    suspend fun prepareLogin(): ByteArray? {
        // 尝试第一套流程
        prepareFirstFlow()?.let {
            useSecondFlow = false
            return it
        }

        // 第一套失败：清理状态
        logout()
        sessData = ""

        // 尝试第二套流程
        return prepareSecondFlow()?.also {
            useSecondFlow = true
        }
    }

    private suspend fun prepareFirstFlow(): ByteArray? = runCatching {
        val homeResponse = apiServices.firstInterWeb()
        if (!homeResponse.isSuccessful) return@runCatching null

        val captchaBytes = fetchVerifyCode() ?: return@runCatching null
        val data = fetchSessData() ?: return@runCatching null

        sessData = data
        captchaBytes
    }.onFailure { Log.e("LoginSystem", "First flow error: ${it.message}") }.getOrNull()

    private suspend fun prepareSecondFlow(): ByteArray? = runCatching {
        val homeResponse = apiServices.secondeInterWeb()
        if (!homeResponse.isSuccessful) return@runCatching null

        fetchVerifyCode()
    }.onFailure { Log.e("LoginSystem", "Second flow error: ${it.message}") }.getOrNull()

    // ----------------------------------------------------------------
    // 提交阶段
    // ----------------------------------------------------------------

    suspend fun submitLogin(
        userAccount: String,
        userPassword: String,
        code: String
    ): LoginResult = runCatching {
        val response = if (useSecondFlow) {
            secondePostUserInfo(userAccount, userPassword, code)
        } else {
            postUserInfo(userAccount, userPassword, code)
        }

        if (response != null && response.isSuccessful) {
            val finalUrl = response.raw().request.url.toString()
            val responseHtml = response.body() ?: ""

            if (finalUrl.contains("xsMain.jsp")) {
                LoginResult.Success(Unit)
            } else {
                LoginResult.Error(parser.parseLoginError(responseHtml))
            }
        } else {
            LoginResult.Error("服务器响应异常: ${response?.code()}")
        }
    }.getOrElse { e ->
        // 发生系统级异常（如断网）时返回
        LoginResult.Error("系统错误: ${e.localizedMessage ?: "未知异常"}")
    }.also {
        // 无论成功还是异常，最后都重置状态
        sessData = ""
        useSecondFlow = false
    }

    // ----------------------------------------------------------------
    // 验证码 / 会话
    // ----------------------------------------------------------------

    suspend fun fetchVerifyCode(): ByteArray? = runCatching {
        val response = apiServices.getVerifyCode()
        if (response.isSuccessful) response.body()?.bytes() else null
    }.getOrNull()

    private suspend fun fetchSessData(): String? = runCatching {
        val response = apiServices.initSession()
        if (response.isSuccessful) {
            val body = response.body()
            if (body == null || body == "no") null else body
        } else null
    }.getOrNull()

    // ----------------------------------------------------------------
    // 表单构造 (由于内部调用了 encoder 可能会抛异常，同样可以包裹)
    // ----------------------------------------------------------------

    private suspend fun postUserInfo(
        userAccount: String,
        userPassword: String,
        code: String
    ): Response<String>? = runCatching {
        val finalEncoded = encoder.getFirstEncode(userAccount, userPassword, sessData)
        apiServices.firstPostUser(
            fields = mutableMapOf(
                "yncjLogon"    to "",
                "userAccount"  to userAccount,
                "userPassword" to userPassword,
                "RANDOMCODE"   to code,
                "encoded"      to finalEncoded
            )
        )
    }.getOrNull()

    private suspend fun secondePostUserInfo(
        userAccount: String,
        userPassword: String,
        code: String
    ): Response<String>? = runCatching {
        val finalEncoded = encoder.getSecondeEncode(userAccount, userPassword)
        apiServices.secondePostUser(
            fields = mutableMapOf(
                "yncjLogon"    to "",
                "userAccount"  to userAccount,
                "userPassword" to "",
                "RANDOMCODE"   to code,
                "encoded"      to finalEncoded
            )
        )
    }.getOrNull()

    // ----------------------------------------------------------------
    // 登出
    // ----------------------------------------------------------------

    suspend fun logout() {
        runCatching {
            apiServices.logoutSystem(tktime = System.currentTimeMillis())
        }
        // 无论网络请求是否成功，必须清理本地 Cookie
        cookieJar.clear()
    }
}