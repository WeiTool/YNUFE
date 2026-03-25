package com.ynufe.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════
// CourseType.kt — 课程表页专属排版与布局常量
//
// 本文件集中管理 CourseScreen 所需的全部排版参数与尺寸，
// 禁止在 CourseScreen.kt 内硬编码任何 sp / dp 数值。
//
// 结构说明：
//   CourseLayout     → 页面级布局尺寸（dp 常量）
//   CourseTextStyle  → 固定文字样式（非 MaterialTheme 派生）
//   CourseCardStyles → 卡片三行文字的一组样式（data class）
//   courseCardStylesForWidth()        → 按宽度选档（原始，无安全收缩）
//   rememberSafeCourseCardStyles()    → 带 fontScale 防溢出的 Composable 工厂
// ═══════════════════════════════════════════════════════════════════

// ───────────────────────────────────────────────────────────────────
// CourseLayout — 页面级布局尺寸
// ───────────────────────────────────────────────────────────────────
object CourseLayout {

    // ── 课表格子 ────────────────────────────────────────────────────
    /** 每个时间格子的高度，课程卡片按 rowSpan 倍数叠加 */
    val CellHeight = 80.dp
    /** 左侧时间列的固定宽度 */
    val TimeColWidth = 50.dp

    // ── 课程卡片 ────────────────────────────────────────────────────
    /** 卡片圆角半径 */
    val CardCorner = 6.dp
    /** 卡片水平内边距 */
    val CardPaddingH = 3.dp
    /** 卡片垂直内边距 */
    val CardPaddingV = 4.dp
    /** DayColumn 中卡片与列边框之间的间距 */
    val DayColumnCardPadding = 2.dp

    // ── 骨架屏（Loading 状态）───────────────────────────────────────
    /** 骨架屏占位卡高度 */
    val SkeletonCardHeight = 70.dp
    /** 骨架屏占位卡圆角 */
    val SkeletonCardCorner = 8.dp
    /** 骨架屏横向外边距 */
    val SkeletonPaddingH = 16.dp
    /** 骨架屏纵向外边距 */
    val SkeletonPaddingV = 12.dp
    /** 骨架屏卡片间距 */
    val SkeletonSpacing = 10.dp

    // ── 空态（NoUser / Empty 状态）──────────────────────────────────
    /** 空态图标尺寸 */
    val EmptyStateIconSize = 72.dp

    // ── 回到本周 FAB ─────────────────────────────────────────────
    /** 悬浮按钮大小 */
    val FabSize = 56.dp
    /** 悬浮按钮圆角 */
    val FabCorner = 16.dp
    /** 悬浮按钮距右边缘 */
    val FabPaddingEnd = 20.dp
    /** 悬浮按钮距底边缘 */
    val FabPaddingBottom = 32.dp

    // ── TopAppBar 菜单图标 ─────────────────────────────────────────
    /** 菜单 IconButton 宽度 */
    val MenuIconButtonWidth = 56.dp
    /** 菜单图标大小 */
    val MenuIconSize = 18.dp

    // ── 档位标签 Badge（TopAppBar actions）─────────────────────────
    /** 档位 Badge 水平内边距 */
    val LevelBadgePaddingH = 6.dp
    /** 档位 Badge 垂直内边距 */
    val LevelBadgePaddingV = 2.dp
    /** 档位 Badge 圆角 */
    val LevelBadgeCorner = 4.dp
    /** 档位 Badge 距 AppBar 右边距 */
    val LevelBadgePaddingEnd = 12.dp

    // ── 星期表头行 ─────────────────────────────────────────────────
    /** 表头每列垂直内边距 */
    val WeekHeaderPaddingV = 6.dp
    /** 今日列下方红色高亮条宽度 */
    val TodayIndicatorWidth = 20.dp
    /** 今日列下方红色高亮条高度 */
    val TodayIndicatorHeight = 2.dp
    /** 表头列名与高亮条之间的垂直间距 */
    val WeekHeaderInnerSpacing = 3.dp

