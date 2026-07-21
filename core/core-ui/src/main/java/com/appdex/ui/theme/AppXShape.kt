package com.appdex.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * AppX Shape System — 统一圆角规范
 *
 * 禁止页面自定义 RoundedCornerShape，统一使用 AppXShape。
 */
object AppXShape {
    val None = RoundedCornerShape(0.dp)
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(12.dp)
    val Large = RoundedCornerShape(16.dp)
    val ExtraLarge = RoundedCornerShape(20.dp)
    val Full = RoundedCornerShape(28.dp)
    val Circle = RoundedCornerShape(50)
}

// ─── Material3 Shapes (for components using MaterialTheme.shapes) ───
val AppXShapes = Shapes(
    extraSmall = AppXShape.Small,
    small = AppXShape.Small,
    medium = AppXShape.Medium,
    large = AppXShape.Large,
    extraLarge = AppXShape.ExtraLarge
)
