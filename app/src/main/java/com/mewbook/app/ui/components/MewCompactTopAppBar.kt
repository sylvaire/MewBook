package com.mewbook.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat

@Composable
fun MewCompactTopAppBar(
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleContent: (@Composable RowScope.() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val rawStatusBarInsetPx = WindowInsetsCompat
        .toWindowInsetsCompat(view.rootWindowInsets)
        .getInsets(WindowInsetsCompat.Type.statusBars())
        .top
    val topChromePadding = with(density) { rawStatusBarInsetPx.toDp() }.coerceAtMost(16.dp)
    val contentRowHeight = 36.dp
    val isDarkTheme = isSystemInDarkTheme()
    val resolvedContainerColor = if (containerColor != Color.Unspecified) {
        containerColor
    } else if (isDarkTheme) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val resolvedContentColor = if (contentColor != Color.Unspecified) {
        contentColor
    } else if (isDarkTheme) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Surface(
        color = resolvedContainerColor,
        contentColor = resolvedContentColor
    ) {
        CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topChromePadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(contentRowHeight)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (navigationIcon != null) 48.dp else 12.dp)
                            .height(contentRowHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        navigationIcon?.invoke()
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (titleContent != null) {
                            titleContent()
                        } else {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .height(contentRowHeight)
                            .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }
        }
    }
}