    // ── 侧边抽屉 ───────────────────────────────────────────────────
    /** 抽屉最大宽度（平板适配上限） */
    val DrawerMaxWidth = 360.dp
    /** 抽屉条目圆角 */
    val DrawerItemCorner = 12.dp
}

// ───────────────────────────────────────────────────────────────────
// CourseTextStyle — 课程表页固定文字样式
//
// 所有样式均为独立的 TextStyle，不依赖 MaterialTheme，
// 可在任意 Composable 中直接引用，无需处于 composition 内。
// ───────────────────────────────────────────────────────────────────
object CourseTextStyle {

    // ── 时间列 ──────────────────────────────────────────────────────

    /**
     * 节次编号文字（"1"、"2"…"13"）。
     * 放置在 50dp 宽的时间列中，需清晰易读，SemiBold 加强视觉权重。
     */
    val timeSlotSection = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 15.sp,
    )

    /**
     * 节次对应的上/下课时间（如"08:00"、"08:40"）。
     * 辅助信息，9sp 小字；lineHeight 收紧，确保上下课两行均能
     * 在 80dp 单元格内与节次编号共存而不溢出。
     */
    val timeSlotTime = TextStyle(
        fontSize = 9.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 10.5.sp,
    )

    // ── 星期表头行 ──────────────────────────────────────────────────

    /**
     * 列顶部星期标签（"周一"…"周日"），12sp。
     * fontWeight 由调用处根据"是否为今日"动态覆盖为 Bold 或 Medium，
     * 此处定义的 Medium 为非当天的默认值。
     */
    val weekDayLabel = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
    )

    // ── TopAppBar 菜单图标标签 ──────────────────────────────────────

    /**
     * 菜单图标正下方的"菜单"文字标签，10sp。
     * 放置在 56dp 宽的 IconButton 内，字号须足够小以不拥挤布局。
     */
    val menuIconLabel = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 11.sp,
    )

    // ── 档位标签 Badge ──────────────────────────────────────────────

    /**
     * TopAppBar 右侧显示当前宽度档位（"极窄屏"/"窄屏"等）的小标签。
     * 使用 Black 字重以形成视觉层次，10sp 保持紧凑。
     */
    val levelBadge = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        lineHeight = 11.sp,
    )
}

// ═══════════════════════════════════════════════════════════════════
// CourseCardStyles — 课程卡片三行文字样式组
//
// ─── 卡片内容宽度估算 ──────────────────────────────────────────────
//   内容宽度 ≈ (屏幕宽度 - TimeColWidth 50dp) / 7
//              - DayColumn 左右 padding 各 2dp（共 4dp）
//              - 卡片水平 padding 各 3dp（共 6dp）
//   ≈ (屏幕宽度 - 50) / 7 - 10
//
// ─── 卡片可用高度（rowSpan=1）─────────────────────────────────────
//   CellHeight 80dp
//   - DayColumn padding 4dp
//   - 卡片 vertical padding 8dp
//   ────────────────────────────────────────
//   BoxWithConstraints.maxHeight ≈ 68dp
//
// ─── 档位断点（依据卡片内容宽度）─────────────────────────────────
//   < 36dp  → XS 极窄档：小屏手机  （480dpi 1K 机舒适基准）
//   36~48dp → SM 窄档  ：主流大屏手机（最常见）
//   48~68dp → MD 中档  ：折叠屏/平板竖屏
//   ≥ 68dp  → LG 宽档  ：平板横屏/大折叠屏全展开
//
// ─── 高度预算验证（fontScale=1.0）────────────────────────────────
//   XS (titleMaxLines=2): 2×17.0 + 12.5 + 12.0 = 58.5dp ✓  余量 9.5dp
//   SM (titleMaxLines=2): 2×18.5 + 13.5 + 13.0 = 63.5dp ✓  余量 4.5dp
//   MD (titleMaxLines=1): 1×21.5 + 15.5 + 14.0 = 51.0dp ✓  余量 17.0dp
//   LG (titleMaxLines=1): 1×24.0 + 17.5 + 16.0 = 57.5dp ✓  余量 10.5dp
//
// ─── 国产 2K 机适配说明 ──────────────────────────────────────────
//   溢出根本原因是 fontScale > 1.0，而非物理 PPI。
//   safeFactor 机制：读取 fontScale 后等比压缩字号至恰好填满卡片，
//   压缩下限 0.60，极端情况由 maxLines + TextOverflow.Ellipsis 兜底。
// ═══════════════════════════════════════════════════════════════════

