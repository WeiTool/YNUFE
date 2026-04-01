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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ynufe.data.room.wlan.UserWlanInfoEntity
import com.ynufe.ui.theme.type.GradeLayout
import com.ynufe.ui.theme.type.WlanLayout
import com.ynufe.utils.WlanUiState
import com.ynufe.utils.toHalfWidth

// ─────────────────────────────────────────────────────────────────
// 统一 Dialog 状态：类型 + 关联数据合为一体，消除分散的布尔值
// ─────────────────────────────────────────────────────────────────

sealed class WlanDialogState {
    object None   : WlanDialogState()
    object Add    : WlanDialogState()
    data class Edit(val entity: UserWlanInfoEntity)   : WlanDialogState()
    data class Log(val entity: UserWlanInfoEntity)    : WlanDialogState()
    data class Delete(val entity: UserWlanInfoEntity) : WlanDialogState()
}

@Composable
fun WlanScreen(
    onBack: () -> Unit = {},
    viewModel: WlanViewModel = hiltViewModel()
) {
    BackHandler { onBack() }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 单一状态变量替代原来的 activeDialog + selectedEntity 两个分散变量
    var dialogState by remember { mutableStateOf<WlanDialogState>(WlanDialogState.None) }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is WlanUiState.Loading -> WlanSkeletonContent()
                is WlanUiState.Empty   -> WlanEmptyContent(
                    onAddClick = { dialogState = WlanDialogState.Add }
                )
                is WlanUiState.Error   -> WlanErrorContent(message = state.message)
                is WlanUiState.Success -> WlanListContent(
                    accounts         = state.users,
                    onLoginClick     = { entity -> viewModel.loginWlan(entity) },
                    onLogoutClick    = { entity -> viewModel.logoutWlan(entity) },
                    onLogClick       = { entity -> dialogState = WlanDialogState.Log(entity) },
                    onDeleteClick    = { entity -> dialogState = WlanDialogState.Delete(entity) },
                    onEditClick      = { entity ->
                        val plainPassword = viewModel.decryptPassword(entity.password)
                        dialogState = WlanDialogState.Edit(entity.copy(password = plainPassword))
                    },
                    onSetPrimaryClick = { entity -> viewModel.setPrimaryAccount(entity.studentId) }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(WlanLayout.FabAreaPadding),
            verticalArrangement   = Arrangement.spacedBy(WlanLayout.FabSpacing),
            horizontalAlignment   = Alignment.End
        ) {
            ExtendedFloatingActionButton(
                onClick        = { dialogState = WlanDialogState.Add },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                icon           = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text           = { Text("添加账号") }
            )
            ExtendedFloatingActionButton(
                onClick        = { viewModel.deleteAllStudent() },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor   = MaterialTheme.colorScheme.error,
                icon           = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                text           = { Text("删除所有") }
            )
        }
    }

    if (dialogState != WlanDialogState.None) {
        WlanActionDialog(
            state         = dialogState,
            onDismiss     = { dialogState = WlanDialogState.None },
            onConfirmAdd  = { studentId, password, location ->
                viewModel.addAccount(studentId, password, location)
                dialogState = WlanDialogState.None
            },
            onConfirmEdit = { studentId, password, location ->
                viewModel.updateAccount(studentId, password, location)
                dialogState = WlanDialogState.None
            },
            onConfirmDelete = {
                (dialogState as? WlanDialogState.Delete)?.let {
                    viewModel.deleteAccount(it.entity.studentId)
                }
                dialogState = WlanDialogState.None
            }
        )
    }
}

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
        modifier      = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = WlanLayout.ListBottomPadding)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WlanLayout.ListTitlePaddingH, vertical = WlanLayout.ListTitlePaddingV),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "校园网账号",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(items = accounts, key = { it.studentId }) { account ->
            UserWlanCard(
                info             = account,
                onLoginClick     = { onLoginClick(account) },
                onLogoutClick    = { onLogoutClick(account) },
                onLogClick       = { onLogClick(account) },
                onDeleteClick    = { onDeleteClick(account) },
                onEditClick      = { onEditClick(account) },
                onSetPrimaryClick = { onSetPrimaryClick(account) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 统一 Dialog：接受 WlanDialogState，内部根据类型分发内容
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WlanActionDialog(
    state: WlanDialogState,
    onDismiss: () -> Unit,
    onConfirmAdd: (studentId: String, password: String, location: String) -> Unit,
    onConfirmEdit: (studentId: String, password: String, location: String) -> Unit,
    onConfirmDelete: () -> Unit,
) {
    // 从 state 中提取当前关联的实体（Add 无实体）
    val selectedEntity: UserWlanInfoEntity? = when (state) {
        is WlanDialogState.Edit   -> state.entity
        is WlanDialogState.Log    -> state.entity
        is WlanDialogState.Delete -> state.entity
        else                      -> null
    }

    val isEdit = state is WlanDialogState.Edit
    val isAdd  = state is WlanDialogState.Add

    var idText by remember(state) {
        mutableStateOf(if (isEdit) selectedEntity?.studentId ?: "" else "")
    }
    var passwordText by remember(state) {
        mutableStateOf(if (isEdit) selectedEntity?.password ?: "" else "")
    }
    var passwordVisible by remember { mutableStateOf(false) }

    val locationOptions = listOf("宿舍区域" to "@ctc", "教学区域" to "@ynufe")
    var selectedLocationTag by remember(state) {
        mutableStateOf(
            if (isEdit) {
                val loc = selectedEntity?.location ?: ""
                when {
                    loc == "@ctc"   || loc.contains("宿舍") || loc.contains("ctc")   -> "@ctc"
                    loc == "@ynufe" || loc.contains("教学") || loc.contains("ynufe") -> "@ynufe"
                    else -> "@ctc"
                }
            } else "@ctc"
        )
    }

    val isIdError       = idText.isBlank()
    val isPasswordError = passwordText.isBlank()
    val canSave         = !isIdError && !isPasswordError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WlanLayout.DialogTitleIconToTextSpacing)
            ) {
                Icon(
                    imageVector = when (state) {
                        is WlanDialogState.Add    -> Icons.Default.PersonAdd
                        is WlanDialogState.Edit   -> Icons.Default.Edit
                        is WlanDialogState.Log    -> Icons.Default.NetworkCheck
                        is WlanDialogState.Delete -> Icons.Default.Delete
                        is WlanDialogState.None   -> Icons.Default.Person
                    },
                    contentDescription = null,
                    tint = when (state) {
                        is WlanDialogState.Delete -> MaterialTheme.colorScheme.error
                        is WlanDialogState.Log    -> MaterialTheme.colorScheme.secondary
                        else                      -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(WlanLayout.DialogTitleIconSize)
                )
                Text(
                    text = when (state) {
                        is WlanDialogState.Add    -> "添加校园网账号"
                        is WlanDialogState.Edit   -> "修改账号信息"
                        is WlanDialogState.Log    -> "${state.entity.studentId} 的日志"
                        is WlanDialogState.Delete -> "删除确认"
                        is WlanDialogState.None   -> ""
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier              = Modifier.fillMaxWidth(),
                verticalArrangement   = Arrangement.spacedBy(WlanLayout.DialogFormSpacing)
            ) {
                when (state) {
                    is WlanDialogState.Add, is WlanDialogState.Edit -> {
                        if (isEdit) {
                            Surface(
                                color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape    = RoundedCornerShape(WlanLayout.DialogHintCorner),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier              = Modifier.padding(horizontal = WlanLayout.DialogHintPaddingH, vertical = WlanLayout.DialogHintPaddingV),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(WlanLayout.DialogHintIconToTextSpacing)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        null,
                                        tint     = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(WlanLayout.DialogHintIconSize)
                                    )
                                    Text(
                                        "学号为账号唯一标识，不可修改",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value         = idText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) idText = it },
                            label         = { Text("学号") },
                            singleLine    = true,
                            enabled       = isAdd,
                            isError       = isIdError && isAdd,
                            supportingText = {
                                if (isIdError && isAdd) Text("学号不能为空")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.School,
                                    null,
                                    modifier = Modifier.size(WlanLayout.DialogFieldIconSize),
                                    tint     = if (isIdError && isAdd)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value         = passwordText,
                            onValueChange = { passwordText = it.toHalfWidth() },
                            label         = { Text("密码") },
                            singleLine    = true,
                            isError       = isPasswordError,
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
                                    modifier = Modifier.size(WlanLayout.DialogFieldIconSize),
                                    tint     = if (isPasswordError)
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

                        Column {
                            Row(
                                verticalAlignment    = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(WlanLayout.DialogLocationLabelSpacing),
                                modifier             = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(WlanLayout.DialogLocationLabelIconSize)
                                )
                                Text(
                                    text  = "选择登录区域",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(WlanLayout.DialogLocationOptionSpacing)
                            ) {
                                locationOptions.forEach { (displayName, tag) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier          = Modifier
                                            .weight(1f)
                                            .clickable { selectedLocationTag = tag }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = (selectedLocationTag == tag),
                                            onClick  = { selectedLocationTag = tag }
                                        )
                                        Text(
                                            text  = displayName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is WlanDialogState.Log -> {
                        val entity = state.entity
                        val isOk = entity.error == "ok" || entity.sucMsg.isNotBlank()

                        WlanLogRow(
                            icon      = if (isOk) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            iconTint  = if (isOk) Color(0xFF00A000) else MaterialTheme.colorScheme.error,
                            label     = "状态码",
                            value     = entity.error.ifBlank { "—" }
                        )
                        WlanLogRow(
                            icon     = Icons.Default.CheckCircle,
                            iconTint = Color(0xFF00A000),
                            label    = "成功信息",
                            value    = entity.sucMsg
                        )
                        WlanLogRow(
                            icon     = Icons.Default.ErrorOutline,
                            iconTint = MaterialTheme.colorScheme.error,
                            label    = "错误信息",
                            value    = entity.errorMsg
                        )
                        WlanLogRow(
                            icon     = Icons.Default.NetworkCheck,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            label    = "策略信息",
                            value    = entity.ployMsg
                        )

                        HorizontalDivider(
                            modifier  = Modifier.padding(vertical = WlanLayout.DialogLogDividerPaddingV),
                            thickness = 0.5.dp,
                            color     = MaterialTheme.colorScheme.outlineVariant
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text     = "异常日志记录",
                                style    = MaterialTheme.typography.labelMedium,
                                color    = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            if (entity.log.isNotBlank()) {
                                Text(
                                    text        = entity.log,
                                    style       = MaterialTheme.typography.bodySmall,
                                    color       = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight  = TextUnit(16f, TextUnitType.Sp)
                                )
                            } else {
                                Text(
                                    text  = "暂无日志记录",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    is WlanDialogState.Delete -> {
                        Text(
                            text  = "确定要删除学号为 ${state.entity.studentId} 的账号吗？\n此操作不可撤销。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    is WlanDialogState.None -> {}
                }
            }
        },
        confirmButton = {
            when (state) {
                is WlanDialogState.Add, is WlanDialogState.Edit -> {
                    TextButton(
                        onClick = {
                            if (isAdd)
                                onConfirmAdd(idText.trim(), passwordText, selectedLocationTag)
                            else
                                onConfirmEdit(idText.trim(), passwordText, selectedLocationTag)
                        },
                        enabled = canSave
                    ) { Text(if (isAdd) "添加" else "保存") }
                }
                is WlanDialogState.Delete -> {
                    TextButton(onClick = onConfirmDelete) {
                        Text("确认删除", color = MaterialTheme.colorScheme.error)
                    }
                }
                is WlanDialogState.Log -> {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                is WlanDialogState.None -> {}
            }
        },
        dismissButton = {
            if (state !is WlanDialogState.Log) {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

@Composable
fun WlanLogRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(WlanLayout.LogRowCorner)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = WlanLayout.LogRowPaddingH, vertical = WlanLayout.LogRowPaddingV),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WlanLayout.LogRowIconToContentSpacing)
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(WlanLayout.LogRowIconSize))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun WlanSkeletonContent() {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(WlanLayout.SkeletonPadding),
        verticalArrangement = Arrangement.spacedBy(WlanLayout.SkeletonCardSpacing)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WlanLayout.SkeletonCardHeight),
                shape  = RoundedCornerShape(WlanLayout.SkeletonCardCorner),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {}
        }
    }
}

@Composable
fun WlanEmptyContent(onAddClick: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WlanLayout.EmptyContentSpacing)
        ) {
            Icon(
                Icons.Default.WifiOff,
                null,
                modifier = Modifier.size(WlanLayout.EmptyIconSize),
                tint     = Color.LightGray
            )
            Text("暂未添加 Wlan 账号", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            Surface(
                onClick      = onAddClick,
                shape        = RoundedCornerShape(WlanLayout.EmptyButtonCorner),
                color        = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = WlanLayout.EmptyButtonPaddingH, vertical = WlanLayout.EmptyButtonPaddingV),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(WlanLayout.EmptyButtonIconSize))
                    Text("添加账号", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun WlanErrorContent(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WlanLayout.ErrorContentSpacing)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                null,
                modifier = Modifier.size(WlanLayout.ErrorIconSize),
                tint     = MaterialTheme.colorScheme.error
            )
            Text("加载失败", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

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
            .padding(horizontal = WlanLayout.CardPaddingH, vertical = WlanLayout.CardPaddingV)
            .clickable {
                if (!info.isActive) onSetPrimaryClick()
            },
        shape  = RoundedCornerShape(WlanLayout.CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = GradeLayout.CardElevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = WlanLayout.CardHeaderPaddingStart, end = WlanLayout.CardHeaderPaddingEnd, top = WlanLayout.CardHeaderPaddingV, bottom = WlanLayout.CardHeaderPaddingV),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CellTower,
                    null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(WlanLayout.CardHeaderIconSize)
                )
                Spacer(modifier = Modifier.width(WlanLayout.CardHeaderIconToIdSpacing))

                Text(
                    info.studentId,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (info.isActive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(WlanLayout.ActiveLabelCorner)
                    ) {
                        Text(
                            text     = "主要",
                            modifier = Modifier.padding(horizontal = WlanLayout.ActiveLabelPaddingH, vertical = WlanLayout.ActiveLabelPaddingV),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick        = onEditClick,
                    contentPadding = PaddingValues(horizontal = WlanLayout.EditButtonPaddingH)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier           = Modifier.size(WlanLayout.EditButtonIconSize)
                        )
                        Spacer(Modifier.width(WlanLayout.EditButtonIconToTextSpacing))
                        Text(text = "点击修改", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            HorizontalDivider(
                modifier  = Modifier.padding(horizontal = WlanLayout.CardDividerPaddingH),
                thickness = 1.dp,
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier
                    .padding(WlanLayout.CardContentPadding)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(WlanLayout.DetailRowSpacing)
                ) {
                    WlanDetailRow(Icons.Default.Place, "区域", formatLocation(info.location))
                    WlanDetailRow(Icons.Default.Language, "IP 地址", info.ip)
                    WlanDetailRow(Icons.Default.People, "总在线数量", info.onlineUser)

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(WlanLayout.ActionButtonRowSpacing)) {
                        Row(
                            modifier          = Modifier.clickable { onLogClick() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NetworkCheck, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(WlanLayout.ActionButtonIconSize))
                            Spacer(modifier = Modifier.width(WlanLayout.ActionButtonIconToTextSpacing))
                            Text("日志", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(
                            modifier          = Modifier.clickable { onDeleteClick() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(WlanLayout.ActionButtonIconSize))
                            Spacer(modifier = Modifier.width(WlanLayout.ActionButtonIconToTextSpacing))
                            Text("移除", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = "提示：点击卡片区域可将该账号设为主要",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.width(WlanLayout.ContentSectionSpacing))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick        = onLoginClick,
                        modifier       = Modifier.width(WlanLayout.LoginButtonWidth),
                        shape          = RoundedCornerShape(WlanLayout.LoginButtonCorner),
                        colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D100)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("登录", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick        = onLogoutClick,
                        modifier       = Modifier.width(WlanLayout.LoginButtonWidth),
                        shape          = RoundedCornerShape(WlanLayout.LoginButtonCorner),
                        colors         = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
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
            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(WlanLayout.DetailRowIconSize)
        )
        Spacer(modifier = Modifier.width(WlanLayout.DetailRowIconToLabelSpacing))
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

fun formatLocation(location: String): String {
    return when (location) {
        "@ctc"   -> "宿舍区域"
        "@ynufe" -> "教学区域"
        else     -> location.ifBlank { "—" }
    }
}