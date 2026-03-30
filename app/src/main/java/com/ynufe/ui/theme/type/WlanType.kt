package com.ynufe.ui.theme.type

import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════
// WlanType.kt — Wlan 页专属布局常量
//
// 集中管理 WlanScreen 所需的全部尺寸与间距，
// 禁止在 WlanScreen.kt 内硬编码 dp 数值。
//
// 文字样式统一使用 MaterialTheme.typography.xxx 体系，
// 本文件不重复定义（Wlan 页无自适应文字需求）。
// ═══════════════════════════════════════════════════════════════════
object WlanLayout {

    // ── 右下角工具栏（FAB 按钮区）────────────────────────────────────
    /** 工具栏列四边外边距 */
    val FabAreaPadding = 16.dp
    /** 两个 ExtendedFAB 按钮之间的垂直间距 */
    val FabSpacing = 12.dp

    // ── 账号列表（WlanListContent）──────────────────────────────────
    /** 列表底部内边距（避免最后一张卡片被右下角 FAB 遮挡） */
    val ListBottomPadding = 88.dp
    /** 列表标题行水平内边距 */
    val ListTitlePaddingH = 16.dp
    /** 列表标题行垂直内边距 */
    val ListTitlePaddingV = 12.dp

    // ── 账号卡片（UserWlanCard）─────────────────────────────────────
    /** 卡片水平外边距 */
    val CardPaddingH = 16.dp
    /** 卡片垂直外边距 */
    val CardPaddingV = 8.dp
    /** 卡片圆角 */
    val CardCorner = 12.dp
    /** 卡片 Header 行左侧内边距 */
    val CardHeaderPaddingStart = 16.dp
    /** 卡片 Header 行右侧内边距（为"点击修改"按钮留出空间） */
    val CardHeaderPaddingEnd = 8.dp
    /** 卡片 Header 行上下内边距 */
    val CardHeaderPaddingV = 8.dp
    /** 卡片 Header 信号塔图标（CellTower）尺寸 */
    val CardHeaderIconSize = 20.dp
    /** 信号塔图标与学号文字之间的水平间距 */
    val CardHeaderIconToIdSpacing = 8.dp
    /** 「主要」激活状态标签圆角 */
    val ActiveLabelCorner = 4.dp
    /** 「主要」激活状态标签水平内边距 */
    val ActiveLabelPaddingH = 6.dp
    /** 「主要」激活状态标签垂直内边距 */
    val ActiveLabelPaddingV = 2.dp
    /** 「点击修改」TextButton 水平 ContentPadding */
    val EditButtonPaddingH = 8.dp
    /** 「点击修改」按钮内图标（Edit）尺寸 */
    val EditButtonIconSize = 18.dp
    /** 「点击修改」按钮内图标与文字之间的水平间距 */
    val EditButtonIconToTextSpacing = 4.dp
    /** 卡片内 Header 与内容区之间分割线的水平内边距 */
    val CardDividerPaddingH = 16.dp
    /** 卡片下方内容区四边内边距 */
    val CardContentPadding = 16.dp
    /** 详情行（WlanDetailRow）之间的垂直间距 */
    val DetailRowSpacing = 8.dp
    /** 「日志」「移除」操作按钮行内各按钮之间的水平间距 */
    val ActionButtonRowSpacing = 16.dp
    /** 操作按钮（日志 / 移除）内图标尺寸 */
    val ActionButtonIconSize = 16.dp
    /** 操作按钮内图标与文字之间的水平间距 */
    val ActionButtonIconToTextSpacing = 4.dp
    /** 左侧信息列与右侧登录 / 注销按钮列之间的水平间距 */
    val ContentSectionSpacing = 12.dp
    /** 登录 / 注销按钮固定宽度 */
    val LoginButtonWidth = 85.dp
    /** 登录 / 注销按钮圆角 */
    val LoginButtonCorner = 8.dp

