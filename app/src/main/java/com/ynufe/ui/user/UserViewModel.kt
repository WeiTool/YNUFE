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

    // ── 对话框专用：验证码图片 ──────────────────────────────
    private val _verifyCodeImage = mutableStateOf<ByteArray?>(null)
    val verifyCodeImage: State<ByteArray?> = _verifyCodeImage

    // ── 对话框专用：所有已保存账号列表 ───────────────────────
    val allUsers: StateFlow<List<UserEntity>> = userRepository.getAllUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // ── 同步操作错误信息（null 表示无错误）──────────────────
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    // ── 内部状态（不对外暴露）────────────────────────────────

    /** 操作加载（同步课表 / 成绩 / 切换账号）标志 */
    private val _isOperationLoading = MutableStateFlow(false)

    /**
     * 数据库首次查询状态。
     * 使用 Pair<Boolean, UserEntity?> 区分「尚未返回 (false, null)」
     * 与「已返回但无账号 (true, null)」，避免切换 Tab 时出现闪屏。
     *
     * Eagerly：上游永不停止，StateFlow 始终持有最新值，
     * 切回 Tab 时 collectAsState() 立刻获得当前状态，不会重置回 Pair(false, null)。
     */
    private val _dbState = userRepository.getIsActiveUser
        .map { Pair(true, it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Pair(false, null)
        )

    /**
     * 用 flatMapLatest 把「当前激活用户」与「该用户的 userInfo」串联。
     * Eagerly 保证切 Tab 后不会因上游停止而在回来时重新发出 null。
     */
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

    // ── 对外暴露：统一 UI 状态 ────────────────────────────────

    /**
     * 驱动整个用户页面显示的单一状态流：
     * - [UserUiState.Initializing] → 数据库尚未就绪，显示骨架屏
     * - [UserUiState.Empty]        → 无绑定账号，显示引导页
     * - [UserUiState.Success]      → 显示用户信息
     *
     * Eagerly：切 Tab 后 StateFlow 不重置，回来时直接恢复 Success，
     * 彻底消除重新进入页面时的骨架屏闪烁。
     */
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

    // ── 业务操作 ──────────────────────────────────────────────

    fun startLoginFlow() {
        viewModelScope.launch {
            _syncError.value = null
            _verifyCodeImage.value = null
            _verifyCodeImage.value = userRepository.prepareLogin()
        }
    }

    fun clearSyncError() {
        _syncError.value = null
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
                    password // 如果解密失败（比如旧版本明文数据），则尝试原始密码
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
                    password // 如果解密失败（比如旧版本明文数据），则尝试原始密码
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

    fun decryptPassword(encrypted: String): String {
        return try {
            cryptoManager.decrypt(encrypted)
        } catch (e: Exception) {
            ""
        }
    }

    fun saveUserAccount(studentId: String, password: String) {
        viewModelScope.launch {
            // 这里的 password 是 UI 传来的明文
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
}