package com.ynufe.utils

import com.ynufe.data.room.course.CourseEntity
import com.ynufe.data.room.grade.GradeEntity
import com.ynufe.data.room.user.UserEntity
import com.ynufe.data.room.userInfo.UserInfoEntity

// ────────────────────────────────────────────────────────────
// 网络 / 仓库层结果
// ────────────────────────────────────────────────────────────

sealed class LoginResult {
    data class Success<T>(val data: T? = null) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

// ────────────────────────────────────────────────────────────
// 用户页 UI 状态
//
// Initializing → 数据库首次查询尚未完成，显示骨架屏
// Empty        → 无绑定账号，显示引导空状态
// Success      → 账号已加载；isOperationLoading 标识同步操作进行中
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
//
// Loading → 等待数据库首次返回
// NoUser  → 未绑定任何账号
// Empty   → 已绑定账号但尚未同步课表（附带 classBeginTime 供抽屉使用）
// Success → 课表加载成功
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
//
// Loading → 等待数据库首次返回
// Empty   → 暂无成绩记录
// Success → 成绩列表加载成功
// Error   → 加载出错
// ────────────────────────────────────────────────────────────

sealed class GradeUiState {
    data object Loading : GradeUiState()
    data object Empty : GradeUiState()
    data class Success(val grades: List<GradeEntity>) : GradeUiState()
    data class Error(val message: String) : GradeUiState()
}