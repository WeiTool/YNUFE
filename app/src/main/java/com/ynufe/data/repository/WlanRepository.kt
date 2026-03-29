package com.ynufe.data.repository

import com.google.gson.Gson
import com.ynufe.data.api.ApiServices
import com.ynufe.data.model.TokenResponse
import com.ynufe.data.model.WlanUserInfoResponse
import com.ynufe.data.room.wlan.UserWlanInfoDao
import com.ynufe.data.room.wlan.UserWlanInfoEntity
import com.ynufe.utils.CryptoManager
import com.ynufe.utils.LoginResult
import com.ynufe.utils.wlan.Base64
import com.ynufe.utils.wlan.TEA
import com.ynufe.utils.wlan.hmacMd5
import com.ynufe.utils.wlan.sha1
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class WlanRepository @Inject constructor(
    private val apiServices: ApiServices,
    private val wlanUserDao: UserWlanInfoDao,
    private val cryptoManager: CryptoManager,
    private val gson: Gson,
    private val tea: TEA
) {
    private val acid = 7
    private val n = 200
    private val enc = "srun_bx1"
    private val type = 1

    /**
     * 改进后的日志记录：增加时间戳，方便在 UI 上区分多次尝试
     */
    private suspend fun recordErrorLog(studentId: String, msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formattedMsg = "[$time] $msg"

        val currentLog = wlanUserDao.getLogById(studentId) ?: ""
        val newLog = if (currentLog.isBlank()) formattedMsg else "$currentLog\n$formattedMsg"
        wlanUserDao.updateLog(studentId, newLog)
    }

    suspend fun addAccountOnly(studentId: String, password: String, location: String) {
        val encryptedPassword = try {
            cryptoManager.encrypt(password)
        } catch (e: Exception) {
            password
        }

        val initialEntity = UserWlanInfoEntity(
            studentId = studentId,
            password = encryptedPassword,
            location = location,
            ip = "0.0.0.0",
            onlineUser = "0",
            error = "未登录",
            errorMsg = "等待手动登录",
            log = ""
        )
        wlanUserDao.insertUserInfo(initialEntity)
    }

    suspend fun getToken(username: String, time: String): LoginResult {
        return try {
            val response = apiServices.getToken(username, time)

            if (response.isSuccessful) {
                val tokenData = response.body()
                if (tokenData != null && tokenData.challenge.isNotEmpty()) {
                    LoginResult.Success(tokenData)
                } else {
                    LoginResult.Error("Token 字段解析为空或格式错误")
                }
            } else {
                LoginResult.Error("HTTP错误: ${response.code()}")
            }
        } catch (e: Exception) {
            LoginResult.Error("连接异常: ${e.localizedMessage}")
        }
    }

    /**
     * 完整的登录流程：包含解密、获取Token、加密请求、以及延迟状态同步
     */
    suspend fun login(studentId: String, encryptedPasswordFromDb: String): LoginResult {
        // 每次点击登录先清空旧状态
        wlanUserDao.clearAllStatus(studentId)

        // 解密阶段
        val plainPassword = try {
            cryptoManager.decrypt(encryptedPasswordFromDb)
        } catch (e: Exception) {
            val errMsg = "[解密异常] 无法解析存储的密码: ${e.localizedMessage}"
            recordErrorLog(studentId, errMsg)
            return LoginResult.Error(errMsg)
        }

        val savedLocation = wlanUserDao.getLocationById(studentId) ?: ""
        val usernameWithTag = studentId + savedLocation
        val currentTime = System.currentTimeMillis().toString()

        // 获取 Token 阶段
        val tokenResult = getToken(usernameWithTag, currentTime)
        if (tokenResult is LoginResult.Error) {
            val errMsg = "[Token阶段] ${tokenResult.message}"
            recordErrorLog(studentId, errMsg)
            return tokenResult
        }

        val data = (tokenResult as? LoginResult.Success<*>)?.data as? TokenResponse
            ?: return LoginResult.Error("[Token阶段] 解析响应体失败")

        val token = data.challenge
        val onlineIp = data.onlineIp

        return try {
            // 加密与参数构造阶段
            val hmd5 = plainPassword.hmacMd5(token)
            val infoMap = mapOf(
                "username" to usernameWithTag,
                "password" to plainPassword,
                "ip" to onlineIp,
                "acid" to acid,
                "enc_ver" to enc
            )
            val info = "{SRBX1}" + Base64.encode(tea.encode(gson.toJson(infoMap), token))
            val checksum = buildString {
                append(token).append(usernameWithTag).append(token).append(hmd5)
                append(token).append(acid).append(token).append(onlineIp)
                append(token).append(n).append(token).append(type).append(token).append(info)
            }.sha1()

            // 发起登录请求阶段
            val response = apiServices.loginWlan(
                username = usernameWithTag,
                password = "{MD5}$hmd5",
                chksum = checksum,
                info = info,
                ip = onlineIp,
                time = currentTime
            )

            if (response.isSuccessful) {
                val loginBody = response.body()
                if (loginBody != null) {
                    // 根据业务响应码判断
                    when (loginBody.sucMsg) {
                        "login_ok", "ip_already_login", "ip_already_online_error" -> {

                            // --- 状态同步重试逻辑 ---
                            var infoBody: WlanUserInfoResponse? = null
                            // 尝试 3 次同步，每次间隔 1 秒，解决服务器更新不及时的问题
                            repeat(3) { attempt ->
                                delay(1000L * (attempt + 1))
                                val infoResponse = apiServices.wlanUserInfo(System.currentTimeMillis().toString())
                                if (infoResponse.isSuccessful && infoResponse.body() != null) {
                                    infoBody = infoResponse.body()
                                    return@repeat // 成功取到数据，跳出循环
                                }
                            }

                            if (infoBody != null) {
                                val entity = UserWlanInfoEntity(
                                    studentId = studentId,
                                    password = encryptedPasswordFromDb,
                                    location = savedLocation,
                                    ip = infoBody.onlineIp,
                                    onlineUser = infoBody.onlineUser ?: "",
                                    error = loginBody.error ?: "",
                                    errorMsg = loginBody.errorMsg ?: "",
                                    ployMsg = loginBody.ployMsg ?: "",
                                    sucMsg = loginBody.sucMsg,
                                    log = ""
                                )
                                wlanUserDao.insertUserInfo(entity)

                                // 根据实际业务返回给 UI 不同的 Toast 文本
                                val uiMsg = when (loginBody.sucMsg) {
                                    "login_ok" -> "登录成功"
                                    else -> "账号已在线"
                                }
                                LoginResult.Success(uiMsg)
                            } else {
                                val errMsg = "[数据同步阶段] 登录成功但同步详情超时，请手动刷新"
                                recordErrorLog(studentId, errMsg)
                                LoginResult.Error(errMsg)
                            }
                        }
                        else -> {
                            val errMsg = loginBody.errorMsg ?: "登录失败"
                            recordErrorLog(studentId, errMsg)
                            LoginResult.Error(errMsg)
                        }
                    }
                } else {
                    LoginResult.Error("[接口异常] 返回体为空")
                }
            } else {
                val errMsg = "[接口异常] 服务器响应错误码: ${response.code()}"
                recordErrorLog(studentId, errMsg)
                LoginResult.Error(errMsg)
            }
        } catch (e: Exception) {
            val errMsg = "[逻辑崩溃] 运行异常: ${e.localizedMessage}"
            recordErrorLog(studentId, errMsg)
            LoginResult.Error(errMsg)
        }
    }

    suspend fun logout(studentId: String, ip: String): LoginResult {
        return try {
            val savedLocation = wlanUserDao.getLocationById(studentId) ?: ""
            val fullUsername = studentId + savedLocation

            val response = apiServices.logoutWlan(
                username = fullUsername,
                ip = ip,
                time = System.currentTimeMillis().toString()
            )

            if (response.isSuccessful) {
                val logoutBody = response.body()
                if (logoutBody != null) {
                    // 统一判断逻辑
                    if (logoutBody.error == "ok") {
                        wlanUserDao.updateLogoutStatus(studentId, "ok", "已成功注销")
                        LoginResult.Success("注销成功")
                    } else {
                        val errMsg = logoutBody.errorMsg ?: "注销失败"
                        recordErrorLog(studentId, "[注销业务错误] $errMsg")
                        LoginResult.Error(errMsg)
                    }
                } else {
                    LoginResult.Error("注销接口返回为空")
                }
            } else {
                val errMsg = "HTTP ${response.code()}"
                recordErrorLog(studentId, "[注销接口错误] $errMsg")
                LoginResult.Error(errMsg)
            }
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: "未知异常"
            recordErrorLog(studentId, "[注销崩溃] $errMsg")
            LoginResult.Error("网络异常: $errMsg")
        }
    }

    // 提供给View

    val getAllWlanInfo = wlanUserDao.getAllWlanInfo() // 获取所有用户数据

    val getActiveFlow = wlanUserDao.getActiveFlow() // 获取活跃用户校园网信息

    suspend fun activateUser(studentId: String) = wlanUserDao.activateUser(studentId) // 激活指定用户

    suspend fun deactivateAllUsers() = wlanUserDao.deactivateAllUsers() // 清除所有激活用户

    suspend fun deleteByStudentId(studentId: String) =
        wlanUserDao.deleteByStudentId(studentId) // 删除指定用户信息

    suspend fun deleteAllStudent() = wlanUserDao.deleteAllStudent() // 删除所有账户

    fun decryptPassword(encrypted: String): String = cryptoManager.decrypt(encrypted) // 解密
    fun encryptPassword(password: String): String = cryptoManager.encrypt(password) // 加密
}