package com.nexe.pdfforge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
        topBar = { TopAppBar(title = { Text("Settings") }) }
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

            SectionHeader("Privacy")
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PDF Forge works fully offline. Your documents, images and text are processed on your device and are never uploaded. The app doesn't ask for internet access, and it only reads files you choose yourself.",
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
