package com.ynufe.ui.user

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.ynufe.data.room.user.UserEntity
import com.ynufe.data.room.userInfo.UserInfoEntity
import com.ynufe.utils.UserUiState
import com.ynufe.utils.toHalfWidth

enum class UserDialogType {
    NONE, EDIT, GET, DELETE, CHOOSE
}

// ─────────────────────────────────────────────────────────────────
// 入口：从 ViewModel 收集 uiState 并向下传递
// ─────────────────────────────────────────────────────────────────

@Composable
fun UserScreen(viewModel: UserViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val verifyCodeImage by viewModel.verifyCodeImage
    val syncError by viewModel.syncError.collectAsState()

    UserScreenContent(
        uiState = uiState,
        allUsers = allUsers,
        verifyCodeImage = verifyCodeImage,
        syncError = syncError,
        onPrepareLogin = { viewModel.startLoginFlow() },
        onSave = { id, pass -> viewModel.saveUserAccount(id, pass) },
        onGetCourseClick = { code ->
            (uiState as? UserUiState.Success)?.user?.let {
                viewModel.getCourse(it.studentId, it.password, code)
            }
        },
        onGetGradesClick = { code ->
            (uiState as? UserUiState.Success)?.user?.let {
                viewModel.getGrade(it.studentId, it.password, code)
            }
        },
        onDeleteUser = { studentId -> viewModel.deleteCurrentUser(studentId) },
        onSwitchUser = { studentId -> viewModel.switchUser(studentId) },
        onClearSyncError = { viewModel.clearSyncError() },
    )
}

// ─────────────────────────────────────────────────────────────────
// 内容层：根据 UserUiState 分发到对应子 Composable
// ─────────────────────────────────────────────────────────────────

