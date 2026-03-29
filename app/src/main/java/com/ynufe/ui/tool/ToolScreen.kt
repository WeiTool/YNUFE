package com.ynufe.ui.tool

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ynufe.data.room.grade.GradeEntity
import com.ynufe.data.room.wlan.UserWlanInfoEntity
import com.ynufe.ui.wlan.UserWlanCard
import com.ynufe.ui.wlan.WlanActionDialog
import com.ynufe.ui.wlan.WlanDialogType
import com.ynufe.ui.wlan.WlanViewModel
import kotlin.math.absoluteValue

// 与 GradeScreen 保持一致的背景色（2边卡片专用）
val GradeScreenBackgroundColor = Color(0xFFFDF1F0)

// ── 堆叠布局尺寸常量 ───────────────────────────────────────────────
private val CARD_HEIGHT  = 140.dp
private val PEEK_HEIGHT  =  35.dp
private val STACK_HEIGHT = CARD_HEIGHT + PEEK_HEIGHT * 2  // 210.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ToolScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToGrade: () -> Unit,
    onNavigateToWlan: () -> Unit,
    viewModel: ToolViewModel = hiltViewModel(),
    wlanViewModel: WlanViewModel = hiltViewModel()
) {
    val grades     by viewModel.randomGrades.collectAsState()
    val activeWlan by viewModel.activeWlanInfo.collectAsState()

    ToolScreenContent(
        grades                  = grades,
        activeWlan              = activeWlan,
        wlanViewModel           = wlanViewModel,
        sharedTransitionScope   = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onNavigateToGrade       = onNavigateToGrade,
        onNavigateToWlan        = onNavigateToWlan
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ToolScreenContent(
    grades: List<GradeEntity>,
    activeWlan: UserWlanInfoEntity?,
    wlanViewModel: WlanViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToGrade: () -> Unit,
    onNavigateToWlan: () -> Unit
) {
    var activeDialog   by remember { mutableStateOf(WlanDialogType.NONE) }
    var selectedEntity by remember { mutableStateOf<UserWlanInfoEntity?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── 堆叠卡片区域 ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(STACK_HEIGHT + 32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (grades.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GradeStackArea(
                        grades                  = grades,
                        sharedTransitionScope   = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onCardClick             = onNavigateToGrade
                    )
                    Text(
                        text     = "点击卡片查看完整成绩单",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                Text(
                    text  = "暂无成绩数据",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── 活跃网络账号区域 ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // 标题
            Text(
                text       = "活跃网络账号",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 12.dp)
            )

            // 卡片 / 空状态提示
            if (activeWlan != null) {
                UserWlanCard(
                    info          = activeWlan,
                    onLoginClick  = { wlanViewModel.loginWlan(activeWlan) },
                    onLogoutClick = { wlanViewModel.logoutWlan(activeWlan) },
                    onLogClick    = {
                        selectedEntity = activeWlan
                        activeDialog   = WlanDialogType.LOG
                    },
                    onEditClick   = {
                        val plainPassword = wlanViewModel.decryptPassword(activeWlan.password)
                        selectedEntity    = activeWlan.copy(password = plainPassword)
                        activeDialog      = WlanDialogType.EDIT
                    },
                    onDeleteClick = {
                        selectedEntity = activeWlan
                        activeDialog   = WlanDialogType.DELETE
                    }
                )
            } else {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = "未发现激活的校园网账号\n请前往校园网页面设置活跃用户",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── 前往 WlanScreen 详情页的导航按钮 ────────────────────
            //    样式对齐成绩区域的提示行，底部分割线 + 图标 + 文字 + 箭头
            HorizontalDivider(
                modifier  = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToWlan() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector    = Icons.Default.Wifi,
                    contentDescription = null,
                    tint           = MaterialTheme.colorScheme.primary,
                    modifier       = Modifier.size(18.dp)
                )
                Text(
                    text      = "管理全部校园网账号",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.primary,
                    modifier  = Modifier.weight(1f)
                )
                Icon(
                    imageVector    = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint           = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier       = Modifier.size(18.dp)
                )
            }
        }
    }

    if (activeDialog != WlanDialogType.NONE) {
        WlanActionDialog(
            type           = activeDialog,
            selectedEntity = selectedEntity,
            onDismiss      = {
                activeDialog   = WlanDialogType.NONE
                selectedEntity = null
            },
            onConfirmAdd    = { id, pw, loc -> wlanViewModel.addAccount(id, pw, loc) },
            onConfirmEdit   = { id, pw, loc -> wlanViewModel.updateAccount(id, pw, loc) },
            onConfirmDelete = {
                selectedEntity?.let { wlanViewModel.deleteAccount(it.studentId) }
                activeDialog = WlanDialogType.NONE
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GradeStackArea（堆叠轮播，核心逻辑见注释，未改动）
// ─────────────────────────────────────────────────────────────────────────────
@SuppressLint("FrequentlyChangingValue")
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GradeStackArea(
    grades: List<GradeEntity>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (grades.isEmpty()) return

    val displayGrades = remember(grades) { grades.take(5) }
    val virtualCount  = Int.MAX_VALUE
    val initialIndex  = virtualCount / 2 - (virtualCount / 2 % displayGrades.size)

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount   = { virtualCount }
    )

    VerticalPager(
        state               = pagerState,
        modifier            = modifier
            .fillMaxWidth()
            .height(STACK_HEIGHT),
        pageSize            = PageSize.Fixed(CARD_HEIGHT),
        contentPadding      = PaddingValues(vertical = PEEK_HEIGHT),
        horizontalAlignment = Alignment.CenterHorizontally,
        beyondViewportPageCount = 1
    ) { virtualPage ->

        val actualIndex = virtualPage % displayGrades.size
        val grade       = displayGrades[actualIndex]

        val pageOffset = (pagerState.currentPage - virtualPage) +
                pagerState.currentPageOffsetFraction
        val absOffset  = pageOffset.absoluteValue

        if (absOffset <= 1.5f) {
            val isCenter = absOffset < 0.05f
            val scale    = 1f - absOffset.coerceIn(0f, 1f) * 0.03f
            val alpha    = 1f - absOffset.coerceIn(0f, 1f) * 0.15f

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(CARD_HEIGHT)
                    .zIndex(2f - absOffset)
                    .graphicsLayer {
                        scaleX       = scale
                        scaleY       = scale
                        this.alpha   = alpha
                        translationY = pageOffset * (CARD_HEIGHT - PEEK_HEIGHT).toPx()
                    }
                    .then(
                        if (isCenter) Modifier.clickable { onCardClick() } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                GradeItemCard(
                    grade                   = grade,
                    sharedTransitionScope   = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    isShared                = isCenter
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GradeItemCard（未改动）
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GradeItemCard(
    grade: GradeEntity,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isShared: Boolean
) {
    val scoreValue = grade.score.toDoubleOrNull() ?: 0.0
    val isPassed   = scoreValue >= 60.0
    val scoreColor = if (isPassed) Color(0xFF008000) else MaterialTheme.colorScheme.error

    with(sharedTransitionScope) {
        Card(
            modifier = Modifier
                .then(
                    if (isShared) Modifier.sharedElement(
                        rememberSharedContentState(key = "grade_container"),
                        animatedVisibilityScope = animatedVisibilityScope
                    ) else Modifier
                )
                .fillMaxWidth()
                .height(CARD_HEIGHT)
                .padding(horizontal = 8.dp),
            shape     = RoundedCornerShape(16.dp),
            border    = BorderStroke(
                width = 1.dp,
                color = if (isShared) Color.White.copy(alpha = 0.4f) else Color(0xFFE8E8E8)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isShared) 8.dp else 1.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isShared) MaterialTheme.colorScheme.surfaceVariant
                else GradeScreenBackgroundColor
            )
        ) {
            Row(
                modifier              = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = grade.courseName,
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = "${grade.courseType} | 学分: ${grade.credit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text       = grade.score,
                        style      = MaterialTheme.typography.headlineSmall,
                        color      = scoreColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text  = "绩点: ${grade.gradePoint}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}