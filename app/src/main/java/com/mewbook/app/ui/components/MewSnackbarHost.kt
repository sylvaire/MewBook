package com.mewbook.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mewbook.app.ui.theme.ClayDesign
import com.mewbook.app.ui.theme.ExpenseRed
import com.mewbook.app.ui.theme.IncomeGreen
import com.mewbook.app.ui.theme.clayShadow

@Composable
fun MewSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    ) { snackbarData ->
        MewSnackbar(snackbarData = snackbarData)
    }
}

@Composable
private fun MewSnackbar(
    snackbarData: SnackbarData
) {
    val message = snackbarData.visuals.message
    val style = snackbarStyleFor(message)
    val (title, subtitle) = splitSnackbarMessage(message)
    val snackbarShape = RoundedCornerShape(ClayDesign.ButtonRadius)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clayShadow(
                elevation = ClayDesign.CardShadowElevation1,
                shape = snackbarShape,
                alpha = 0.10f
            )
            .clayShadow(
                elevation = ClayDesign.CardShadowElevation2,
                shape = snackbarShape,
                alpha = 0.06f
            ),
        shape = snackbarShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = style.accentColor.copy(alpha = 0.36f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(style.accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            snackbarData.visuals.actionLabel?.let { actionLabel ->
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = snackbarData::performAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun snackbarStyleFor(message: String): SnackbarStyle {
    return when {
        message.contains("失败") || message.contains("不存在") || message.contains("过期") -> SnackbarStyle(
            icon = Icons.Filled.ErrorOutline,
            accentColor = MaterialTheme.colorScheme.error
        )

        message.contains("永久删除") -> SnackbarStyle(
            icon = Icons.Filled.DeleteForever,
            accentColor = ExpenseRed
        )

        message.contains("回收站") || message.contains("恢复") -> SnackbarStyle(
            icon = Icons.Filled.Restore,
            accentColor = MaterialTheme.colorScheme.primary
        )

        else -> SnackbarStyle(
            icon = Icons.Filled.CheckCircle,
            accentColor = IncomeGreen
        )
    }
}

private data class SnackbarStyle(
    val icon: ImageVector,
    val accentColor: Color
)

private fun splitSnackbarMessage(message: String): Pair<String, String?> {
    val separatorIndex = message.indexOf('，')
    if (separatorIndex <= 0 || separatorIndex == message.lastIndex) {
        return message to null
    }
    return message.substring(0, separatorIndex) to message.substring(separatorIndex + 1)
}
