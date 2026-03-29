package com.ynufe.utils

import com.ynufe.data.room.course.CourseEntity
import com.ynufe.data.room.grade.GradeEntity
import com.ynufe.data.room.user.UserEntity
import com.ynufe.data.room.userInfo.UserInfoEntity
import com.ynufe.data.room.wlan.UserWlanInfoEntity

// ────────────────────────────────────────────────────────────
// 网络 / 仓库层结果
// ────────────────────────────────────────────────────────────

sealed class LoginResult {
    data class Success<T>(val data: T? = null) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

sealed class UpdateResult {
    data class HasUpdate(
        val latestVersion: String,
        val currentVersion: String,
        val releaseNotes: String,
        val downloadUrl: String?
    ) : UpdateResult()

    object NoUpdate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

// ────────────────────────────────────────────────────────────
// 用户页 UI 状态
// ────────────────────────────────────────────────────────────

sealed class UserUiState {
    data object Initializing : UserUiState()
    data object Empty : UserUiState()
    data class Success(
        val user: UserEntity,
        val userInfo: UserInfoEntity?,
        val isOperationLoading: Boolean
    ) : UserUiState()
}

// ────────────────────────────────────────────────────────────
// 课程页 UI 状态
// ────────────────────────────────────────────────────────────

sealed class CourseUiState {
    data object Loading : CourseUiState()
    data object NoUser : CourseUiState()
    data class Empty(val classBeginTime: Long?) : CourseUiState()
    data class Success(
        val courses: List<CourseEntity>,
        val classBeginTime: Long?
    ) : CourseUiState()
}

// ────────────────────────────────────────────────────────────
// 成绩页 UI 状态
// ────────────────────────────────────────────────────────────

sealed class GradeUiState {
    data object Loading : GradeUiState()
    data object Empty : GradeUiState()
    data class Success(val grades: List<GradeEntity>) : GradeUiState()
    data class Error(val message: String) : GradeUiState()
}

// ────────────────────────────────────────────────────────────
// 校园网登陆 UI 状态
//
// Loading → 等待数据库首次返回
// Empty   → 暂无记录
// Success → 已有账号列表（每条附带可选的在线信息）
// Error   → 加载出错
// ────────────────────────────────────────────────────────────

sealed class WlanUiState {
    data object Loading : WlanUiState()
    data object Empty : WlanUiState()
    data class Success(val users: List<UserWlanInfoEntity>) : WlanUiState()
    data class Error(val message: String) : WlanUiState()
}