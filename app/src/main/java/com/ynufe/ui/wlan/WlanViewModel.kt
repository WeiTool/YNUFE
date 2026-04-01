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

    // ─────────────────────────────────────────────────────────────────
    // 事件通道：用于向 UI 发送一次性 Toast / Snackbar 文案
    //   Channel 天然是一次性消费，不会因 UI 重组而重复触发
    // ─────────────────────────────────────────────────────────────────

    private val _errorEvents = Channel<String>()
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
    // 网络操作：登录 / 注销（调用仓库完整流程，结果通过事件通道通知 UI）
    // ─────────────────────────────────────────────────────────────────

    fun loginWlan(entity: UserWlanInfoEntity) = viewModelScope.launch {
        _errorEvents.send("登录请求已发送，请稍候...")
        when (val result = wlanRepository.login(entity.studentId, entity.password)) {
            is LoginResult.Success<*> -> {
                val msg = (result.data as? String) ?: "操作成功"
                _errorEvents.send(msg)
            }
            is LoginResult.Error -> {
                _errorEvents.send(result.message)
            }
        }
    }

    fun logoutWlan(entity: UserWlanInfoEntity) = viewModelScope.launch {
        when (val result = wlanRepository.logout(entity.studentId, entity.ip)) {
            is LoginResult.Success<*> -> {
                val msg = (result.data as? String) ?: "注销成功"
                _errorEvents.send(msg)
            }
            is LoginResult.Error -> {
                _errorEvents.send(result.message)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 加解密：对外暴露密码解密，屏蔽异常细节
    // ─────────────────────────────────────────────────────────────────

    fun decryptPassword(encryptedPassword: String): String {
        return try {
            wlanRepository.decryptPassword(encryptedPassword)
        } catch (e: Exception) {
            ""
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 账号管理：增 / 改 / 设主要 / 删单条 / 删全部
    // ─────────────────────────────────────────────────────────────────

    fun addAccount(studentId: String, password: String, location: String) = viewModelScope.launch {
        wlanRepository.addAccountOnly(studentId, password, location)
    }

    fun updateAccount(studentId: String, password: String, location: String) = viewModelScope.launch {
        val encryptedPassword = wlanRepository.encryptPassword(password)
        wlanRepository.updateAccountInfo(studentId, encryptedPassword, location)
    }

    fun setPrimaryAccount(studentId: String) = viewModelScope.launch {
        wlanRepository.deactivateAllUsers()
        wlanRepository.activateUser(studentId)
        _errorEvents.send("已将 $studentId 设为主要账号")
    }

    fun deleteAccount(studentId: String) = viewModelScope.launch {
        wlanRepository.deleteByStudentId(studentId)
    }

    fun deleteAllStudent() = viewModelScope.launch {
        wlanRepository.deleteAllStudent()
    }
}