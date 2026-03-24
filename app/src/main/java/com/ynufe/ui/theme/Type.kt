package com.ynufe.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════
// Material3 基础排版
//
// 仅覆盖 bodyLarge 作为全局基准，其余角色（headlineMedium、
// titleMedium、bodySmall 等）沿用 Material3 默认值。
// 页面中凡是使用 MaterialTheme.typography.xxx 的地方均走此处。
// ═══════════════════════════════════════════════════════════════════
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

// ═══════════════════════════════════════════════════════════════════
// 课程卡片自适应文字样式
//
// 【背景说明】
// 课程卡片排列在 7 列等宽网格中，每列实际可用的"内容宽度"
//（即 BoxWithConstraints 内部测量到的 maxWidth）计算公式：
//
//   内容宽度 = (屏幕宽度 - 时间列宽 50dp) / 7
//              - DayColumn 左右 padding 各 2dp（共 4dp）
//              - 卡片 水平 padding 各 3dp（共 6dp）
//
// 各典型设备对应卡片内容宽度估算：
//   ┌─────────────────────┬────────────┬───────────────┐
//   │ 设备类型             │ 屏幕宽度   │ 卡片内容宽度  │
//   ├─────────────────────┼────────────┼───────────────┤
//   │ 小屏手机             │ ~360 dp    │ ~34 dp        │
//   │ 主流手机             │ ~390 dp    │ ~38 dp        │
//   │ 大屏手机             │ ~412 dp    │ ~42 dp        │
//   │ 折叠屏/平板竖屏      │ ~600 dp    │ ~69 dp        │
//   │ 平板横屏/大折叠屏    │ ~840 dp+   │ ~109 dp+      │
//   └─────────────────────┴────────────┴───────────────┘
//
// 【断点划分】（依据卡片内容宽度，单位 dp）
//   < 36  → 极窄档 XS：小屏手机
//   36~48 → 窄档   SM：主流/大屏手机（最常见场景）
//   48~68 → 中档   MD：折叠屏半开、平板竖屏
//   ≥ 68  → 宽档   LG：平板横屏、大折叠屏全展开
//
// 【卡片高度说明】
//   单格(rowSpan=1) 内容高度 ≈ 80 - 4(DayColumn上下) - 8(卡片上下) = 68 dp
//   双格(rowSpan=2) 内容高度 ≈ 160 - 4 - 8 = 148 dp
//
// 【设计目标】
//   三行信息（课名 / 教室 / 教师）在 rowSpan=1 的 68dp 高度内全部可见：
//   以 SM 档为例：
//     课名  9.5sp × 最多2行 = 22dp
//     教室  8.5sp × 1行    =  9dp
//     教师  8.0sp × 1行    =  8dp
//     合计 ≈ 39dp < 68dp  ✓ 有充足余量
//   全部配合 maxLines + TextOverflow.Ellipsis 保底，不会越界
// ═══════════════════════════════════════════════════════════════════

/**
 * 课程卡片在某一宽度档位下使用的一组文字样式。
 *
 * 通过 [courseCardStylesForWidth] 工厂函数获取实例，
 * 在 CourseCard 的 BoxWithConstraints 回调内使用。
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
// 维持你认为“能看清”的 15sp 大字号基准
// ───────────────────────────────────────────────────────────────────
private val cardStyleXS = CourseCardStyles(
    title = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 17.sp,
    ),
    room = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 12.sp,
    ),
    teacher = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 12.sp,
    ),
)

// ───────────────────────────────────────────────────────────────────
// 窄档 SM：36 dp ≤ 卡片内容宽度 < 48 dp（主流大屏手机）
// 在 XS 的基础上适度增加，匹配更宽的列
// ───────────────────────────────────────────────────────────────────
private val cardStyleSM = CourseCardStyles(
    title = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 19.sp,
    ),
    room = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 14.5.sp,
    ),
    teacher = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 13.5.sp,
    ),
)

// ───────────────────────────────────────────────────────────────────
// 中档 MD：48 dp ≤ 卡片内容宽度 < 68 dp（折叠屏展开/平板竖屏）
// ───────────────────────────────────────────────────────────────────
private val cardStyleMD = CourseCardStyles(
    title = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 22.sp,
    ),
    room = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 17.sp,
    ),
    teacher = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
    ),
)

// ───────────────────────────────────────────────────────────────────
// 宽档 LG：卡片内容宽度 ≥ 68 dp（大平板横屏）
// ───────────────────────────────────────────────────────────────────
private val cardStyleLG = CourseCardStyles(
    title = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 25.sp,
    ),
    room = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    ),
    teacher = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 19.sp,
    ),
)

/**
 * 根据 BoxWithConstraints 内测量到的卡片内容宽度，返回对应档位的样式组合。
 *
 * 调用示例（CourseCard 内的 BoxWithConstraints 回调中）：
 * ```kotlin
 * BoxWithConstraints(...) {
 *     val styles = courseCardStylesForWidth(maxWidth.value)
 *     Text(text = course.name,    style = styles.title,   maxLines = 3, overflow = TextOverflow.Ellipsis)
 *     Text(text = course.room,    style = styles.room,    maxLines = 1, overflow = TextOverflow.Ellipsis)
 *     Text(text = course.teacher, style = styles.teacher, maxLines = 1, overflow = TextOverflow.Ellipsis)
 * }
 * ```
 *
 * @param cardMaxWidthDp  BoxWithConstraints.maxWidth.value（单位 dp，Float）
 * @return 与当前宽度匹配的 [CourseCardStyles] 实例
 */
fun courseCardStylesForWidth(cardMaxWidthDp: Float): CourseCardStyles = when {
    cardMaxWidthDp < 36f -> cardStyleXS   // 极窄：小屏手机
    cardMaxWidthDp < 48f -> cardStyleSM   // 窄：主流手机（最常见）
    cardMaxWidthDp < 68f -> cardStyleMD   // 中：折叠屏/平板竖屏
    else                 -> cardStyleLG   // 宽：平板横屏/大折叠屏
}

// ═══════════════════════════════════════════════════════════════════
// 课表页其他固定文字样式
//
// 以下样式用于课程卡片之外的 UI 组件，不需要随卡片宽度自适应，
// 统一通过 AppTextStyle 单例集中管理，禁止在页面文件内硬编码 sp。
// ═══════════════════════════════════════════════════════════════════
object AppTextStyle {

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

    // ── TopAppBar 菜单 ──────────────────────────────────────────

    /**
     * 菜单图标正下方的"菜单"文字标签，10sp。
     * 放置在 56dp 宽的 IconButton 内，字号须足够小以不拥挤布局。
     */
    val menuIconLabel = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 11.sp,
    )
}