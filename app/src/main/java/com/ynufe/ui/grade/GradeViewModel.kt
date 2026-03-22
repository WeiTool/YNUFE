package com.ynufe.ui.grade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynufe.data.repository.GradeRepository
import com.ynufe.data.room.user.UserDao
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
    userDao: UserDao
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val filterStatus = MutableStateFlow(GradeFilter.ALL)

    /**
     * 驱动整个成绩页面显示的单一状态流：
     * - [GradeUiState.Loading] → 数据库初始化中，显示骨架屏
     * - [GradeUiState.Empty]   → 暂无成绩记录
     * - [GradeUiState.Success] → 成绩列表加载成功
     * - [GradeUiState.Error]   → 加载出错
     *
     * 使用 Eagerly 而非 WhileSubscribed：
     * - WhileSubscribed 下切 Tab → 订阅者归零 → 5s 后上游停止
     *   → 回到 Tab 时上游重启 → 先发出 Loading → 骨架屏闪烁
     * - Eagerly 下上游在 ViewModel 创建时立刻启动且永不停止
     *   → StateFlow 始终持有最新值 → 切回 Tab 立即恢复 Success，无闪烁
     *
     * 同时移除了 .onStart { emit(Loading) }：
     * - Eagerly 已经在首次订阅前就完成了初始加载，initialValue 的 Loading
     *   只会在 App 启动的极短瞬间显示一次，之后不再重复触发。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GradeUiState> = userDao.getUser()
        .flatMapLatest { currentUser ->
            val studentId = currentUser?.studentId ?: ""
            if (studentId.isEmpty()) return@flatMapLatest flowOf(GradeUiState.Empty)

            combine(
                gradeRepository.getGradesByStudentId(studentId),
                searchQuery,
                filterStatus
            ) { grades, query, filter ->

                val filteredList = grades.asSequence()
                    .filter { grade ->
                        if (query.isBlank()) return@filter true
                        val cleanQuery = query.replace("[\\s　]".toRegex(), "")
                        grade.courseName.replace("[\\s　]".toRegex(), "")
                            .contains(cleanQuery, ignoreCase = true)
                    }
                    .filter { grade ->
                        val numScore = grade.score.toDoubleOrNull() ?: 0.0
                        when (filter) {
                            GradeFilter.ALL -> true
                            GradeFilter.PASSED -> numScore >= 60.0
                            GradeFilter.FAILED -> numScore < 60.0
                        }
                    }
                    .toList()

                if (filteredList.isEmpty()) {
                    GradeUiState.Empty
                } else {
                    GradeUiState.Success(filteredList.sortedByDescending { it.term })
                }
            }
        }
        .catch { e ->
            emit(GradeUiState.Error(e.message ?: "数据加载异常"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GradeUiState.Loading
        )

    fun onSearchQueryChange(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onFilterChange(newFilter: GradeFilter) {
        filterStatus.value = newFilter
    }
}

enum class GradeFilter {
    ALL,        // 显示全部（最高分）
    PASSED,     // 只显示及格 (>= 60)
    FAILED      // 只显示不及格 (< 60)
}