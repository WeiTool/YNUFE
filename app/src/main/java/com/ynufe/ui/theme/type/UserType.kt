package com.ynufe.ui.theme.type

import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════
// UserType.kt — 用户页专属布局常量
//
// 集中管理 UserScreen 所需的全部尺寸与间距，
// 禁止在 UserScreen.kt 内硬编码 dp 数值。
//
// 文字样式统一使用 MaterialTheme.typography.xxx 体系，
// 本文件不重复定义（用户页无自适应文字需求）。
// ═══════════════════════════════════════════════════════════════════
object UserLayout {

    // ── 头像与个人信息区（ProfileHeader）────────────────────────────
    /** 个人信息区四边内边距 */
    val ProfileHeaderPadding = 24.dp
    /** 头像（圆形）直径 */
    val AvatarSize = 64.dp
    /** 头像与姓名/学号列之间的水平间距 */
    val AvatarToNameSpacing = 16.dp

    // ── 基本信息区（InfoSection）─────────────────────────────────────
    /** 信息卡片整体外边距 */
    val InfoSectionPadding = 16.dp
    /** 信息卡片圆角 */
    val InfoSectionCorner = 12.dp
    /** 分割线上下的垂直边距 */
    val InfoDividerPaddingV = 12.dp
    /** InfoItem 图标尺寸 */
    val InfoItemIconSize = 20.dp
    /** InfoItem 图标与文字之间的水平间距 */
    val InfoItemIconToTextSpacing = 12.dp

    // ── 操作按钮（ActionGrid）────────────────────────────────────────
    /** ActionGrid 四边外边距 */
    val ActionGridPadding = 16.dp
    /** 两个操作卡片之间的水平间距 */
    val ActionTileSpacing = 16.dp
    /** 操作卡片高度 */
    val ActionTileHeight = 100.dp
    /** 操作卡片圆角 */
    val ActionTileCorner = 16.dp
    /** 操作卡片内图标与文字之间的间距 */
    val ActionTileIconToTextSpacing = 8.dp

    // ── 提示信息横幅（HintCard，在操作按钮下方）─────────────────────
    /** 提示横幅外边距 */
    val HintCardMargin = 16.dp
    /** 提示横幅圆角 */
    val HintCardCorner = 12.dp
    /** 提示横幅水平内边距 */
    val HintCardPaddingH = 16.dp
    /** 提示横幅垂直内边距 */
    val HintCardPaddingV = 12.dp
    /** 提示横幅边框透明度 */
    val HintCardBorderAlpha = 0.2f
    /** 提示横幅背景透明度 */
    val HintCardBgAlpha = 0.4f
    /** 提示横幅图标与文字列的间距 */
    val HintCardIconToTextSpacing = 12.dp
    /** 提示横幅图标尺寸 */
    val HintCardIconSize = 20.dp

    // ── 悬浮工具栏（ExpandableToolbar）──────────────────────────────
    /** 主 FAB 直径 */
    val FabSize = 56.dp
    /** 工具栏条目（胶囊按钮）之间的间距 */
    val ToolbarItemSpacing = 8.dp
    /** 工具栏条目水平内边距 */
    val ToolbarItemPaddingH = 16.dp
    /** 工具栏条目垂直内边距 */
    val ToolbarItemPaddingV = 10.dp
    /** 工具栏条目内图标尺寸 */
    val ToolbarItemIconSize = 20.dp
    /** 主 FAB 内图标尺寸 */
    val FabIconSize = 24.dp

    // ── 对话框（UserActionDialog）────────────────────────────────────
    /** 验证码图片高度 */
    val VerifyCodeImageHeight = 60.dp
    /** 验证码图片宽度 */
    val VerifyCodeImageWidth = 150.dp
    /** 同步失败时错误图标尺寸 */
    val ErrorIconSize = 48.dp
    /** 加载进度圈尺寸 */
    val LoadingIndicatorSize = 24.dp
    /** 切换账号列表中头像圆圈直径 */
    val ChooseUserAvatarSize = 36.dp
    /** 切换账号列表中头像内图标尺寸 */
    val ChooseUserAvatarIconSize = 20.dp
    /** 切换账号每行水平内边距 */
    val ChooseUserItemPaddingH = 4.dp
    /** 切换账号每行垂直内边距 */
    val ChooseUserItemPaddingV = 10.dp
    /** 切换账号行圆角（高亮选中状态背景） */
    val ChooseUserItemCorner = 8.dp
    /** 切换账号头像与学号之间的水平间距 */
    val ChooseUserAvatarToIdSpacing = 12.dp
    /** 已选中用户右侧勾选图标尺寸 */
    val ChooseUserCheckIconSize = 20.dp

    // ── 骨架屏（Initializing 状态）──────────────────────────────────
    /** 骨架操作卡片高度（与 ActionTileHeight 保持一致） */
    val SkeletonTileHeight = 100.dp
    /** 骨架操作卡片圆角（与 ActionTileCorner 保持一致） */
    val SkeletonTileCorner = 16.dp
    /** 骨架操作卡片之间的间距（与 ActionTileSpacing 保持一致） */
    val SkeletonTileSpacing = 16.dp
    /** 骨架操作区整体外边距（与 ActionGridPadding 保持一致） */
    val SkeletonTilePadding = 16.dp

    // ── 完全空态（UserUiState.Empty）────────────────────────────────
    /** 空态图标尺寸 */
    val EmptyStateIconSize = 80.dp
    /** 空态图标与说明文字之间的间距 */
    val EmptyStateIconToTextSpacing = 16.dp
}