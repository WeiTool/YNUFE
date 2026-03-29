package com.ynufe.data.repository

import com.ynufe.data.api.AppApi
import com.ynufe.data.room.user.UserDao
import com.ynufe.data.room.userInfo.UserDeleteDao
import com.ynufe.utils.LoginResult
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val loginSystem: LoginSystem,
    private val appApi: AppApi,
    private val parser: ParseJsp,
    private val deleteDao: UserDeleteDao,
    private val userDao: UserDao,
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
            val response = appApi.getMainPage()
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

    // 提供给viewmodel
    val getIsActiveUser = userDao.getIsActiveUser() // 获取当前活动User

    val getIsActiveUserStudentId = userDao.getIsActiveUserStudentId() // 获取当前活动User学号

    val getAllUsers = userDao.getAllUsers() // 获取所有User

    suspend fun deactivateAllUsers() = userDao.deactivateAllUsers() // 将所有用户设为非激活

    suspend fun activateUser(studentId: String) = userDao.activateUser(studentId) // 激活指定用户

    suspend fun updateUserStartTime(id: String, time: Long) = userDao.updateUserStartTime(id, time) // 更新用户上课时间

    suspend fun deleteUser(studentId: String) = deleteDao.deleteUser(studentId) // 删除指定学号的用户信息
    suspend fun deleteAllUserInfo(studentId: String) = deleteDao.deleteAllUserInfo(studentId) // 删除指定学号的用户信息，包括课程表、成绩表和用户表
}