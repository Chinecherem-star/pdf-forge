package com.nexe.pdfforge.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nexe.pdfforge.data.remote.RemoteConfig
import com.nexe.pdfforge.ui.components.AuroraBackground
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.components.ToolCard
import com.nexe.pdfforge.ui.navigation.Screen
import com.nexe.pdfforge.ui.navigation.toolSections
import com.nexe.pdfforge.ui.theme.Cyan
import com.nexe.pdfforge.ui.theme.Violet
import kotlin.math.min

@Composable
fun HomeScreen(
    config: RemoteConfig,
    onToolClick: (Screen) -> Unit
) {
    var offset = 0
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroCard()
        }
        if (config.announcementEnabled && config.announcementMessage.isNotBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AnnouncementCard(
                    title = config.announcementTitle,
                    message = config.announcementMessage
                )
            }
        }
        toolSections.forEach { (sectionTitle, tools) ->
            val base = offset
            offset += tools.size
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = sectionTitle,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            itemsIndexed(tools, key = { _, tool -> tool.route }) { index, tool ->
                val disabled = tool.route in config.disabledTools
                ToolCard(
                    title = tool.title,
                    subtitle = if (disabled) "Temporarily unavailable" else tool.subtitle,
                    icon = tool.icon,
                    accentStart = tool.accentStart,
                    accentEnd = tool.accentEnd,
                    enabled = !disabled,
                    entranceDelayMs = min((base + index) * 70, 700),
                    onClick = { onToolClick(tool) }
                )
            }
        }
    }
}

@Composable
private fun HeroCard() {
    val transition = rememberInfiniteTransition(label = "hero")
    val bob by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "bob"
    )
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = Violet.copy(alpha = 0.4f),
                spotColor = Cyan.copy(alpha = 0.4f)
            )
    ) {
        AuroraBackground(modifier = Modifier.matchParentSize())
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPill("Offline  \u2022  Private")
                Text(
                    text = "PDF Forge",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                Text(
                    text = "Create, edit, convert and protect your documents.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer { translationY = bob * density }
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassPill(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun AnnouncementCard(title: String, message: String) {
    var dismissed by rememberSaveable(title, message) { mutableStateOf(false) }
    if (dismissed) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.Campaign, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                if (title.isNotBlank()) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                }
                Text(message, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { dismissed = true }) { Text("Dismiss") }
            }
        }
    }
}
