package com.mewbook.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mewbook.app.domain.model.Category

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(category.color)
    val backgroundColor = if (isSelected) {
        accentColor.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    }

    val borderColor = if (isSelected) {
        accentColor
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CategoryIconBadge(
                category = category,
                emphasized = isSelected,
                containerSize = 42.dp,
                iconSize = 22.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    accentColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryIconBadge(
    category: Category,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
    containerSize: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val accentColor = Color(category.color)
    val backgroundColor = if (emphasized) {
        accentColor.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val iconTint = if (emphasized) {
        accentColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
    }

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getIconForCategory(category.icon),
            contentDescription = category.name,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
