package com.mewbook.app.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * 黏土风卡片双层阴影：暖色主色调，亮/暗主题自适应透明度。
 */
fun Modifier.clayCardShadow(): Modifier = composed {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val shadowPrimary = remember(primary, isDarkTheme) {
        if (isDarkTheme) Color.Black.copy(alpha = 0.44f) else primary.copy(alpha = 0.15f)
    }
    val shadowSecondary = remember(primary, isDarkTheme) {
        if (isDarkTheme) Color.Black.copy(alpha = 0.22f) else primary.copy(alpha = 0.10f)
    }
    val cardEdge = remember(outlineVariant, isDarkTheme) {
        outlineVariant.copy(alpha = if (isDarkTheme) 0.48f else 0f)
    }
    val shape = RoundedCornerShape(ClayDesign.CardRadius)
    val shadowed = this
        .shadow(
            elevation = ClayDesign.CardShadowElevation1,
            shape = shape,
            ambientColor = shadowSecondary,
            spotColor = shadowPrimary
        )
        .shadow(
            elevation = ClayDesign.CardShadowElevation2,
            shape = shape,
            ambientColor = shadowSecondary,
            spotColor = shadowSecondary
        )

    if (isDarkTheme) shadowed.border(1.dp, cardEdge, shape) else shadowed
}

/**
 * 按钮/浮动操作按钮阴影：暖色主色调。
 */
fun Modifier.clayButtonShadow(): Modifier = composed {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primary = MaterialTheme.colorScheme.primary
    this.shadow(
        elevation = ClayDesign.ButtonShadowElevation,
        spotColor = primary.copy(alpha = if (isDarkTheme) 0.18f else 0.25f),
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