    // ── 详情行（WlanDetailRow）──────────────────────────────────────
    /** 详情行图标尺寸 */
    val DetailRowIconSize = 14.dp
    /** 详情行图标与标签文字之间的水平间距 */
    val DetailRowIconToLabelSpacing = 6.dp

    // ── 日志行（WlanLogRow）─────────────────────────────────────────
    /** 日志行背景 Surface 圆角 */
    val LogRowCorner = 8.dp
    /** 日志行水平内边距 */
    val LogRowPaddingH = 12.dp
    /** 日志行垂直内边距 */
    val LogRowPaddingV = 8.dp
    /** 日志行图标与右侧内容列之间的水平间距 */
    val LogRowIconToContentSpacing = 8.dp
    /** 日志行图标尺寸 */
    val LogRowIconSize = 16.dp

    // ── 骨架屏（WlanSkeletonContent）────────────────────────────────
    /** 骨架屏整体外边距 */
    val SkeletonPadding = 16.dp
    /** 骨架占位卡片之间的垂直间距 */
    val SkeletonCardSpacing = 12.dp
    /** 骨架占位卡片高度 */
    val SkeletonCardHeight = 130.dp
    /** 骨架占位卡片圆角 */
    val SkeletonCardCorner = 12.dp

    // ── 空态（WlanEmptyContent）─────────────────────────────────────
    /** 空态各元素（图标 / 文字 / 按钮）之间的垂直间距 */
    val EmptyContentSpacing = 12.dp
    /** 空态图标（WifiOff）尺寸 */
    val EmptyIconSize = 64.dp
    /** 空态「添加账号」按钮圆角（胶囊形） */
    val EmptyButtonCorner = 20.dp
    /** 空态「添加账号」按钮水平内边距 */
    val EmptyButtonPaddingH = 16.dp
    /** 空态「添加账号」按钮垂直内边距 */
    val EmptyButtonPaddingV = 9.dp
    /** 空态「添加账号」按钮内图标（PersonAdd）尺寸 */
    val EmptyButtonIconSize = 16.dp

    // ── 错误态（WlanErrorContent）───────────────────────────────────
    /** 错误态各元素之间的垂直间距 */
    val ErrorContentSpacing = 8.dp
    /** 错误态图标（ErrorOutline）尺寸 */
    val ErrorIconSize = 48.dp

    // ── 对话框（WlanActionDialog）────────────────────────────────────
    /** 对话框标题行图标与文字之间的水平间距 */
    val DialogTitleIconToTextSpacing = 8.dp
    /** 对话框标题行图标尺寸 */
    val DialogTitleIconSize = 22.dp
    /** 表单列各输入项之间的垂直间距 */
    val DialogFormSpacing = 8.dp
    /** EDIT 模式"学号不可修改"提示横幅圆角 */
    val DialogHintCorner = 8.dp
    /** EDIT 模式提示横幅水平内边距 */
    val DialogHintPaddingH = 12.dp
    /** EDIT 模式提示横幅垂直内边距 */
    val DialogHintPaddingV = 8.dp
    /** EDIT 模式提示横幅内图标与文字之间的间距 */
    val DialogHintIconToTextSpacing = 6.dp
    /** EDIT 模式提示横幅内图标尺寸 */
    val DialogHintIconSize = 14.dp
    /** 表单输入框 LeadingIcon（School / Lock）尺寸 */
    val DialogFieldIconSize = 20.dp
    /** 区域选择区标签行内元素之间的水平间距 */
    val DialogLocationLabelSpacing = 4.dp
    /** 区域选择区标签行图标（Place）尺寸 */
    val DialogLocationLabelIconSize = 14.dp
    /** 区域选择各单选项之间的水平间距 */
    val DialogLocationOptionSpacing = 8.dp
    /** LOG 对话框中分割线的垂直内边距 */
    val DialogLogDividerPaddingV = 12.dp
}