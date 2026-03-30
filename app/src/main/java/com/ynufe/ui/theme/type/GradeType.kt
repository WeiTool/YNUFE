package com.ynufe.ui.theme.type

import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════
// GradeType.kt — 成绩页专属布局常量
//
// 集中管理 GradeScreen 所需的全部尺寸与间距，
// 禁止在 GradeScreen.kt 内硬编码 dp 数值。
//
// 文字样式统一使用 MaterialTheme.typography.xxx 体系，
// 本文件不重复定义（成绩页无自适应文字需求）。
// ═══════════════════════════════════════════════════════════════════
object GradeLayout {

    // ── 列表整体 ────────────────────────────────────────────────────
    /** LazyColumn 四边内边距 */
    val ContentPadding = 16.dp
    /** 成绩卡片之间的垂直间距 */
    val ItemSpacing = 12.dp

    // ── 顶部搜索 & 筛选区 ───────────────────────────────────────────
    /** 标题与搜索框之间的间距 */
    val TitleToSearchSpacing = 16.dp
    /** 搜索框圆角半径 */
    val SearchBarCorner = 12.dp
    /** 搜索框与筛选栏之间的间距 */
    val SearchToFilterSpacing = 12.dp
    /** 筛选 Chip 之间的水平间距 */
    val FilterChipSpacing = 8.dp

    // ── 成绩卡片 ────────────────────────────────────────────────────
    /** 卡片阴影高度 */
    val CardElevation = 2.dp

    // ── 骨架屏（Loading 状态）───────────────────────────────────────
    /** 骨架屏占位卡高度 */
    val SkeletonCardHeight = 80.dp
    /** 骨架屏占位卡圆角 */
    val SkeletonCardCorner = 12.dp

    // ── 空态（搜索/筛选结果为空）────────────────────────────────────
    /** 搜索结果为空时图标尺寸（内嵌于列表） */
    val EmptySearchIconSize = 48.dp
    /** 搜索结果为空时，图标距列表顶部的 padding */
    val EmptySearchTopPadding = 64.dp
    /** 图标与"未找到相关成绩记录"文字之间的间距 */
    val EmptySearchIconToTextSpacing = 8.dp

    // ── 完全空态（GradeUiState.Empty）──────────────────────────────
    /** 全空态页图标尺寸 */
    val EmptyStateIconSize = 64.dp
    /** 全空态图标与标题之间的间距 */
    val EmptyStateIconToTitleSpacing = 16.dp
}