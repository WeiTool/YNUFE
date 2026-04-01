package com.ynufe.ui.user

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynufe.data.repository.CourseRepository
import com.ynufe.data.repository.GradeRepository
import com.ynufe.data.repository.UserRepository
import com.ynufe.data.room.user.UserDao
import com.ynufe.data.room.user.UserEntity
import com.ynufe.data.room.userInfo.UserInfoDao
import com.ynufe.utils.CryptoManager
import com.ynufe.utils.LoginResult
import com.ynufe.utils.UserUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val gradeRepository: GradeRepository,
    private val userInfoDao: UserInfoDao,
    private val cryptoManager: CryptoManager,
    private val userDao: UserDao
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────
    // 对话框辅助状态：验证码图片（登录弹窗专用，不归入主 uiState）
    // ─────────────────────────────────────────────────────────────────

    private val _verifyCodeImage = mutableStateOf<ByteArray?>(null)
    val verifyCodeImage: State<ByteArray?> = _verifyCodeImage

    // ─────────────────────────────────────────────────────────────────
    // 账号列表：供账号切换弹窗使用
    //   Eagerly 保证 ViewModel 创建后立刻开始收集，弹窗打开时无需等待
    // ─────────────────────────────────────────────────────────────────

    val allUsers: StateFlow<List<UserEntity>> = userRepository.getAllUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // ─────────────────────────────────────────────────────────────────
    // 错误 / 加载状态：同步操作（同步课表 / 成绩 / 切换账号）的反馈
    //   _syncError   → null 表示无错误，非 null 则显示错误提示
    //   _isOperationLoading → 控制全屏 Loading 遮罩
    // ─────────────────────────────────────────────────────────────────

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _isOperationLoading = MutableStateFlow(false)

    // ─────────────────────────────────────────────────────────────────
    // 内部数据流（不对外暴露，仅用于组合成 uiState）
    //
    //   _dbState  → Pair<已就绪, 当前激活用户>
    //               区分「尚未返回 (false, null)」与「已返回但无账号 (true, null)」，
    //               避免切换 Tab 时出现骨架屏闪烁
    //
    //   _userInfo → 通过 flatMapLatest 与激活用户联动，
    //               用户切换时自动切换到对应的 userInfo 数据流
    //
    //   两者均使用 Eagerly：上游永不停止，StateFlow 始终持有最新值，
    //   切回 Tab 时 collectAsState() 立刻获得当前状态，不会重置回初始值
    // ─────────────────────────────────────────────────────────────────

    private val _dbState = userRepository.getIsActiveUser
        .map { Pair(true, it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Pair(false, null)
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _userInfo = userRepository.getIsActiveUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null)
            else userInfoDao.getUserInfoByStudentId(user.studentId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    // ─────────────────────────────────────────────────────────────────
    // 主 UI 状态：合并三路内部流，驱动整个用户页面的渲染
    //   Initializing → 数据库尚未就绪，显示骨架屏
    //   Empty        → 无绑定账号，显示引导页
    //   Success      → 正常显示用户信息及操作状态
    //
    //   Eagerly：切 Tab 后 StateFlow 不重置，回来时直接恢复 Success，
    //   彻底消除重新进入页面时的骨架屏闪烁
    // ─────────────────────────────────────────────────────────────────

    val uiState: StateFlow<UserUiState> = combine(
        _dbState,
        _userInfo,
        _isOperationLoading
    ) { (dbReady, user), userInfo, isOperationLoading ->
        when {
            !dbReady -> UserUiState.Initializing
            user == null -> UserUiState.Empty
            else -> UserUiState.Success(
                user = user,
                userInfo = userInfo,
                isOperationLoading = isOperationLoading
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserUiState.Initializing
    )

    // ─────────────────────────────────────────────────────────────────
    // 登录流程：预加载验证码 / 触发课表同步 / 触发成绩同步
    //   每次调用前重置 syncError 与 verifyCodeImage，避免残留旧状态
    // ─────────────────────────────────────────────────────────────────

    fun startLoginFlow() {
        viewModelScope.launch {
            _verifyCodeImage.value = null
            val image = userRepository.prepareLogin()

            // 获取到新图片后再清除错误，此时 UI 会从错误界面切换回验证码输入界面
            _syncError.value = null
            _verifyCodeImage.value = image
        }
    }

    fun getCourse(studentId: String, password: String, code: String) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            _verifyCodeImage.value = null
            try {
                userRepository.deleteUser(studentId)

                val plainPassword = try {
                    cryptoManager.decrypt(password)
                } catch (_: Exception) {
                    password // 解密失败时（旧版明文数据）使用原始值
                }

                val loginResult = userRepository.fetchUserInfo(studentId, plainPassword, code)
                if (loginResult is LoginResult.Error) {
                    _syncError.value = loginResult.message
                    return@launch
                }
                courseRepository.deleteCoursesByStudentId(studentId)
                val courseResult = courseRepository.getCourseTable(studentId)
                if (courseResult is LoginResult.Error) {
                    _syncError.value = courseResult.message
                }
            } finally {
                userRepository.logout()
                _isOperationLoading.value = false
            }
        }
    }

    fun getGrade(studentId: String, password: String, code: String) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            _verifyCodeImage.value = null
            try {
                userRepository.deleteUser(studentId)

                val plainPassword = try {
                    cryptoManager.decrypt(password)
                } catch (_: Exception) {
                    password // 解密失败时（旧版明文数据）使用原始值
                }

                val loginResult = userRepository.fetchUserInfo(studentId, plainPassword, code)
                if (loginResult is LoginResult.Error) {
                    _syncError.value = loginResult.message
                    return@launch
                }
                gradeRepository.deleteGradesByStudentId(studentId)
                val gradeResult = gradeRepository.getGradeTable(studentId)
                if (gradeResult is LoginResult.Error) {
                    _syncError.value = gradeResult.message
                }
            } finally {
                userRepository.logout()
                _isOperationLoading.value = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 账号管理：保存 / 切换 / 删除 / 解密密码
    // ─────────────────────────────────────────────────────────────────

    fun saveUserAccount(studentId: String, password: String) {
        viewModelScope.launch {
            val encryptedPassword = cryptoManager.encrypt(password)
            userRepository.deactivateAllUsers()
            userDao.insertUser(
                UserEntity(studentId = studentId, password = encryptedPassword, isActive = true)
            )
        }
    }

    fun switchUser(studentId: String) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            try {
                userRepository.deactivateAllUsers()
                userRepository.setActivateUser(studentId)
                delay(100)
            } finally {
                _isOperationLoading.value = false
            }
        }
    }

    fun deleteCurrentUser(studentId: String) {
        viewModelScope.launch {
            userRepository.deleteAllUserInfo(studentId)
        }
    }

    fun decryptPassword(encrypted: String): String {
        return try {
            cryptoManager.decrypt(encrypted)
        } catch (e: Exception) {
            ""
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 工具方法：重置错误状态（供 UI 在用户关闭弹窗后调用）
    // ─────────────────────────────────────────────────────────────────

    fun clearSyncError() {
        _syncError.value = null
    }
}