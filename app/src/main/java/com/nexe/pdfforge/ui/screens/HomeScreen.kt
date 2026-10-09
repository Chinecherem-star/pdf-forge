package com.nexe.pdfforge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexe.pdfforge.data.remote.RemoteConfig
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.components.ToolCard
import com.nexe.pdfforge.ui.navigation.Screen
import com.nexe.pdfforge.ui.navigation.toolSections

@Composable
fun HomeScreen(
    config: RemoteConfig,
    onToolClick: (Screen) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = sectionTitle,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(tools, key = { it.route }) { tool ->
                val disabled = tool.route in config.disabledTools
                ToolCard(
                    title = tool.title,
                    subtitle = if (disabled) "Temporarily unavailable" else tool.subtitle,
                    icon = tool.icon,
                    enabled = !disabled,
                    onClick = { onToolClick(tool) }
                )
            }
        }
    }
}

@Composable
private fun HeroCard() {
    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(brush)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "PDF Forge",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Text(
            text = "Create, convert and manage PDFs. Your files stay on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun AnnouncementCard(title: String, message: String) {
    var dismissed by rememberSaveable(title, message) { mutableStateOf(false) }
    if (dismissed) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
