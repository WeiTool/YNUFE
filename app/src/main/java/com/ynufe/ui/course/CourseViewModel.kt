package com.ynufe.ui.course

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynufe.data.room.course.CourseDao
import com.ynufe.data.room.user.UserDao
import com.ynufe.utils.CourseUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimeSlot(
    val section: Int,
    val start: String,
    val end: String
)

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseDao: CourseDao,
    private val userDao: UserDao,
) : ViewModel() {

    // ── 校区时间表（纯 UI 状态，无需持久化）──────────────────

    private val scheduleAnning = listOf(
        "08:00" to "08:40", "08:50" to "09:30", "10:00" to "10:40", "10:50" to "11:30",
        "11:40" to "12:20", "14:30" to "15:10", "15:20" to "16:00", "16:30" to "17:10",
        "17:20" to "18:00", "19:00" to "19:40", "19:50" to "20:30", "20:50" to "21:20", "21:30" to "22:20"
    ).mapIndexed { index, pair -> TimeSlot(index + 1, pair.first, pair.second) }

    private val scheduleLongquan = listOf(
        "08:20" to "09:00", "09:10" to "09:50", "10:10" to "10:50", "11:00" to "11:40",
        "11:50" to "12:30", "14:00" to "14:40", "14:50" to "15:30", "15:40" to "16:20",
        "16:40" to "17:20", "17:30" to "18:10", "19:00" to "19:40", "19:50" to "20:30", "20:40" to "21:20"
    ).mapIndexed { index, pair -> TimeSlot(index + 1, pair.first, pair.second) }

    var currentTimeSlots by mutableStateOf(scheduleLongquan)
        private set

    var currentScheduleName by mutableStateOf("安宁校区")
        private set

    fun toggleSchedule(isAnning: Boolean) {
        if (isAnning) {
            currentTimeSlots = scheduleLongquan
            currentScheduleName = "安宁校区"
        } else {
            currentTimeSlots = scheduleAnning
            currentScheduleName = "龙泉校区"
        }
    }

    // ── 学期开始日期（写入 DB，由 uiState 中的 semesterStartMs 观察）

    fun updateSemesterStart(newStartTime: Long) {
        viewModelScope.launch {
            val user = userDao.getUser().firstOrNull()
            user?.let { userDao.updateUserStartTime(it.studentId, newStartTime) }
        }
    }

    // ── 统一 UI 状态 ──────────────────────────────────────────

    /**
     * 驱动整个课程页面显示的单一状态流：
     * - [CourseUiState.Loading] → 等待数据库首次返回，显示骨架屏
     * - [CourseUiState.NoUser]  → 未绑定任何账号，显示引导空状态
     * - [CourseUiState.Empty]   → 已绑定账号但尚未同步课表
     * - [CourseUiState.Success] → 课表加载成功，正常渲染
     *
     * 使用 Eagerly 而非 WhileSubscribed：
     * - WhileSubscribed 下切 Tab → 订阅者归零 → 5s 后上游停止
     *   → 回到 Tab 时上游重启 → StateFlow 先回到 initialValue(Loading)
     *   → 骨架屏重新闪烁
     * - Eagerly 下上游在 ViewModel 创建时立刻启动且永不停止，
     *   StateFlow 始终持有最新值，切回 Tab 立即恢复 Success，无闪烁。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CourseUiState> = userDao.getUser()
        .flatMapLatest { user ->
            when {
                user == null -> flowOf(CourseUiState.NoUser)
                else -> courseDao.getCoursesByStudentId(user.studentId)
                    .map { courses ->
                        if (courses.isEmpty()) {
                            CourseUiState.Empty(classBeginTime = user.startTime)
                        } else {
                            CourseUiState.Success(
                                courses = courses,
                                classBeginTime = user.startTime
                            )
                        }
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CourseUiState.Loading
        )
}