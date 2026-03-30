package com.ynufe.ui.theme.type

import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════
// ToolType.kt — 工具页专属布局常量
//
// 集中管理 ToolScreen 所需的全部尺寸与间距，
// 禁止在 ToolScreen.kt 内硬编码 dp 数值。
//
// 堆叠卡片区的私有常量（CARD_HEIGHT / PEEK_HEIGHT / STACK_HEIGHT）
// 也迁移至此处统一管理，ToolScreen 中原有的顶层 val 可删除。
//
// 文字样式统一使用 MaterialTheme.typography.xxx 体系，
// 本文件不重复定义（工具页无自适应文字需求）。
// ═══════════════════════════════════════════════════════════════════
object ToolLayout {

    // ── 堆叠成绩卡片区（GradeStackArea / GradeItemCard）─────────────
    /** 单张成绩卡片的固定高度（PageSize.Fixed 参数） */
    val GradeCardHeight = 140.dp
    /** 上下相邻卡片的"窥视"高度（VerticalPager contentPadding） */
    val GradePeekHeight = 35.dp
    /** 堆叠整体高度（= GradeCardHeight + GradePeekHeight × 2） */
    val GradeStackHeight = 210.dp
    /** 堆叠 Box 在 GradeStackHeight 之外的额外高度余量 */
    val GradeStackBoxExtra = 32.dp
    /** 居中卡片宽度占屏幕宽度的比例（fillMaxWidth 参数，非 dp） */
    val GradeCardWidthFraction = 0.88f
    /** 成绩卡片水平内边距（左右各缩进量） */
    val GradeCardPaddingH = 8.dp
    /** 成绩卡片圆角 */
    val GradeCardCorner = 16.dp
    /** 成绩卡片内容区（Row）四边内边距 */
    val GradeCardContentPadding = 16.dp

    // ── 成绩区底部提示文字（「点击卡片查看完整成绩单」）──────────────
    /** 提示文字与成绩卡片堆叠区之间的顶部间距 */
    val GradeHintPaddingTop = 8.dp

    // ── 活跃网络账号区域（ActiveWlan）───────────────────────────────
    /** 「活跃网络账号」标题行左侧内边距 */
    val ActiveWlanTitlePaddingStart = 16.dp
    /** 「活跃网络账号」标题行右侧内边距 */
    val ActiveWlanTitlePaddingEnd = 16.dp
    /** 「活跃网络账号」标题行下方内边距 */
    val ActiveWlanTitlePaddingBottom = 12.dp
    /** 无激活账号时提示文字的顶部内边距（垂直居中补偿） */
    val ActiveWlanEmptyPaddingTop = 40.dp

    // ── 导航按钮（「管理全部校园网账号」跳转行）─────────────────────
    /** 导航区上方分割线水平内边距 */
    val NavDividerPaddingH = 16.dp
    /** 导航行水平内边距 */
    val NavRowPaddingH = 16.dp
    /** 导航行垂直内边距 */
    val NavRowPaddingV = 14.dp
    /** 导航行内图标、文字、箭头之间的水平间距 */
    val NavRowSpacing = 12.dp
    /** 导航行图标（Wifi / KeyboardArrowRight）尺寸 */
    val NavIconSize = 18.dp
}