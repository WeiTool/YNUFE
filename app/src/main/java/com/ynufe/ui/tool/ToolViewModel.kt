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

    // 1. 直接获取校园网激活用户的完整实体信息
    // 这样 UI 只需要订阅这个 state 就能拿到所有字段（ip, onlineUser, location等）
    val activeWlanInfo: StateFlow<UserWlanInfoEntity?> = wlanRepository.getActiveFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 2. 教务处学号（用于查询成绩）
    val activeGradeStudentId: StateFlow<String?> = userRepository.getIsActiveUserStudentId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 3. 联动查询成绩
    @OptIn(ExperimentalCoroutinesApi::class)
    val randomGrades: StateFlow<List<GradeEntity>> = activeGradeStudentId
        .flatMapLatest { id ->
            if (id != null) gradeRepository.getGradesByStudentId(id)
            else flowOf(emptyList())
        }
        .map { it.shuffled().take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}