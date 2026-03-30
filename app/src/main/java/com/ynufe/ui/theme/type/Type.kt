package com.ynufe.ui.theme.type

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════
// Material3 全局基础排版
//
// 仅覆盖 bodyLarge 作为全局基准，其余角色（headlineMedium、
// titleMedium、bodySmall 等）沿用 Material3 默认值。
// 页面中凡是使用 MaterialTheme.typography.xxx 的地方均走此处。
//
// 页面专属的排版常量与布局尺寸请分别查阅：
//   CourseType.kt  → CourseTextStyle / CourseLayout / CourseCardStyles
//   GradeType.kt   → GradeLayout
//   UserType.kt    → UserLayout
//   InfoType.kt    → InfoLayout
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