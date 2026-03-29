package com.ynufe.ui.user

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import com.ynufe.ui.theme.UserLayout
import com.ynufe.utils.UserUiState
import com.ynufe.utils.toHalfWidth

enum class UserDialogType {
    NONE, EDIT, GET, DELETE, CHOOSE
}

// ─────────────────────────────────────────────────────────────────
// 入口：从 ViewModel 收集 uiState 并分发
// ─────────────────────────────────────────────────────────────────

@Composable
fun UserScreen(viewModel: UserViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val verifyCodeImage by viewModel.verifyCodeImage
    val syncError by viewModel.syncError.collectAsState()

    var activeDialog by remember { mutableStateOf(UserDialogType.NONE) }
    var isFetchingCourse by remember { mutableStateOf(true) }
    var isAddingNewUser by remember { mutableStateOf(false) }

    // 用来传递给 Dialog 的实体（密码为明文，学号区分添加/修改）
    var selectedUserForDialog by remember { mutableStateOf<UserEntity?>(null) }

    val isOperationLoading = (uiState as? UserUiState.Success)?.isOperationLoading ?: false

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (val state = uiState) {
                is UserUiState.Initializing -> UserSkeletonContent()
                is UserUiState.Empty -> EmptyStateView()
                is UserUiState.Success -> UserProfileContent(
                    uiState = state,
                    onEditClick = {
                        // 1. 先解密当前用户的密码
                        val plain = viewModel.decryptPassword(state.user.password)
                        // 2. 创建临时实体，密码是明文
                        selectedUserForDialog = state.user.copy(password = plain)
                        isAddingNewUser = false
                        activeDialog = UserDialogType.EDIT
                    },
                    onDeleteClick = {
                        selectedUserForDialog = state.user
                        activeDialog = UserDialogType.DELETE
                    },
                    onGetCourseClick = {
                        isFetchingCourse = true
                        viewModel.startLoginFlow()
                        activeDialog = UserDialogType.GET
                    },
                    onGetGradesClick = {
                        isFetchingCourse = false
                        viewModel.startLoginFlow()
                        activeDialog = UserDialogType.GET
                    }
                )
            }
        }

        ExpandableToolbar(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            onChooseUser = { activeDialog = UserDialogType.CHOOSE },
            onAddUser = {
                // 1. 创建空实体（添加新用户）
                selectedUserForDialog = UserEntity("", "")
                isAddingNewUser = true
                activeDialog = UserDialogType.EDIT
            }
        )
    }

    if (activeDialog != UserDialogType.NONE) {
        // 使用临时的 plain password 实体，或者创建一个空的
        val dialogUser = selectedUserForDialog ?: UserEntity("", "")
        UserActionDialog(
            type = activeDialog,
            user = dialogUser, // 此时的 password 是明文
            allUsers = allUsers,
            verifyCodeImage = verifyCodeImage,
            syncError = syncError,
            isOperationLoading = isOperationLoading,
            onDismiss = {
                viewModel.clearSyncError()
                activeDialog = UserDialogType.NONE
                isAddingNewUser = false
                selectedUserForDialog = null
            },
            onConfirmEdit = { id, pass ->
                // ViewModel 执行重新加密保存
                viewModel.saveUserAccount(id, pass)
            },
            onConfirmSync = { code ->
                // 这里需要使用数据库中的原始加密密码
                val dbUser = (uiState as? UserUiState.Success)?.user
                dbUser?.let {
                    if (isFetchingCourse) viewModel.getCourse(it.studentId, it.password, code)
                    else viewModel.getGrade(it.studentId, it.password, code)
                }
            },
            onConfirmDelete = {
                (uiState as? UserUiState.Success)?.user?.let { viewModel.deleteCurrentUser(it.studentId) }
            },
            onSwitchUser = { studentId ->
                viewModel.switchUser(studentId)
                activeDialog = UserDialogType.NONE
            },
            onRetry = { viewModel.startLoginFlow() },
            onRefreshCode = { viewModel.startLoginFlow() },
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// UserUiState.Initializing → 骨架屏占位内容（不变）
// ─────────────────────────────────────────────────────────────────

@Composable
fun UserSkeletonContent() {
    ProfileHeader(name = "", studentId = "", isLoading = true, onEdit = {}, onDelete = {})
    InfoSection(userInfo = null, isLoading = true)
    Row(
        modifier = Modifier
            .padding(UserLayout.SkeletonTilePadding)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UserLayout.SkeletonTileSpacing)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(UserLayout.SkeletonTileHeight)
                .clip(RoundedCornerShape(UserLayout.SkeletonTileCorner))
                .shimmerLoading(true, RoundedCornerShape(UserLayout.SkeletonTileCorner))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(UserLayout.SkeletonTileHeight)
                .clip(RoundedCornerShape(UserLayout.SkeletonTileCorner))
                .shimmerLoading(true, RoundedCornerShape(UserLayout.SkeletonTileCorner))
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// UserUiState.Success → 用户信息内容（不变）
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
            .padding(UserLayout.HintCardMargin)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = UserLayout.HintCardBgAlpha),
        shape = RoundedCornerShape(UserLayout.HintCardCorner),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.tertiary.copy(alpha = UserLayout.HintCardBorderAlpha)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = UserLayout.HintCardPaddingH,
                vertical = UserLayout.HintCardPaddingV
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(UserLayout.HintCardIconSize)
            )
            Spacer(modifier = Modifier.width(UserLayout.HintCardIconToTextSpacing))
            Column {
                Text(
                    "用户信息",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
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
//   EDIT dialog 标题与表单输入框已按照 WlanScreen 规则全面美化
// ─────────────────────────────────────────────────────────────────

@Composable
fun UserActionDialog(
    type: UserDialogType,
    user: UserEntity,
    allUsers: List<UserEntity>,
    verifyCodeImage: ByteArray?,
    syncError: String?,
    isOperationLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirmEdit: (String, String) -> Unit,
    onConfirmSync: (String) -> Unit,
    onConfirmDelete: () -> Unit,
    onSwitchUser: (String) -> Unit,
    onRetry: () -> Unit,
    onRefreshCode: () -> Unit,
) {
    // ── EDIT/ADD 共用表单状态 ─────────────────────────────────────
    // 如果学号为空，代表是“添加新用户”；否则是“修改已有用户账号”
    var idText by remember(type, user) {
        mutableStateOf(if (type == UserDialogType.EDIT) user.studentId else "")
    }
    var passwordText by remember(type, user) {
        mutableStateOf(
            when {
                type == UserDialogType.EDIT && user.password.isNotBlank() -> user.password
                else -> "Aa!"
            }
        )
    }

    var codeText by remember(type) { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // ── 表单校验（Blank 即报错）─────────────────────────────────────
    val isIdError = idText.isBlank()
    val isPasswordError = passwordText.isBlank()
    val canSave = !isIdError && !isPasswordError

    val showError = type == UserDialogType.GET && syncError != null
    var pendingSync by remember(type) { mutableStateOf(false) }

    // 判断是否为添加新用户（user.studentId 为空代表新用户）
    val isAddingNewUser = user.studentId.isBlank()

    LaunchedEffect(isOperationLoading, syncError) {
        if (pendingSync && !isOperationLoading && syncError == null) {
            pendingSync = false
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        // ── Dialog 标题（带图标美化）──────────────────────────────
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (type) {
                        UserDialogType.EDIT   ->
                            if (isAddingNewUser) Icons.Default.PersonAdd else Icons.Default.Edit
                        UserDialogType.GET    ->
                            if (showError) Icons.Default.ErrorOutline else Icons.Default.Visibility
                        UserDialogType.DELETE -> Icons.Default.DeleteOutline
                        UserDialogType.CHOOSE -> Icons.Default.People
                        UserDialogType.NONE   -> Icons.Default.Person
                    },
                    contentDescription = null,
                    tint = when (type) {
                        UserDialogType.DELETE -> MaterialTheme.colorScheme.error
                        UserDialogType.GET    ->
                            if (showError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        else                  -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = when (type) {
                        UserDialogType.EDIT   ->
                            if (isAddingNewUser) "添加教务账号" else "修改账号设置"
                        UserDialogType.GET    ->
                            if (showError) "同步失败" else "验证登录"
                        UserDialogType.DELETE -> "删除确认"
                        UserDialogType.CHOOSE -> "切换账号"
                        UserDialogType.NONE   -> ""
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },

        // ── Dialog 正文 ───────────────────────────────────────────
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (type) {
                    // ────────────────────────────────────────────────
                    // EDIT：账号设置（添加 / 修改，对齐 Wlan 规则）
                    //   ADD  → 学号可编辑，密码空
                    //   EDIT → 学号禁用，密码预填 (明文)
                    //   表单校验：NotBlank 即报错
                    // ────────────────────────────────────────────────
                    UserDialogType.EDIT -> {

                        // EDIT 模式下的提示信息
                        if (!isAddingNewUser) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "学号为账号唯一标识，不可修改",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // 学号输入框：Person 图标 (ADD 时可编辑)
                        OutlinedTextField(
                            value = idText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) idText = it },
                            label = { Text("学号") },
                            singleLine = true,
                            enabled = isAddingNewUser, // EDIT 时学号不可改
                            isError = isIdError && isAddingNewUser, // EDIT时无需报错
                            supportingText = {
                                if (isIdError && isAddingNewUser) Text("学号不能为空")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.School,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isIdError && isAddingNewUser)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 密码输入框：Lock 图标 + 显隐切换
                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { passwordText = it.toHalfWidth() },
                            label = { Text("密码") },
                            singleLine = true,
                            isError = isPasswordError,
                            supportingText = {
                                if (isPasswordError) Text("密码不能为空")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isPasswordError)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            // 密码显隐切换（基于明文直接控制视觉转换）
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = image,
                                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ────────────────────────────────────────────────
                    // 验证登录（含错误重试视图，内容不变）
                    // ────────────────────────────────────────────────
                    UserDialogType.GET -> {
                        AnimatedContent(
                            targetState = showError,
                            transitionSpec = {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                            },
                            label = "GetDialogContent"
                        ) { isError ->
                            if (isError) {
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
                                        modifier = Modifier.size(UserLayout.ErrorIconSize),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = syncError ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    SubcomposeAsyncImage(
                                        model = verifyCodeImage,
                                        contentDescription = "验证码",
                                        modifier = Modifier
                                            .height(UserLayout.VerifyCodeImageHeight)
                                            .width(UserLayout.VerifyCodeImageWidth)
                                            .clickable { onRefreshCode() },
                                        loading = {
                                            Box(contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(UserLayout.LoadingIndicatorSize)
                                                )
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

                    // ────────────────────────────────────────────────
                    // 删除确认（内容不变）
                    // ────────────────────────────────────────────────
                    UserDialogType.DELETE -> {
                        Text(
                            text = "确定要删除账号 ${user.studentId} 吗? 此操作不可撤销。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // ────────────────────────────────────────────────
                    // 切换账号（内容不变）
                    // ────────────────────────────────────────────────
                    UserDialogType.CHOOSE -> {
                        if (allUsers.isEmpty()) {
                            Text("暂无已保存的教务账号", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                allUsers.forEachIndexed { index, u ->
                                    val isActive = u.isActive
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(UserLayout.ChooseUserItemCorner))
                                            .background(
                                                if (isActive)
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                else Color.Transparent
                                            )
                                            .clickable(enabled = !isActive) { onSwitchUser(u.studentId) }
                                            .padding(
                                                horizontal = UserLayout.ChooseUserItemPaddingH,
                                                vertical = UserLayout.ChooseUserItemPaddingV
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(UserLayout.ChooseUserAvatarSize)
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
                                                modifier = Modifier.size(UserLayout.ChooseUserAvatarIconSize)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(UserLayout.ChooseUserAvatarToIdSpacing))
                                        Text(
                                            text = u.studentId,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isActive) Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(UserLayout.ChooseUserCheckIconSize)
                                        )
                                        if (index < allUsers.lastIndex) HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            thickness = 0.5.dp
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

        // ── 按钮美化 ────────────────────────────────────────────────
        confirmButton = {
            when {
                showError -> TextButton(onClick = { codeText = ""; onRetry() }) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重试")
                }

                type == UserDialogType.CHOOSE -> Unit

                type == UserDialogType.GET -> TextButton(
                    onClick = {
                        if (codeText.isNotBlank()) {
                            pendingSync = true; onConfirmSync(codeText)
                        }
                    }
                ) { Text("登录同步") }

                type == UserDialogType.EDIT -> {
                    val defaultPassword = "Aa!"
                    val isPasswordInvalidForNewUser = isAddingNewUser && passwordText == defaultPassword
                    val canSaveNow = if (isAddingNewUser) {
                        !isIdError && passwordText.isNotBlank() && !isPasswordInvalidForNewUser
                    } else {
                        !isIdError && !isPasswordError
                    }
                    TextButton(
                        onClick = {
                            if (canSaveNow) {
                                onConfirmEdit(idText, passwordText)
                                onDismiss()
                            }
                        },
                        enabled = canSaveNow
                    ) { Text("保存") }
                }

                type == UserDialogType.DELETE -> TextButton(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }

                else -> TextButton(onClick = onDismiss) { Text("确定") }
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
// 复用子组件（以下内容均不变）
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
            .padding(UserLayout.ProfileHeaderPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(UserLayout.AvatarSize)
                    .clip(CircleShape)
                    .shimmerLoading(isLoading, CircleShape)
                    .background(
                        if (isLoading) Color.Transparent
                        else MaterialTheme.colorScheme.primary
                    ),
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
            Spacer(modifier = Modifier.width(UserLayout.AvatarToNameSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .shimmerLoading(isLoading)
                        .widthIn(min = 80.dp),
                    color = if (isLoading) Color.Transparent else Color.Unspecified
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = studentId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLoading) Color.Transparent
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .shimmerLoading(isLoading)
                        .widthIn(min = 120.dp)
                )
            }
            Column {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        "编辑",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun InfoSection(userInfo: UserInfoEntity?, isLoading: Boolean) {
    Column(modifier = Modifier.padding(UserLayout.InfoSectionPadding)) {
        Text(
            "基本信息",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Surface(
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(UserLayout.InfoSectionCorner),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(UserLayout.InfoSectionPadding)) {
                InfoItem(Icons.Default.School, "专业", userInfo?.major ?: "未设置", isLoading)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = UserLayout.InfoDividerPaddingV),
                    thickness = 0.5.dp
                )
                InfoItem(Icons.Default.AccountBalance, "学院", userInfo?.college ?: "未设置", isLoading)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = UserLayout.InfoDividerPaddingV),
                    thickness = 0.5.dp
                )
                InfoItem(Icons.Default.Groups, "班级", userInfo?.className ?: "未设置", isLoading)
            }
        }
    }
}

@Composable
fun InfoItem(icon: ImageVector, label: String, value: String, isLoading: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(UserLayout.InfoItemIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(UserLayout.InfoItemIconToTextSpacing))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier
                .width(60.dp)
                .shimmerLoading(isLoading)
        )
        Spacer(modifier = Modifier.width(UserLayout.InfoItemIconToTextSpacing))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isLoading) Color.Transparent else Color.Unspecified,
            modifier = Modifier
                .weight(1f)
                .shimmerLoading(isLoading)
        )
    }
}

@Composable
fun ActionGrid(onGetCourse: () -> Unit, onGetGrades: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(UserLayout.ActionGridPadding)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UserLayout.ActionTileSpacing)
    ) {
        ActionTile(
            modifier = Modifier.weight(1f),
            title = "同步课表",
            icon = Icons.Default.CalendarMonth,
            containerColor = MaterialTheme.colorScheme.primary,
            onClick = onGetCourse
        )
        ActionTile(
            modifier = Modifier.weight(1f),
            title = "同步成绩",
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
        modifier = modifier.height(UserLayout.ActionTileHeight),
        shape = RoundedCornerShape(UserLayout.ActionTileCorner),
        color = containerColor.copy(alpha = 0.1f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = containerColor)
            Spacer(modifier = Modifier.height(UserLayout.ActionTileIconToTextSpacing))
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = containerColor
            )
        }
    }
}

@Composable
fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.AccountCircle,
                null,
                modifier = Modifier.size(UserLayout.EmptyStateIconSize),
                tint = Color.LightGray
            )
            Spacer(modifier = Modifier.height(UserLayout.EmptyStateIconToTextSpacing))
            Text(
                "尚未绑定学生账号",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ExpandableToolbar(
    modifier: Modifier = Modifier,
    onChooseUser: () -> Unit,
    onAddUser: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // 使用 Row 布局，水平排列：[动画展开的内容] + [主 FAB]
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 展开时以动画滑入两个功能按钮
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() +
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
                // 按钮1：切换账号
                ToolbarItem(
                    icon = Icons.Default.People,
                    label = "切换账号",
                    onClick = { onChooseUser(); expanded = false }
                )
                // 按钮2：添加账号
                ToolbarItem(
                    icon = Icons.Default.PersonAdd,
                    label = "添加教务账号",
                    onClick = { onAddUser(); expanded = false }
                )
            }
        }

        // 2. 主 FAB：展开/收起控制按钮
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = "工具箱",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun ToolbarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
            )
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
    this
        .clip(shape)
        .background(brush)
}