/**
 * 课程卡片在某一宽度档位下使用的一组文字样式。
 *
 * 推荐通过 [rememberSafeCourseCardStyles] 工厂函数获取（含 fontScale 防溢出），
 * 或通过 [courseCardStylesForWidth] 取原始档位样式。
 *
 * @param title   课程名称——最重要，加粗、字号最大，允许多行显示
 * @param room    教室信息——次要，普通字重，单行截断
 * @param teacher 教师姓名——辅助，字号最小，单行截断
 */
data class CourseCardStyles(
    val title: TextStyle,
    val room: TextStyle,
    val teacher: TextStyle,
)

// ───────────────────────────────────────────────────────────────────
// 极窄档 XS：卡片内容宽度 < 36 dp
// 480dpi / 1K 屏 / fontScale=1.0 的用户实测舒适基准，各档等比推导。
// 高度预算（titleMaxLines=2）：2×17.0 + 12.5 + 12.0 = 58.5dp < 68dp ✓
// ───────────────────────────────────────────────────────────────────
private val cardStyleXS = CourseCardStyles(
    title = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 17.0.sp,
    ),
    room = TextStyle(
        fontSize = 11.0.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 12.5.sp,
    ),
    teacher = TextStyle(
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 12.0.sp,
    ),
)

// ───────────────────────────────────────────────────────────────────
// 窄档 SM：36dp ≤ 宽度 < 48dp（主流大屏手机，最常见）
// 以 XS 为基准，各字号上调约 13%。
// 高度预算（titleMaxLines=2）：2×18.5 + 13.5 + 13.0 = 63.5dp < 68dp ✓
// ───────────────────────────────────────────────────────────────────
private val cardStyleSM = CourseCardStyles(
    title = TextStyle(
        fontSize = 17.0.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 18.5.sp,
    ),
    room = TextStyle(
        fontSize = 12.0.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 13.5.sp,
    ),
    teacher = TextStyle(
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 13.0.sp,
    ),
)

// ───────────────────────────────────────────────────────────────────
// 中档 MD：48dp ≤ 宽度 < 68dp（折叠屏/平板竖屏）
// 以 SM 为基准，各字号上调约 12%；titleMaxLines=1 节省高度预算。
// 高度预算（titleMaxLines=1）：1×21.5 + 15.5 + 14.0 = 51.0dp < 68dp ✓
// ───────────────────────────────────────────────────────────────────
private val cardStyleMD = CourseCardStyles(
    title = TextStyle(
        fontSize = 19.0.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 21.5.sp,
    ),
    room = TextStyle(
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 15.5.sp,
    ),
    teacher = TextStyle(
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 14.0.sp,
    ),
)

// ───────────────────────────────────────────────────────────────────
// 宽档 LG：宽度 ≥ 68dp（大平板横屏/大折叠屏全展开）
// 以 MD 为基准，各字号上调约 11%；titleMaxLines=1 精练显示。
// 高度预算（titleMaxLines=1）：1×24.0 + 17.5 + 16.0 = 57.5dp < 68dp ✓
// ───────────────────────────────────────────────────────────────────
private val cardStyleLG = CourseCardStyles(
    title = TextStyle(
        fontSize = 21.0.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.0.sp,
    ),
    room = TextStyle(
        fontSize = 15.5.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 17.5.sp,
    ),
    teacher = TextStyle(
        fontSize = 14.0.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.0.sp,
    ),
)

/**
 * 根据卡片内容宽度返回对应档位的【原始】样式（不含 fontScale 安全收缩）。
 * 多数情况下应优先使用 [rememberSafeCourseCardStyles]。
 *
 * @param cardMaxWidthDp  BoxWithConstraints.maxWidth.value（dp，Float）
 */
