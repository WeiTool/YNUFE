package com.ynufe.ui.wlan

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ynufe.data.room.wlan.UserWlanInfoEntity
import com.ynufe.ui.theme.GradeLayout
import com.ynufe.utils.WlanUiState
import com.ynufe.utils.toHalfWidth

// ─────────────────────────────────────────────────────────────────
// 【Wlan】Dialog 类型枚举
//
//   NONE    → 没有任何 dialog 处于打开状态（默认值）
//   ADD     → 添加新 Wlan 账号 dialog
//             触发：右下角工具栏"添加账号"按钮 / Empty 状态"添加账号"按钮
//   EDIT    → 修改已有 Wlan 账号 dialog（与 ADD 共用同一 Dialog，自动区分模式）
//             触发：卡片学号行右侧"修改"图标按钮
//   LOG     → 查看该账号状态日志 dialog
//             触发：卡片内"日志"文字按钮
//   DELETE  → 确认删除该账号 dialog
//             触发：卡片内"移除"文字按钮
// ─────────────────────────────────────────────────────────────────

enum class WlanDialogType {
    NONE, ADD, EDIT, LOG, DELETE
}

// ─────────────────────────────────────────────────────────────────
// 【Wlan】入口：从 ViewModel 收集 uiState 并分发
//
//  onBack          → 返回上一页（ToolScreen）的回调，默认为空操作
//                    同时拦截系统返回键（BackHandler）
//  activeDialog    → 当前打开的是哪个 dialog（WlanDialogType 枚举）
//  selectedEntity  → 当前操作的完整账号实体
// ─────────────────────────────────────────────────────────────────

@Composable
fun WlanScreen(
    onBack: () -> Unit = {},                          // ← 新增：返回回调
    viewModel: WlanViewModel = hiltViewModel()
) {
    // 拦截系统返回键，与 GradeScreen 保持一致
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var activeDialog   by remember { mutableStateOf(WlanDialogType.NONE) }
    var selectedEntity by remember { mutableStateOf<UserWlanInfoEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is WlanUiState.Loading -> WlanSkeletonContent()
                is WlanUiState.Empty -> WlanEmptyContent(
                    onAddClick = { activeDialog = WlanDialogType.ADD }
                )
                is WlanUiState.Error -> WlanErrorContent(message = state.message)
                is WlanUiState.Success -> WlanListContent(
                    accounts = state.users,
                    onLoginClick = { entity -> viewModel.loginWlan(entity) },
                    onLogoutClick = { entity -> viewModel.logoutWlan(entity) },
                    onLogClick = { entity ->
                        selectedEntity = entity
                        activeDialog = WlanDialogType.LOG
                    },
                    onDeleteClick = { entity ->
                        selectedEntity = entity
                        activeDialog = WlanDialogType.DELETE
                    },
                    onEditClick = { entity ->
                        val plainPassword = viewModel.decryptPassword(entity.password)
                        selectedEntity = entity.copy(password = plainPassword)
                        activeDialog = WlanDialogType.EDIT
                    },
                    onSetPrimaryClick = { entity ->
                        viewModel.setPrimaryAccount(entity.studentId)
                    }
                )
            }
        }

        // ── 右下角常驻工具栏（上下排列，带文字标签） ─────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // 按钮 1：添加账号（主色调）
            ExtendedFloatingActionButton(
                onClick = { activeDialog = WlanDialogType.ADD },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("添加账号") }
            )

            // 按钮 2：删除所有账号（警告色）
            ExtendedFloatingActionButton(
                onClick = { viewModel.deleteAllStudent() },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error,
                icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                text = { Text("删除所有") }
            )
        }
    }

    // Dialog 控制逻辑保持不变
    if (activeDialog != WlanDialogType.NONE) {
        WlanActionDialog(
            type = activeDialog,
            selectedEntity = selectedEntity,
            onDismiss = {
                activeDialog = WlanDialogType.NONE
                selectedEntity = null
            },
            onConfirmAdd = { studentId, password, location ->
                viewModel.addAccount(studentId, password, location)
                activeDialog = WlanDialogType.NONE
            },
            onConfirmEdit = { studentId, password, location ->
                viewModel.updateAccount(studentId, password, location)
                activeDialog = WlanDialogType.NONE
            },
            onConfirmDelete = {
                selectedEntity?.let { viewModel.deleteAccount(it.studentId) }
                activeDialog = WlanDialogType.NONE
            }
        )
    }
}


