package com.ynufe.ui.wlan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynufe.data.repository.WlanRepository
import com.ynufe.data.room.wlan.UserWlanInfoEntity
import com.ynufe.utils.LoginResult
import com.ynufe.utils.WlanUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WlanViewModel @Inject constructor(
    private val wlanRepository: WlanRepository,
) : ViewModel() {

    // 定义一个 Channel 用于发送错误消息
    private val _errorEvents = Channel<String>()
    // 暴露给 UI 观察的 Flow
    val errorEvents = _errorEvents.receiveAsFlow()

    // ─────────────────────────────────────────────────────────────────
    // UI 状态：监听数据库 Flow，自动映射为三种 WlanUiState
    //   Loading → 初始值，数据库尚未返回第一帧
    //   Empty   → 数据库查询到空列表
    //   Success → 数据库有数据，携带账号列表
    //   Error   → Flow 中途抛出异常
    // ─────────────────────────────────────────────────────────────────

    val uiState: StateFlow<WlanUiState> = wlanRepository.getAllWlanInfo
        .map { list ->
            if (list.isEmpty()) WlanUiState.Empty
            else WlanUiState.Success(list)
        }
        .catch { e -> emit(WlanUiState.Error(e.message ?: "未知错误")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WlanUiState.Loading
        )

    // ─────────────────────────────────────────────────────────────────
    // 操作方法
    // ─────────────────────────────────────────────────────────────────

    // 登录：从 entity 取学号和密码，调用仓库的完整登录流程
    fun loginWlan(entity: UserWlanInfoEntity) = viewModelScope.launch {
        _errorEvents.send("登录请求已发送，请稍候...")
        when (val result = wlanRepository.login(entity.studentId, entity.password)) {
            is LoginResult.Success<*> -> {
                // 直接获取 Repository 处理好的文案并发送 Toast 事件
                val msg = (result.data as? String) ?: "操作成功"
                _errorEvents.send(msg)
            }
            is LoginResult.Error -> {
                _errorEvents.send(result.message)
            }
        }
    }

    // 注销：从 entity 取学号和密码，调用仓库的完整注销流程
    fun logoutWlan(entity: UserWlanInfoEntity) = viewModelScope.launch {
        // 直接从 Repository 获取处理后的结果
        when (val result = wlanRepository.logout(entity.studentId, entity.ip)) {
            is LoginResult.Success<*> -> {
                // 获取 Repository 返回的 "注销成功" 文案
                val msg = (result.data as? String) ?: "注销成功"
                _errorEvents.send(msg)
            }
            is LoginResult.Error -> {
                // 获取具体的错误信息
                _errorEvents.send(result.message)
            }
        }
    }

    fun decryptPassword(encryptedPassword: String): String {
        return try {
            wlanRepository.decryptPassword(encryptedPassword)
        } catch (e: Exception) {
            "" // 或者返回原字符串
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 账号操作
    // ─────────────────────────────────────────────────────────────────


    // 添加新账号
    fun addAccount(studentId: String, password: String, location: String) = viewModelScope.launch {
        wlanRepository.addAccountOnly(studentId, password, location)
    }

    // 更新账号
    fun updateAccount(studentId: String, password: String, location: String) = viewModelScope.launch {
        // 将明文密码加密
        val encryptedPassword = wlanRepository.encryptPassword(password)

        wlanRepository.addAccountOnly(studentId, encryptedPassword, location)
    }

    // 设置为主要
    fun setPrimaryAccount(studentId: String) = viewModelScope.launch {
        // 先清除所有账号的激活状态
        wlanRepository.deactivateAllUsers()
        // 激活当前选择的账号
        wlanRepository.activateUser(studentId)
        // 发送提示
        _errorEvents.send("已将 $studentId 设为主要账号")
    }

    // 删除账号：从数据库移除对应学号的记录
    fun deleteAccount(studentId: String) = viewModelScope.launch {
        wlanRepository.deleteByStudentId(studentId)
    }

    // 删除所有账号：从数据库移除对应学号的记录
    fun deleteAllStudent() = viewModelScope.launch {
        wlanRepository.deleteAllStudent()
    }
}