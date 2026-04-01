package com.ynufe.ui.grade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynufe.data.repository.GradeRepository
import com.ynufe.data.repository.UserRepository
import com.ynufe.utils.GradeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GradeViewModel @Inject constructor(
    private val gradeRepository: GradeRepository,
    userRepository: UserRepository
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────
    // 过滤 / 搜索状态：由 UI 驱动，作为 uiState 的输入源之一
    //   searchQuery  → 课程名称关键词，支持全半角空格忽略
    //   filterStatus → 全部 / 及格 / 不及格，对应 GradeFilter 枚举
    // ─────────────────────────────────────────────────────────────────

    val searchQuery = MutableStateFlow("")
    val filterStatus = MutableStateFlow(GradeFilter.ALL)

    // ─────────────────────────────────────────────────────────────────
    // 主 UI 状态：合并激活用户、成绩数据、搜索词、过滤条件四路流
    //   Loading → 数据库初始化中，显示骨架屏
    //   Empty   → 暂无成绩记录（未登录或尚未同步）
    //   Success → 成绩列表加载成功，已完成搜索 + 过滤 + 排序
    //   Error   → 上游数据流抛出异常
    //
    //   使用 Eagerly 而非 WhileSubscribed：
    //   - WhileSubscribed 下切 Tab → 订阅者归零 → 5s 后上游停止
    //     → 回到 Tab 时上游重启 → 先发出 Loading → 骨架屏闪烁
    //   - Eagerly 下上游在 ViewModel 创建时立刻启动且永不停止，
    //     StateFlow 始终持有最新值，切回 Tab 立即恢复 Success，无闪烁
    // ─────────────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GradeUiState> = userRepository.getIsActiveUser
        .flatMapLatest { currentUser ->
            val studentId = currentUser?.studentId ?: ""
            if (studentId.isEmpty()) return@flatMapLatest flowOf(GradeUiState.Empty)

            combine(
                gradeRepository.getGradesByStudentId(studentId),
                searchQuery,
                filterStatus
            ) { grades, query, filter ->
                if (grades.isEmpty()) return@combine GradeUiState.Empty

                val filteredList = grades.asSequence()
                    .filter { grade ->
                        if (query.isBlank()) return@filter true
                        val cleanQuery = query.replace("[\\s　]".toRegex(), "")
                        grade.courseName
                            .replace("[\\s　]".toRegex(), "")
                            .contains(cleanQuery, ignoreCase = true)
                    }
                    .filter { grade ->
                        val numScore = grade.score.toDoubleOrNull() ?: 0.0
                        when (filter) {
                            GradeFilter.ALL    -> true
                            GradeFilter.PASSED -> numScore >= 60.0
                            GradeFilter.FAILED -> numScore < 60.0
                        }
                    }
                    .toList()

                GradeUiState.Success(filteredList.sortedByDescending { it.term })
            }
        }
        .catch { e -> emit(GradeUiState.Error(e.message ?: "数据加载异常")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GradeUiState.Loading
        )

    // ─────────────────────────────────────────────────────────────────
    // 操作方法：更新搜索词 / 切换过滤条件（由 UI 事件驱动）
    // ─────────────────────────────────────────────────────────────────

    fun onSearchQueryChange(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onFilterChange(newFilter: GradeFilter) {
        filterStatus.value = newFilter
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 枚举：成绩过滤维度
//   ALL    → 显示全部成绩
//   PASSED → 只显示及格（>= 60）
//   FAILED → 只显示不及格（< 60）
// ─────────────────────────────────────────────────────────────────────────────

enum class GradeFilter {
    ALL, PASSED, FAILED
}