// ─────────────────────────────────────────────────────────────────
// 【Wlan】Success 状态：LazyColumn 多卡片
//  移除了顶部"添加账号"按钮（已迁移至右下角工具栏）
//  新增 onEditClick 回调传给每张卡片
// ─────────────────────────────────────────────────────────────────

@Composable
fun WlanListContent(
    accounts: List<UserWlanInfoEntity>,
    onLoginClick: (UserWlanInfoEntity) -> Unit,
    onLogoutClick: (UserWlanInfoEntity) -> Unit,
    onLogClick: (UserWlanInfoEntity) -> Unit,
    onDeleteClick: (UserWlanInfoEntity) -> Unit,
    onEditClick: (UserWlanInfoEntity) -> Unit,
    onSetPrimaryClick: (UserWlanInfoEntity) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // 底部留出空间，避免卡片被右下角 FAB 遮挡
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        // ── 顶部标题行（已移除"添加账号"按钮）────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "校园网账号",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── 账号卡片列表 ────────────────────────────────────────────
        items(
            items = accounts,
            key = { it.studentId }
        ) { account ->
            UserWlanCard(
                info = account,
                onLoginClick = { onLoginClick(account) },
                onLogoutClick = { onLogoutClick(account) },
                onLogClick = { onLogClick(account) },
                onDeleteClick = { onDeleteClick(account) },
                onEditClick = { onEditClick(account) },
                onSetPrimaryClick = { onSetPrimaryClick(account) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 【Wlan】统一 Dialog 组件
//
//  type 决定渲染哪种内容：
//    ADD  / EDIT → 共用同一表单（学号 + 密码 + 区域）
//                  ADD：学号可编辑，密码为空
//                  EDIT：学号禁用（主键不可改），密码预填
//    LOG          → 展示 selectedEntity 的状态字段
//    DELETE       → 删除确认文本
//
//  表单校验（对齐 UserScreen 规则）：
//    - 学号为空 → isError = true，红色边框 + 错误提示
//    - 密码为空 → isError = true，红色边框 + 错误提示
//    - 未全部填写完成时保存按钮禁用（enabled = canSave）
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WlanActionDialog(
    type: WlanDialogType,
    selectedEntity: UserWlanInfoEntity?,
    onDismiss: () -> Unit,
    onConfirmAdd: (studentId: String, password: String, location: String) -> Unit,
    onConfirmEdit: (studentId: String, password: String, location: String) -> Unit,
    onConfirmDelete: () -> Unit,
) {
    // ── ADD/EDIT 共用表单状态 ─────────────────────────────────────
    // EDIT 模式：预填充学号和区域；密码出于安全原则预填（实体有密码字段则回显）
    var idText by remember(type, selectedEntity) {
        mutableStateOf(if (type == WlanDialogType.EDIT) selectedEntity?.studentId ?: "" else "")
    }
    var passwordText by remember(type, selectedEntity) {
        mutableStateOf(if (type == WlanDialogType.EDIT) selectedEntity?.password ?: "" else "")
    }
    var passwordVisible by remember { mutableStateOf(false) }

    // 区域选项：显示名 → 内部标识符
    val locationOptions = listOf("宿舍区域" to "@ctc", "教学区域" to "@ynufe")
    var selectedLocationTag by remember(type, selectedEntity) {
        mutableStateOf(
            if (type == WlanDialogType.EDIT) {
                val loc = selectedEntity?.location ?: ""
                when {
                    loc == "@ctc" || loc.contains("宿舍") || loc.contains("ctc") -> "@ctc"
                    loc == "@ynufe" || loc.contains("教学") || loc.contains("ynufe") -> "@ynufe"
                    else -> "@ctc"
                }
            } else "@ctc"
        )
    }

    // ── 表单校验（与 UserScreen 保持一致：isBlank() 即报错）────────
    val isIdError = idText.isBlank()
    val isPasswordError = passwordText.isBlank()
    val canSave = !isIdError && !isPasswordError

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
                        WlanDialogType.ADD    -> Icons.Default.PersonAdd
                        WlanDialogType.EDIT   -> Icons.Default.Edit
                        WlanDialogType.LOG    -> Icons.Default.NetworkCheck
                        WlanDialogType.DELETE -> Icons.Default.Delete
                        WlanDialogType.NONE   -> Icons.Default.Person
                    },
                    contentDescription = null,
                    tint = when (type) {
                        WlanDialogType.DELETE -> MaterialTheme.colorScheme.error
                        WlanDialogType.LOG    -> MaterialTheme.colorScheme.secondary
                        else                  -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = when (type) {
                        WlanDialogType.ADD    -> "添加校园网账号"
                        WlanDialogType.EDIT   -> "修改账号信息"
                        WlanDialogType.LOG    -> "${selectedEntity?.studentId ?: ""} 的日志"
                        WlanDialogType.DELETE -> "删除确认"
                        WlanDialogType.NONE   -> ""
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
                    // 【ADD / EDIT】共用表单
                    // ────────────────────────────────────────────────
                    WlanDialogType.ADD, WlanDialogType.EDIT -> {

                        // EDIT 模式下的提示信息
                        if (type == WlanDialogType.EDIT) {
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

                        // 学号输入框（EDIT 模式下禁用）
                        OutlinedTextField(
                            value = idText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) idText = it },
                            label = { Text("学号") },
                            singleLine = true,
                            enabled = type == WlanDialogType.ADD, // EDIT 时学号不可改
                            isError = isIdError && type == WlanDialogType.ADD, // EDIT时学号不为空无需报错
                            supportingText = {
                                if (isIdError && type == WlanDialogType.ADD) {
                                    Text("学号不能为空")
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.School,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isIdError && type == WlanDialogType.ADD)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 密码输入框（含显示/隐藏切换）
                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { passwordText = it.toHalfWidth() },
                            label = { Text("密码") },
                            singleLine = true,
                            isError = isPasswordError,
                            supportingText = {
                                if (isPasswordError) Text("密码不能为空")
                            },
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
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
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible)
                                            Icons.Default.Visibility
                                        else
                                            Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 区域单选
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "选择登录区域",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                locationOptions.forEach { (displayName, tag) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedLocationTag = tag }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = (selectedLocationTag == tag),
                                            onClick = { selectedLocationTag = tag }
                                        )
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ────────────────────────────────────────────────
                    // 【LOG dialog】账号状态字段展示（内容不变）
                    // ────────────────────────────────────────────────
                    WlanDialogType.LOG -> {
                        if (selectedEntity == null) {
                            Text("无数据", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            val isOk = selectedEntity.error == "ok" || selectedEntity.sucMsg.isNotBlank()

                            WlanLogRow(
                                icon = if (isOk) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                iconTint = if (isOk) Color(0xFF00A000) else MaterialTheme.colorScheme.error,
                                label = "状态码",
                                value = selectedEntity.error.ifBlank { "—" }
                            )
                            WlanLogRow(
                                icon = Icons.Default.CheckCircle,
                                iconTint = Color(0xFF00A000),
                                label = "成功信息",
                                value = selectedEntity.sucMsg
                            )
                            WlanLogRow(
                                icon = Icons.Default.ErrorOutline,
                                iconTint = MaterialTheme.colorScheme.error,
                                label = "错误信息",
                                value = selectedEntity.errorMsg
                            )
                            WlanLogRow(
                                icon = Icons.Default.NetworkCheck,
                                iconTint = MaterialTheme.colorScheme.secondary,
                                label = "策略信息",
                                value = selectedEntity.ployMsg
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "异常日志记录",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                if (selectedEntity.log.isNotBlank()) {
                                    Text(
                                        text = selectedEntity.log,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = TextUnit(16f, TextUnitType.Sp)
                                    )
                                } else {
                                    Text(
                                        text = "暂无日志记录",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // ────────────────────────────────────────────────
                    // 【DELETE dialog】删除确认
                    // ────────────────────────────────────────────────
                    WlanDialogType.DELETE -> {
                        Text(
                            text = "确定要删除学号为 ${selectedEntity?.studentId ?: "—"} 的账号吗？\n此操作不可撤销。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    WlanDialogType.NONE -> {}
                }
            }
        },

        // ── 确认 / 取消按钮 ───────────────────────────────────────
        confirmButton = {
            when (type) {
                WlanDialogType.ADD, WlanDialogType.EDIT -> {
                    TextButton(
                        onClick = {
                            if (type == WlanDialogType.ADD)
                                onConfirmAdd(idText.trim(), passwordText, selectedLocationTag)
                            else
                                onConfirmEdit(idText.trim(), passwordText, selectedLocationTag)
                        },
                        enabled = canSave
                    ) { Text(if (type == WlanDialogType.ADD) "添加" else "保存") }
                }
                WlanDialogType.DELETE -> {
                    TextButton(onClick = onConfirmDelete) {
                        Text("确认删除", color = MaterialTheme.colorScheme.error)
                    }
                }
                WlanDialogType.LOG -> {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                WlanDialogType.NONE -> {}
            }
        },
        dismissButton = {
            if (type != WlanDialogType.LOG) {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────
// WlanLogRow（辅助组件，未改动）
// ─────────────────────────────────────────────────────────────────

@Composable
fun WlanLogRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(value, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 【Wlan】Loading 状态：骨架屏（3 张占位卡片）
// ─────────────────────────────────────────────────────────────────

@Composable
fun WlanSkeletonContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 【Wlan】Empty 状态：暂无账号 + 添加按钮
// ─────────────────────────────────────────────────────────────────

@Composable
fun WlanEmptyContent(onAddClick: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.WifiOff,
                null,
                modifier = Modifier.size(64.dp),
                tint = Color.LightGray
            )
            Text(
                "暂未添加 Wlan 账号",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Surface(
                onClick = onAddClick,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                    Text("添加账号", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 【Wlan】Error 状态：加载失败提示
// ─────────────────────────────────────────────────────────────────

@Composable
fun WlanErrorContent(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                "加载失败",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 【Wlan】账号卡片组件
//
//  学号 Header 行右侧新增"修改"图标按钮（TooltipBox 提示文字）
//  onEditClick → 触发 WlanDialogType.EDIT
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserWlanCard(
    modifier: Modifier = Modifier,
    info: UserWlanInfoEntity,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLogClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onSetPrimaryClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                if (!info.isActive) onSetPrimaryClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = GradeLayout.CardElevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── 顶部 Header ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CellTower,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    info.studentId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // ── 激活状态：方形背景标签 ──
                if (info.isActive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "主要",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // ── 修改按钮：保留图标 + 文字描述 ──
                TextButton(
                    onClick = onEditClick,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "点击修改",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // ── 分割线 ──
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            // ── 下方内容区 ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧信息与引导
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WlanDetailRow(Icons.Default.Place, "区域", formatLocation(info.location))
                    WlanDetailRow(Icons.Default.Language, "IP 地址", info.ip)
                    WlanDetailRow(Icons.Default.People, "总在线数量", info.onlineUser)

                    Spacer(modifier = Modifier.height(4.dp))

                    // 功能按钮：日志 & 移除
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.clickable { onLogClick() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NetworkCheck, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("日志", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier = Modifier.clickable { onDeleteClick() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("移除", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    // ── 底部提醒文字 ──
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "提示：点击卡片区域可将该账号设为主要",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 右侧登录/注销大按钮
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.width(85.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D100)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("登录", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = onLogoutClick,
                        modifier = Modifier.width(85.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("注销", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun WlanDetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

fun formatLocation(location: String): String {
    return when (location) {
        "@ctc" -> "宿舍区域"
        "@ynufe" -> "教学区域"
        else -> location.ifBlank { "—" }
    }
}