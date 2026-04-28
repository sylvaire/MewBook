package com.mewbook.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance

/**
 * 黏土风卡片双层阴影：暖色主色调，亮/暗主题自适应透明度。
 */
fun Modifier.clayCardShadow(): Modifier = composed {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primary = MaterialTheme.colorScheme.primary
    val shadowPrimary = remember(primary, isDarkTheme) {
        primary.copy(alpha = if (isDarkTheme) 0.08f else 0.15f)
    }
    val shadowSecondary = remember(primary, isDarkTheme) {
        primary.copy(alpha = if (isDarkTheme) 0.04f else 0.10f)
    }
    val shape = RoundedCornerShape(ClayDesign.CardRadius)
    this
        .shadow(ClayDesign.CardShadowElevation1, shape, spotColor = shadowPrimary)
        .shadow(ClayDesign.CardShadowElevation2, shape, spotColor = shadowSecondary)
}

/**
 * 按钮/浮动操作按钮阴影：暖色主色调。
 */
fun Modifier.clayButtonShadow(): Modifier = composed {
    val primary = MaterialTheme.colorScheme.primary
    this.shadow(
        elevation = ClayDesign.ButtonShadowElevation,
        spotColor = primary.copy(alpha = 0.25f),
        shape = RoundedCornerShape(ClayDesign.ButtonRadius)
    )
}

/**
 * 通用黏土阴影：可自定义高度、形状、透明度。
 */
fun Modifier.clayShadow(
    elevation: androidx.compose.ui.unit.Dp,
    shape: Shape = RoundedCornerShape(ClayDesign.CardRadius),
    alpha: Float = 0.12f
): Modifier = composed {
    val primary = MaterialTheme.colorScheme.primary
    this.shadow(elevation, shape, spotColor = primary.copy(alpha = alpha))
}
