package com.ynufe.data.repository

import com.ynufe.data.api.ApiServices
import com.ynufe.data.room.userInfo.UserDeleteDao
import com.ynufe.data.room.userInfo.UserInfoDao
import com.ynufe.utils.LoginResult
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val loginSystem: LoginSystem,
    private val apiServices: ApiServices,
    private val parser: ParseJsp,
    private val deleteDao: UserDeleteDao,
    userInfoDao: UserInfoDao,
) {
    // 登录准备（供 UI 获取验证码图片）
    suspend fun prepareLogin(): ByteArray? = loginSystem.prepareLogin()

    // 核心：登录 + 拉取用户信息
    suspend fun fetchUserInfo(
        userAccount: String,
        userPassword: String,
        code: String
    ): LoginResult {
        val loginResult = loginSystem.submitLogin(userAccount, userPassword, code)
        if (loginResult !is LoginResult.Success<*>) return loginResult

        val saved = fetchAndSaveUserInfo()
        return if (saved) {
            LoginResult.Success(Unit)
        } else {
            LoginResult.Error("登录成功，但用户信息获取失败，请重试")
        }
    }

    private suspend fun fetchAndSaveUserInfo(): Boolean {
        return try {
            val response = apiServices.getMainPage()
            if (response.isSuccessful) {
                val html = response.body() ?: ""
                if (html.isNotEmpty()) {
                    parser.parseStudentInfo(html)
                    true
                } else false
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logout() = loginSystem.logout()

    // 本地数据访问
    val userInfo = userInfoDao.getUserInfoFlow()
    suspend fun deleteUser(studentId: String) = deleteDao.deleteUser(studentId)
    suspend fun deleteAllUserInfo(studentId: String) = deleteDao.deleteAllUserInfo(studentId)
}