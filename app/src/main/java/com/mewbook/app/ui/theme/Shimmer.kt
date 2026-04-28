package com.mewbook.app.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * 微光闪烁动画笔刷，用于骨架屏加载占位。
 */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
    return Brush.linearGradient(
        colors = listOf(baseColor, shimmerColor, baseColor),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ClayDesign.CardRadius))
            .background(shimmerBrush())
    )
}

@Composable
fun RecordItemSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ClayDesign.CardRadius))
            .background(MaterialTheme.colorScheme.surface)
            .padding(ClayDesign.CardPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            ShimmerBox(modifier = Modifier.width(48.dp).height(48.dp).clip(RoundedCornerShape(14.dp)))
            Spacer(modifier = Modifier.width(ClayDesign.CardSpacing))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp))
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp))
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                ShimmerBox(modifier = Modifier.width(72.dp).height(20.dp))
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.width(48.dp).height(12.dp))
            }
        }
    }
}

@Composable
fun StatisticsChartSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(4) {
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
                if (it < 3) Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}
