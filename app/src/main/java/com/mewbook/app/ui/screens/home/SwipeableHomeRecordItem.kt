package com.mewbook.app.ui.screens.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mewbook.app.domain.model.Record
import com.mewbook.app.domain.policy.HapticFeedbackPolicy
import com.mewbook.app.ui.components.RecordItem
import com.mewbook.app.ui.components.rememberMewHapticFeedback
import com.mewbook.app.ui.theme.ClayDesign
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class SwipeAnchor {
    Closed,
    Open
}

private val ActionRevealWidth = 144.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SwipeableHomeRecordItem(
    record: Record,
    categoryName: String,
    categoryIcon: String,
    categoryColor: Long,
    isOpen: Boolean,
    onRequestOpen: () -> Unit,
    onRequestClose: () -> Unit,
    onClick: () -> Unit,
    onEdit: (Record) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    hapticFeedbackEnabled: Boolean = true
) {
    val density = LocalDensity.current
    val revealWidthPx = with(density) { ActionRevealWidth.toPx() }
    val hapticFeedback = rememberMewHapticFeedback(hapticFeedbackEnabled)
    val dragState = remember(density) {
        AnchoredDraggableState(
            initialValue = SwipeAnchor.Closed,
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    val anchors = remember(revealWidthPx) {
        DraggableAnchors {
            SwipeAnchor.Closed at 0f
            SwipeAnchor.Open at -revealWidthPx
        }
    }

    LaunchedEffect(anchors) {
        dragState.updateAnchors(anchors)
    }

    LaunchedEffect(isOpen) {
        val target = if (isOpen) SwipeAnchor.Open else SwipeAnchor.Closed
        if (dragState.targetValue != target) {
            dragState.animateTo(target)
        }
    }

    LaunchedEffect(dragState) {
        snapshotFlow { dragState.currentValue }
            .distinctUntilChanged()
            .collect { anchor ->
                when (anchor) {
                    SwipeAnchor.Open -> onRequestOpen()
                    SwipeAnchor.Closed -> onRequestClose()
                }
            }
    }

    val horizontalOffset by remember(dragState) {
        derivedStateOf {
            dragState.offset.takeUnless(Float::isNaN) ?: 0f
        }
    }
    val revealProgress by remember(dragState, revealWidthPx) {
        derivedStateOf {
            (abs(horizontalOffset) / revealWidthPx).coerceIn(0f, 1f)
        }
    }

    Box(modifier = modifier) {
        SwipeActionBackground(
            revealProgress = revealProgress,
            onEdit = {
                onRequestClose()
                hapticFeedback.perform(HapticFeedbackPolicy.Interaction.Selection)
                onEdit(record)
            },
            onDelete = {
                onRequestClose()
                hapticFeedback.perform(HapticFeedbackPolicy.Interaction.DialogAction)
                onDelete(record.id)
            },
            modifier = Modifier.matchParentSize()
        )

        RecordItem(
            record = record,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            categoryColor = categoryColor,
            onClick = {
                if (isOpen || dragState.currentValue == SwipeAnchor.Open) {
                    onRequestClose()
                } else {
                    onClick()
                }
            },
            modifier = Modifier
                .testTag("home-record-card-${record.id}")
                .offset { IntOffset(horizontalOffset.roundToInt(), 0) }
                .anchoredDraggable(
                    state = dragState,
                    orientation = Orientation.Horizontal
                ),
            hapticFeedbackEnabled = hapticFeedbackEnabled
        )
    }
}

@Composable
private fun SwipeActionBackground(
    revealProgress: Float,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(ClayDesign.CardRadius)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            modifier = Modifier
                .width(ActionRevealWidth)
                .fillMaxHeight()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SwipeActionButton(
                label = "编辑",
                contentDescription = "编辑这条记录",
                onClick = onEdit,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
                revealProgress = revealProgress
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            SwipeActionButton(
                label = "删除",
                contentDescription = "删除这条记录",
                onClick = onDelete,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
                revealProgress = revealProgress
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SwipeActionButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    revealProgress: Float,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                alpha = 0.35f + (0.65f * revealProgress)
                scaleX = 0.92f + (0.08f * revealProgress)
                scaleY = 0.92f + (0.08f * revealProgress)
            }
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        shape = RoundedCornerShape(ClayDesign.ButtonRadius),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