@Composable
fun UserScreenContent(
    uiState: UserUiState,
    allUsers: List<UserEntity>,
    verifyCodeImage: ByteArray?,
    syncError: String?,
    onPrepareLogin: () -> Unit,
    onSave: (String, String) -> Unit,
    onGetCourseClick: (String) -> Unit,
    onGetGradesClick: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onSwitchUser: (String) -> Unit,
    onClearSyncError: () -> Unit,
) {
    var activeDialog by remember { mutableStateOf(UserDialogType.NONE) }
    // 标记当前 GET 操作是获取课表还是成绩
    var isFetchingCourse by remember { mutableStateOf(true) }
    // 区分"添加新账号"与"编辑当前账号"：前者需要清空表单
    var isAddingNewUser by remember { mutableStateOf(false) }

    // 提取 isOperationLoading，用于传入对话框以实现登录成功自动关闭
    val isOperationLoading = (uiState as? UserUiState.Success)?.isOperationLoading ?: false

    Box(modifier = Modifier.fillMaxSize()) {
        // ── 主内容区 ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (uiState) {
                is UserUiState.Initializing -> UserSkeletonContent()
                is UserUiState.Empty -> EmptyStateView()
                is UserUiState.Success -> UserProfileContent(
                    uiState = uiState,
                    onEditClick = {
                        isAddingNewUser = false
                        activeDialog = UserDialogType.EDIT
                    },
                    onDeleteClick = { activeDialog = UserDialogType.DELETE },
                    onGetCourseClick = {
                        isFetchingCourse = true
                        onPrepareLogin()
                        activeDialog = UserDialogType.GET
                    },
                    onGetGradesClick = {
                        isFetchingCourse = false
                        onPrepareLogin()
                        activeDialog = UserDialogType.GET
                    }
                )
            }
        }

        // ── 悬浮工具栏 ────────────────────────────────────────
        ExpandableToolbar(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onChooseUser = { activeDialog = UserDialogType.CHOOSE },
            onAddUser = {
                isAddingNewUser = true
                activeDialog = UserDialogType.EDIT
            }
        )
    }

    // ── 对话框层 ──────────────────────────────────────────────
    if (activeDialog != UserDialogType.NONE) {
        val currentUser = (uiState as? UserUiState.Success)?.user ?: UserEntity("", "")
        // 添加新账号时传空 Entity，编辑时传真实账号
        val dialogUser = if (isAddingNewUser) UserEntity("", "") else currentUser
        UserActionDialog(
            type = activeDialog,
            user = dialogUser,
            allUsers = allUsers,
            verifyCodeImage = verifyCodeImage,
            syncError = syncError,
            isOperationLoading = isOperationLoading,
            onDismiss = {
                onClearSyncError()
                activeDialog = UserDialogType.NONE
                isAddingNewUser = false
            },
            onConfirmEdit = onSave,
            onConfirmSync = { code ->
                if (isFetchingCourse) onGetCourseClick(code) else onGetGradesClick(code)
            },
            onConfirmDelete = {
                (uiState as? UserUiState.Success)?.user?.let { onDeleteUser(it.studentId) }
            },
            onSwitchUser = { studentId ->
                onSwitchUser(studentId)
                activeDialog = UserDialogType.NONE
            },
            // 重试：自动刷新验证码（startLoginFlow 内部已清除 syncError）
            onRetry = onPrepareLogin,
            onRefreshCode = onPrepareLogin,
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// UserUiState.Initializing → 骨架屏占位内容
// ─────────────────────────────────────────────────────────────────

@Composable
fun UserSkeletonContent() {
    ProfileHeader(name = "", studentId = "", isLoading = true, onEdit = {}, onDelete = {})
    InfoSection(userInfo = null, isLoading = true)
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .shimmerLoading(true, RoundedCornerShape(16.dp))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .shimmerLoading(true, RoundedCornerShape(16.dp))
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// UserUiState.Success → 用户信息内容
// ─────────────────────────────────────────────────────────────────

@Composable
fun UserProfileContent(
    uiState: UserUiState.Success,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onGetCourseClick: () -> Unit,
    onGetGradesClick: () -> Unit,
) {
    val isLoading = uiState.isOperationLoading

    ProfileHeader(
        name = uiState.userInfo?.name ?: if (isLoading) "" else "未同步",
        studentId = uiState.user.studentId,
        isLoading = isLoading,
        onEdit = onEditClick,
        onDelete = onDeleteClick
    )
    InfoSection(userInfo = uiState.userInfo, isLoading = isLoading)
    ActionGrid(onGetCourse = onGetCourseClick, onGetGrades = onGetGradesClick)
    Spacer(modifier = Modifier.height(8.dp))
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("用户信息", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                Text(
                    "点击任意一个选项输入验证码可以获取用户信息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 对话框
// ─────────────────────────────────────────────────────────────────

@Composable
fun UserActionDialog(
    type: UserDialogType,
    user: UserEntity,
    allUsers: List<UserEntity>,
    verifyCodeImage: ByteArray?,
    syncError: String?,
    // 用于感知操作是否结束，实现 GET 对话框登录成功自动关闭 ──
    isOperationLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirmEdit: (String, String) -> Unit,
    onConfirmSync: (String) -> Unit,
    onConfirmDelete: () -> Unit,
    onSwitchUser: (String) -> Unit,
    onRetry: () -> Unit,
    onRefreshCode: () -> Unit,
) {
    // ── 状态初始化：利用 remember(key) 自动处理 type 切换时的重置 ──────────────────
    var idText by remember(type, user) {
        mutableStateOf(if (type == UserDialogType.EDIT) user.studentId else "")
    }
    var passwordText by remember(type, user) {
        mutableStateOf(if (type == UserDialogType.EDIT) user.password else "")
    }
    var codeText by remember(type) { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // GET 对话框是否处于错误视图
    val showError = type == UserDialogType.GET && syncError != null

    // ── 自动关闭逻辑 ────────────────────────────────────────────────────────────
    // 标记用户是否点击了登录，防止初始状态（loading=false, error=null）误触发关闭
    var pendingSync by remember(type) { mutableStateOf(false) }

    LaunchedEffect(isOperationLoading, syncError) {
        // 条件：用户点过登录 + 操作已结束 + 没有报错
        if (pendingSync && !isOperationLoading && syncError == null) {
            pendingSync = false
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (type) {
                    UserDialogType.EDIT -> "账号设置"
                    UserDialogType.GET -> if (showError) "同步失败" else "验证登录"
                    UserDialogType.DELETE -> "删除确认"
                    UserDialogType.CHOOSE -> "切换账号"
                    UserDialogType.NONE -> ""
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (type) {
                    // ── 账号设置 ────────────────────────────────
                    UserDialogType.EDIT -> {
                        OutlinedTextField(
                            value = idText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) idText = it },
                            label = { Text("学号") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { passwordText = it.toHalfWidth() },
                            label = { Text("密码") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = if (passwordVisible) "隐藏密码" else "显示密码")
                                }
                            }
                        )
                    }

                    // ── 验证登录（含错误重试视图）───────────────
                    UserDialogType.GET -> {
                        // AnimatedContent 在正常输入 ↔ 错误提示之间平滑切换
                        AnimatedContent(
                            targetState = showError,
                            transitionSpec = {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                            },
                            label = "GetDialogContent"
                        ) { isError ->
                            if (isError) {
                                // ── 错误视图 ──────────────────────
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = syncError ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                // ── 正常输入视图 ──────────────────
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    SubcomposeAsyncImage(
                                        model = verifyCodeImage,
                                        contentDescription = "验证码",
                                        modifier = Modifier
                                            .height(60.dp)
                                            .width(150.dp)
                                            .clickable { onRefreshCode() },
                                        loading = {
                                            Box(contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = codeText,
                                        onValueChange = { codeText = it },
                                        label = { Text("请输入验证码") },
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // ── 删除确认 ────────────────────────────────
                    UserDialogType.DELETE -> {
                        Text("确定要删除当前账号信息并退出登录吗？此操作不可撤销。")
                    }

                    // ── 切换账号 ────────────────────────────────
                    UserDialogType.CHOOSE -> {
                        if (allUsers.isEmpty()) {
                            Text(
                                "暂无已保存的账号",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                allUsers.forEachIndexed { index, u ->
                                    val isActive = u.isActive
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                else Color.Transparent
                                            )
                                            .clickable(enabled = !isActive) { onSwitchUser(u.studentId) }
                                            .padding(horizontal = 4.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isActive) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                null,
                                                tint = if (isActive) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = u.studentId,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isActive) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isActive) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                "当前账号",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    if (index < allUsers.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    UserDialogType.NONE -> {}
                }
            }
        },
        confirmButton = {
            when {
                // GET 错误视图：确认按钮变为「重试」，点击刷新验证码并返回输入视图
                showError -> TextButton(
                    onClick = {
                        codeText = ""
                        onRetry() // 内部会清除 syncError 并刷新验证码
                    }
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重试")
                }

                // 切换账号对话框无确认按钮
                type == UserDialogType.CHOOSE -> Unit

                // ── GET 正常输入视图：点击"登录"只触发请求，不关闭对话框 ──
                // 关闭逻辑由上方 LaunchedEffect 在操作成功后统一处理
                type == UserDialogType.GET -> TextButton(
                    onClick = {
                        if (codeText.isNotBlank()) {
                            pendingSync = true       // 标记已提交，等待结果
                            onConfirmSync(codeText)  // 触发异步请求
                            // 不调用 onDismiss()，让 LaunchedEffect 决定何时关闭
                        }
                    }
                ) {
                    Text("登录")
                }

                // 其他对话框（EDIT / DELETE）：确认后立即关闭
                else -> TextButton(
                    onClick = {
                        when (type) {
                            UserDialogType.EDIT -> if (idText.isNotBlank()) onConfirmEdit(idText, passwordText)
                            UserDialogType.DELETE -> onConfirmDelete()
                            else -> {}
                        }
                        onDismiss()
                    }
                ) {
                    Text(
                        when (type) {
                            UserDialogType.DELETE -> "确定"
                            else -> "保存"
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (type == UserDialogType.CHOOSE) "关闭" else "取消")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// 复用子组件
// ─────────────────────────────────────────────────────────────────

@Composable
fun ProfileHeader(
    name: String,
    studentId: String,
    isLoading: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .shimmerLoading(isLoading, CircleShape)
                    .background(if (isLoading) Color.Transparent else MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (!isLoading) {
                    Text(
                        text = name.take(1),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.shimmerLoading(isLoading).widthIn(min = 80.dp),
                    color = if (isLoading) Color.Transparent else Color.Unspecified
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = studentId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLoading) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.shimmerLoading(isLoading).widthIn(min = 120.dp)
                )
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun InfoSection(userInfo: UserInfoEntity?, isLoading: Boolean) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "基本信息",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Surface(
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoItem(Icons.Default.School, "专业", userInfo?.major ?: "未设置", isLoading)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                InfoItem(Icons.Default.AccountBalance, "学院", userInfo?.college ?: "未设置", isLoading)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                InfoItem(Icons.Default.Groups, "班级", userInfo?.className ?: "未设置", isLoading)
            }
        }
    }
}

@Composable
fun InfoItem(icon: ImageVector, label: String, value: String, isLoading: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.width(60.dp).shimmerLoading(isLoading)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isLoading) Color.Transparent else Color.Unspecified,
            modifier = Modifier.weight(1f).shimmerLoading(isLoading)
        )
    }
}

@Composable
fun ActionGrid(onGetCourse: () -> Unit, onGetGrades: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActionTile(
            modifier = Modifier.weight(1f),
            title = "获取课表",
            icon = Icons.Default.CalendarMonth,
            containerColor = MaterialTheme.colorScheme.primary,
            onClick = onGetCourse
        )
        ActionTile(
            modifier = Modifier.weight(1f),
            title = "获取成绩",
            icon = Icons.Default.AutoGraph,
            containerColor = MaterialTheme.colorScheme.secondary,
            onClick = onGetGrades
        )
    }
}

@Composable
fun ActionTile(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor.copy(alpha = 0.1f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = containerColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Medium, color = containerColor)
        }
    }
}

@Composable
fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("尚未绑定学生账号", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        }
    }
}

@Composable
fun ExpandableToolbar(
    modifier: Modifier = Modifier,
    onChooseUser: () -> Unit,
    onAddUser: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    Row(
        modifier = modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(springSpec) +
                    expandHorizontally(expandFrom = Alignment.End) +
                    slideInHorizontally(initialOffsetX = { it / 2 }),
            exit = fadeOut() +
                    shrinkHorizontally(shrinkTowards = Alignment.End) +
                    slideOutHorizontally(targetOffsetX = { it / 2 })
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarItem(icon = Icons.Default.PersonAdd, label = "添加账号") { onAddUser(); expanded = false }
                ToolbarItem(icon = Icons.Default.People, label = "切换账号") { onChooseUser(); expanded = false }
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = "工具箱",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ToolbarItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
        }
    }
}

fun Modifier.shimmerLoading(
    isLoading: Boolean,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
): Modifier = composed {
    if (!isLoading) return@composed this
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
    this.clip(shape).background(brush)
}