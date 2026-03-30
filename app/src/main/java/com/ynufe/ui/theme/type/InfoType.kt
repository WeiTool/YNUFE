package com.ynufe.ui.theme.type

import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════
// InfoType.kt — 关于页专属布局常量
//
// 集中管理 InfoScreen 所需的全部尺寸与间距，
// 禁止在 InfoScreen.kt 内硬编码 dp 数值。
//
// InfoScreen 通过 import com.ynufe.ui.theme.type.InfoLayout 引用本对象。
// ═══════════════════════════════════════════════════════════════════
object InfoLayout {

    // ── Logo 区域 ───────────────────────────────────────────────────
    /** Logo 图片距页面顶部的间距 */
    val LogoTopMargin = 48.dp
    /** Logo 图片尺寸 */
    val LogoSize = 72.dp
    /** Logo 下方应用名称与版本号之间的间距 */
    val TitleToVersionSpacing = 8.dp
    /** 版本号与下方功能列表之间的间距 */
    val HeaderBottomMargin = 32.dp

    // ── Section 分隔区 ───────────────────────────────────────────────
    /** 两个 Section 之间的垂直间距 */
    val SectionSpacing = 8.dp
    /** Section 标题水平外边距 */
    val SectionHeaderHorizontalPadding = 16.dp
    /** Section 标题上下内边距 */
    val SectionHeaderVerticalPadding = 4.dp

    // ── 功能列表条目卡片（InfoRowItem）───────────────────────────────
    /** 条目卡片左右外边距 */
    val CardHorizontalPadding = 16.dp
    /** 条目卡片上下外边距 */
    val CardVerticalPadding = 4.dp
    /** 条目卡片背景透明度 */
    val CardBackgroundAlpha = 0.5f
    /** 条目卡片圆角半径 */
    val CardCornerRadius = 12.dp
    /** 条目卡片内容区四边内边距 */
    val CardInnerPadding = 16.dp

    // ── 条目内部元素 ─────────────────────────────────────────────────
    /** 左侧功能图标尺寸 */
    val MainIconSize = 24.dp
    /** 图标与文字列之间的水平间距 */
    val IconToTextSpacing = 16.dp
    /** 右侧箭头图标尺寸 */
    val ArrowIconSize = 24.dp
}