fun courseCardStylesForWidth(cardMaxWidthDp: Float): CourseCardStyles = when {
    cardMaxWidthDp < 36f -> cardStyleXS   // 极窄：小屏手机
    cardMaxWidthDp < 48f -> cardStyleSM   // 窄：主流手机（最常见）
    cardMaxWidthDp < 68f -> cardStyleMD   // 中：折叠屏/平板竖屏
    else                 -> cardStyleLG   // 宽：平板横屏/大折叠屏
}

// ───────────────────────────────────────────────────────────────────
// scaledBy — 等比缩放整组样式（safeFactor 辅助函数）
// ───────────────────────────────────────────────────────────────────

/**
 * 将三个字号及其行高等比乘以 [factor]，返回新的 [CourseCardStyles]。
 * factor ≥ 1f 时原样返回，字号下限 6sp、行高下限 7sp。
 */
private fun CourseCardStyles.scaledBy(factor: Float): CourseCardStyles {
    if (factor >= 1f) return this
    val f = factor.coerceAtLeast(0.60f) // 绝对下限：最多缩到 60%
    return CourseCardStyles(
        title = title.copy(
            fontSize   = (title.fontSize.value   * f).coerceAtLeast(6f).sp,
            lineHeight = (title.lineHeight.value * f).coerceAtLeast(7f).sp,
        ),
        room = room.copy(
            fontSize   = (room.fontSize.value   * f).coerceAtLeast(6f).sp,
            lineHeight = (room.lineHeight.value * f).coerceAtLeast(7f).sp,
        ),
        teacher = teacher.copy(
            fontSize   = (teacher.fontSize.value   * f).coerceAtLeast(6f).sp,
            lineHeight = (teacher.lineHeight.value * f).coerceAtLeast(7f).sp,
        ),
    )
}

// ───────────────────────────────────────────────────────────────────
// rememberSafeCourseCardStyles — 带 fontScale 防溢出的 Composable 工厂
// ───────────────────────────────────────────────────────────────────

/**
 * 在 [BoxWithConstraints] 回调内使用，读取系统 fontScale 并自动防溢出。
 *
 * ### 工作流程
 * 1. 按 [cardWidthDp] 选出对应档位的基础样式（XS/SM/MD/LG）
 * 2. 估算当前 fontScale 下三行文字实际占用的 dp 高度：
 *    `neededDp = (title.lineHeight × titleMaxLines + room.lineHeight + teacher.lineHeight) × fontScale`
 * 3. 若 neededDp > [cardHeightDp]，等比压缩全部字号至恰好填满
 * 4. 压缩系数下限 0.60，最终由 maxLines + TextOverflow.Ellipsis 兜底
 *
 * @param cardWidthDp   BoxWithConstraints.maxWidth.value（dp，Float）
 * @param cardHeightDp  BoxWithConstraints.maxHeight.value（dp，Float）
 * @param titleMaxLines 课程名允许的最大行数（1 或 2 用于 rowSpan=1，3 用于 rowSpan≥2）
 * @return 经过 fontScale 安全校正的 [CourseCardStyles]
 */
@Composable
fun rememberSafeCourseCardStyles(
    cardWidthDp: Float,
    cardHeightDp: Float,
    titleMaxLines: Int,
): CourseCardStyles {
    // 读取系统字体缩放比例：fontScale=1.0 → 默认；> 1.0 → 用户放大
    val fontScale = LocalDensity.current.fontScale

    val base = courseCardStylesForWidth(cardWidthDp)

    // 估算三行文字在当前 fontScale 下实际需要的 dp 高度
    // 原理：sp 渲染出的视觉高度(dp) = lineHeight(sp) × fontScale
    val neededDp = (base.title.lineHeight.value * titleMaxLines
            + base.room.lineHeight.value
            + base.teacher.lineHeight.value) * fontScale

    val safeFactor = if (neededDp <= cardHeightDp) 1f
    else cardHeightDp / neededDp

    return base.scaledBy(safeFactor)
}