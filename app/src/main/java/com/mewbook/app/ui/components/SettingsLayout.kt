package com.mewbook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mewbook.app.ui.theme.ClayDesign

@Composable
fun SettingsPageScaffold(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSummaryCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    SettingsSurfaceCard(
        modifier = modifier.then(
            if (onClick != null) Modifier.clip(RoundedCornerShape(ClayDesign.CardRadius)).clickable(onClick = onClick) else Modifier
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                SettingsIconContainer(icon = it, tint = accentColor)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailing != null || onClick != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailing?.invoke() ?: Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    trailing: (@Composable () -> Unit)? = null
) {
    SettingsSurfaceCard(
        modifier = modifier.then(
            if (onClick != null) Modifier.clip(
                RoundedCornerShape(ClayDesign.CardRadius)
            ).clickable(onClick = onClick) else Modifier
        )
    ) {
        SettingsRowContent(
            icon = icon,
            title = title,
            subtitle = subtitle,
            accentColor = accentColor,
            trailing = trailing ?: {
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
fun SettingsSwitchRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    SettingsSurfaceCard(
        modifier = modifier.then(
            Modifier
                .clip(RoundedCornerShape(ClayDesign.CardRadius))
                .clickable { onCheckedChange(!checked) }
        )
    ) {
        SettingsRowContent(
            icon = icon,
            title = title,
            subtitle = subtitle,
            accentColor = accentColor,
            trailing = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
        )
    }
}

@Composable
fun SettingsDangerRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsRowCard(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
        accentColor = MaterialTheme.colorScheme.error,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    )
}

@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsSurfaceCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun SettingsGroupRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    showDivider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
        ) {
            SettingsRowContent(
                icon = icon,
                title = title,
                subtitle = subtitle,
                accentColor = accentColor,
                trailing = trailing ?: {
                    if (onClick != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
        if (showDivider) {
            SettingsGroupDivider()
        }
    }
}

@Composable
fun SettingsGroupSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    showDivider: Boolean = true
) {
    SettingsGroupRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        accentColor = accentColor,
        showDivider = showDivider,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
fun SettingsGroupDangerRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    SettingsGroupRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
        accentColor = MaterialTheme.colorScheme.error,
        showDivider = showDivider,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    )
}

@Composable
fun SettingsSurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val resolvedContainerColor = if (containerColor == Color.Unspecified) {
        settingsCardContainerColor()
    } else {
        containerColor
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .settingsCardShadow(),
        shape = RoundedCornerShape(ClayDesign.CardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = resolvedContainerColor)
    ) {
        content()
    }
}

@Composable
private fun SettingsRowContent(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconContainer(icon = icon, tint = accentColor)

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        trailing()
    }
}

@Composable
fun SettingsIconContainer(
    icon: ImageVector,
    tint: Color
) {
    val isDarkTheme = isDarkSettingsTheme()
    val backgroundAlpha = if (isDarkTheme) 0.24f else 0.12f
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(tint.copy(alpha = backgroundAlpha), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SettingsGroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDarkSettingsTheme()) 0.48f else 0.42f)
    )
}

@Composable
private fun settingsCardContainerColor(): Color {
    return if (isDarkSettingsTheme()) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surface
    }
}

@Composable
private fun isDarkSettingsTheme(): Boolean {
    return MaterialTheme.colorScheme.background.luminance() < 0.5f
}

private fun Modifier.settingsCardShadow(): Modifier = composed {
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
