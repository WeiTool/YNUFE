package com.ynufe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynufe.data.repository.UserRepository
import com.ynufe.data.room.user.UserDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    /**
     * 最短展示计时器：保证开屏页至少展示 800ms，避免一闪而过。
     * 在 init 块里启动，与数据库查询并行执行，互不阻塞。
     */
    private val _minDelayDone = MutableStateFlow(false)

    /**
     * 数据库首次就绪标志。
     *
     * 使用 .map { true } + .onStart { emit(false) } 的组合：
     * - onStart 立刻发出 false，确保 combine 可以立即开始运算
     * - 数据库返回第一条结果（无论 null 还是实体）后，变为 true
     *
     * 这样无论用户是新用户（空库）还是老用户，只要 Room 查询
     * 返回过一次，就视为"数据已就绪"，不会因为空结果卡在加载态。
     */
    private val _dbReady: StateFlow<Boolean> = userRepository.getIsActiveUser
        .map { true }           // 任意结果（包括 null）都代表 DB 已响应
        .onStart { emit(false) } // 初始发出 false，保证 combine 不等待第一次 emit
        .stateIn(
            scope = viewModelScope,
            // ─────────────────────────────────────────────────────
            // 关键修复：改为 Eagerly
            //
            // WhileSubscribed 只在有"真正订阅者"时才启动上游收集。
            // 但 splashScreen.setKeepOnScreenCondition { !isReady.value }
            // 只是读取 .value，并不是 Flow 订阅，所以上游永远不会启动，
            // isReady 永远是 initialValue = false → 开屏卡死。
            //
            // Eagerly 让 Flow 在 ViewModel 创建时立刻开始收集，
            // .value 读到的永远是最新值，彻底解决卡死问题。
            // ─────────────────────────────────────────────────────
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    /**
     * 驱动开屏页的单一就绪状态：
     * - false → 继续显示系统开屏页（SplashScreen）
     * - true  → 放行，进入 App 主界面
     *
     * 两个条件必须同时满足：
     * 1. [_dbReady]      数据库已完成首次查询（用户/课程数据加载完毕）
     * 2. [_minDelayDone] 最短展示时间已到（避免开屏一闪而过）
     */
    val isReady: StateFlow<Boolean> = combine(
        _dbReady,
        _minDelayDone
    ) { dbReady, delayDone ->
        dbReady && delayDone
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly, // 同上，必须 Eagerly
        initialValue = false
    )

    init {
        // 与数据库查询并行：计时器到期后置位，不阻塞任何 DB 操作
        viewModelScope.launch {
            delay(800) // 最短开屏展示时间，可按需调整
            _minDelayDone.value = true
        }
    }
}