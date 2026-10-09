package com.nexe.pdfforge.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Slowly drifting glow orbs on a deep gradient: the animated backdrop of the home hero. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val a by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orbA"
    )
    val b by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orbB"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF2A1FA6), Color(0xFF0B1450)),
                start = Offset.Zero,
                end = Offset(w, h)
            )
        )
        val first = Offset(w * (0.15f + 0.55f * a), h * (0.2f + 0.5f * b))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF22D3EE).copy(alpha = 0.55f), Color.Transparent),
                center = first,
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = first
        )
        val second = Offset(w * (0.85f - 0.5f * b), h * (0.8f - 0.5f * a))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF472B6).copy(alpha = 0.45f), Color.Transparent),
                center = second,
                radius = w * 0.5f
            ),
            radius = w * 0.5f,
            center = second
        )
    }
}
