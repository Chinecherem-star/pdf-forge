package com.nexe.pdfforge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexe.pdfforge.ui.navigation.Screen
import com.nexe.pdfforge.ui.theme.Cyan
import com.nexe.pdfforge.ui.theme.Violet

/** Floating pill navigation. The selected item expands with a gradient and its label. */
@Composable
fun FloatingNavBar(
    items: List<Screen>,
    currentRoute: String?,
    onSelect: (Screen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f),
            shadowElevation = 18.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    NavItem(
                        item = item,
                        selected = item.route == currentRoute,
                        onClick = { onSelect(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(item: Screen, selected: Boolean, onClick: () -> Unit) {
    val brush = Brush.linearGradient(listOf(Violet, Cyan))
    val tint by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navTint"
    )
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .then(if (selected) Modifier.background(brush) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 18.dp else 14.dp, vertical = 12.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = item.icon, contentDescription = item.title, tint = tint)
        AnimatedVisibility(visible = selected) {
            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
