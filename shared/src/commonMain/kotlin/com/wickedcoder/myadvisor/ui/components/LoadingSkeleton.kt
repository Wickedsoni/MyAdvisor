package com.wickedcoder.myadvisor.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.wickedcoder.myadvisor.ui.theme.MotionTokens

/**
 * A shimmering placeholder block. Compose a few of these (or use [ResultSkeleton])
 * while the recommendation engine runs, instead of a bare spinner — it signals the
 * *shape* of what's coming and reads as more polished. The shimmer sweeps a light
 * band across a neutral surface on an infinite loop.
 */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MotionTokens.DurationExtraLong * 2, easing = MotionTokens.standard),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val width = 600f
    val start = -width + progress * (width * 2)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(start, 0f),
        end = Offset(start + width, 0f),
    )
    androidx.compose.foundation.layout.Box(modifier = modifier.background(brush, shape))
}

/** A skeleton shaped like a recommendation result card, for the recommending state. */
@Composable
fun ResultSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LoadingSkeleton(modifier = Modifier.fillMaxWidth(0.6f).height(24.dp))
        LoadingSkeleton(modifier = Modifier.fillMaxWidth(0.9f).height(16.dp))
        LoadingSkeleton(modifier = Modifier.fillMaxWidth(0.4f).height(16.dp))
    }
}
