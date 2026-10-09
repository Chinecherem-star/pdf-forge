package com.nexe.pdfforge.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tool tile with a gradient icon, coloured glow, spring press effect and a staggered entrance.
 */
@Composable
fun ToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentStart: Color,
    accentEnd: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    entranceDelayMs: Int = 0
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardPress"
    )
    val fade = remember { Animatable(0f) }
    val rise = remember { Animatable(28f) }
    LaunchedEffect(Unit) {
        delay(entranceDelayMs.toLong())
        launch { fade.animateTo(1f, tween(500)) }
        rise.animateTo(0f, tween(550, easing = FastOutSlowInEasing))
    }

    val shape = RoundedCornerShape(26.dp)
    Card(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp)
            .graphicsLayer {
                alpha = fade.value * (if (enabled) 1f else 0.5f)
                translationY = rise.value * density
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = accentStart.copy(alpha = 0.25f),
                spotColor = accentEnd.copy(alpha = 0.35f)
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    accentStart.copy(alpha = 0.45f),
                    Color.Transparent,
                    accentEnd.copy(alpha = 0.35f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(accentStart, accentEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.size(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
        }
    }
}
