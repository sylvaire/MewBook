package com.mewbook.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ============================================
// Warm Claymorphism Theme
// 温暖橙色系 + 黏土风圆润设计
// ============================================

private val WarmLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant
)

private val WarmDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

@Composable
fun MewBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 禁用动态颜色，默认使用温暖主题
    dynamicColor: Boolean = false,
    systemBarColorOverride: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // 保持动态颜色选项仅在 Android 12+ 且启用时生效
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> WarmDarkColorScheme
        else -> WarmLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val chromeColor = if (darkTheme) colorScheme.primaryContainer else colorScheme.primary
            val statusBarAppearanceColor = systemBarColorOverride ?: chromeColor
            val navigationBarAppearanceColor = systemBarColorOverride ?: colorScheme.background
            val transparent = Color.Transparent.toArgb()

            window.statusBarColor = statusBarAppearanceColor.toArgb()
            window.navigationBarColor = if (systemBarColorOverride != null) {
                navigationBarAppearanceColor.toArgb()
            } else {
                transparent
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = statusBarAppearanceColor.luminance() > 0.5f
                isAppearanceLightNavigationBars = navigationBarAppearanceColor.luminance() > 0.5f
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// ============================================
// Claymorphism Design Tokens
// 黏土风设计令牌
// ============================================

object ClayDesign {
    // 圆角系统
    val CardRadius = 24.dp
    val ButtonRadius = 16.dp
    val InputRadius = 12.dp
    val ChipRadius = 20.dp
    val IconContainerRadius = 50.dp // 圆形

    // 阴影层级
    val CardShadowElevation1 = 8.dp
    val CardShadowElevation2 = 4.dp
    val ButtonShadowElevation = 6.dp

    // 阴影颜色
    val CardShadowColor = Color(0xFFF97316).copy(alpha = 0.12f)
    val CardShadowColor2 = Color(0xFFF97316).copy(alpha = 0.08f)
    val ButtonShadowColor = Color(0xFFF97316).copy(alpha = 0.25f)

    // 间距
    val CardPadding = 16.dp
    val CardSpacing = 12.dp
    val SectionSpacing = 24.dp
}
