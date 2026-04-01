package com.ynufe.ui.tool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynufe.data.repository.GradeRepository
import com.ynufe.data.repository.UserRepository
import com.ynufe.data.repository.WlanRepository
import com.ynufe.data.room.grade.GradeEntity
import com.ynufe.data.room.wlan.UserWlanInfoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ToolViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val gradeRepository: GradeRepository,
    private val wlanRepository: WlanRepository
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────
    // 校园网状态：当前激活的校园网账号完整实体
    //   UI 直接订阅即可取到 ip、onlineUser、location 等所有字段，
    //   无需额外 map，减少冗余数据转换
    // ─────────────────────────────────────────────────────────────────

    val activeWlanInfo: StateFlow<UserWlanInfoEntity?> = wlanRepository.getActiveFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // ─────────────────────────────────────────────────────────────────
    // 教务处账号：当前激活用户的学号，用于下游成绩查询
    // ─────────────────────────────────────────────────────────────────

    val activeGradeStudentId: StateFlow<String?> = userRepository.getIsActiveUserStudentId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // ─────────────────────────────────────────────────────────────────
    // 随机成绩展示：与激活学号联动，随机抽取 5 条用于工具页卡片展示
    //   flatMapLatest 保证学号变更时立刻切换到对应的成绩数据流，
    //   旧流自动取消，不会出现数据错乱
    // ─────────────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    val randomGrades: StateFlow<List<GradeEntity>> = activeGradeStudentId
        .flatMapLatest { id ->
            if (id != null) gradeRepository.getGradesByStudentId(id)
            else flowOf(emptyList())
        }
        .map { it.shuffled().take(5) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}