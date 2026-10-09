package com.nexe.pdfforge.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexe.pdfforge.data.model.PageSizeOption
import com.nexe.pdfforge.data.model.ThemeMode
import com.nexe.pdfforge.ui.components.ChipRow
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.viewmodel.SettingsViewModel
import com.nexe.pdfforge.util.FileUtils

private const val CONTACT_EMAIL = "nexecreatives@gmail.com"
private const val PHONE_ONE = "+2348102347790"
private const val PHONE_TWO = "+2347039476839"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val versionName = remember {
        val name: String? = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        }
        name ?: "1.0.0"
    }
    val outputPath = remember { FileUtils.outputDir(context).absolutePath }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("Appearance")
            ChipRow(
                items = ThemeMode.values().toList(),
                selected = settings.themeMode,
                label = { "${it.label} theme" },
                onSelect = viewModel::setThemeMode
            )

            SectionHeader("Defaults for new documents")
            ChipRow(
                items = PageSizeOption.values().toList(),
                selected = settings.defaultPageSize,
                label = { it.label },
                onSelect = viewModel::setDefaultPageSize
            )
            ChipRow(
                items = listOf(false, true),
                selected = settings.defaultLandscape,
                label = { if (it) "Landscape" else "Portrait" },
                onSelect = viewModel::setDefaultLandscape
            )
            ChipRow(
                items = listOf(10, 12, 14, 16, 18),
                selected = settings.defaultFontSize,
                label = { "$it pt" },
                onSelect = viewModel::setDefaultFontSize
            )

            SectionHeader("Output folder")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = outputPath,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Everything you create is saved here and listed in My Files. Use \"Save as\" on any result to copy it to Downloads, Drive or any other folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            SectionHeader("Contact us")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ContactRow(
                        icon = Icons.Rounded.Email,
                        label = "Email",
                        value = CONTACT_EMAIL,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CONTACT_EMAIL"))
                                .putExtra(Intent.EXTRA_SUBJECT, "PDF Forge")
                            launch(context, intent)
                        }
                    )
                    ContactRow(
                        icon = Icons.Rounded.Call,
                        label = "Phone",
                        value = PHONE_ONE,
                        onClick = { launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$PHONE_ONE"))) },
                        actionLabel = "WhatsApp",
                        onAction = { launch(context, whatsAppIntent(PHONE_ONE)) }
                    )
                    ContactRow(
                        icon = Icons.Rounded.Call,
                        label = "Phone",
                        value = PHONE_TWO,
                        onClick = { launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$PHONE_TWO"))) },
                        actionLabel = "WhatsApp",
                        onAction = { launch(context, whatsAppIntent(PHONE_TWO)) }
                    )
                }
            }

            SectionHeader("Privacy")
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "All PDF Forge tools work offline. Your documents, images and text are processed on your device and are never uploaded. When you're online, the app checks for announcements and updates, and sends anonymous usage counts (which tool was opened and the app version). No files, text or personal details are ever sent.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            SectionHeader("About")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PDF Forge", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Built by NEXE Creatives",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = value, style = MaterialTheme.typography.titleMedium)
        }
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private fun whatsAppIntent(phone: String): Intent {
    val digits = phone.filter { it.isDigit() }
    return Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
}

private fun launch(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // no app can handle this action
    }
}
