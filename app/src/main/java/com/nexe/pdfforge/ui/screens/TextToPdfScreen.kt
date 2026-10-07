package com.nexe.pdfforge.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexe.pdfforge.data.model.ListStyle
import com.nexe.pdfforge.data.model.MarginOption
import com.nexe.pdfforge.data.model.PageSizeOption
import com.nexe.pdfforge.data.model.TextAlignOption
import com.nexe.pdfforge.ui.components.FormatChip
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.viewmodel.TextToPdfViewModel
import com.nexe.pdfforge.util.FileUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToPdfScreen(
    onBack: () -> Unit,
    viewModel: TextToPdfViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val options = state.options

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val file = state.generatedFile
        if (uri != null && file != null) {
            val ok = FileUtils.copyToUri(context, file, uri)
            viewModel.showMessage(if (ok) "Saved" else "Couldn't save to that location")
        }
    }

    LaunchedEffect(state.message) {
        val text = state.message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Text to PDF") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = options.title,
                onValueChange = { v -> viewModel.updateOptions { it.copy(title = v) } },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = options.body,
                onValueChange = { v -> viewModel.updateOptions { it.copy(body = v) } },
                label = { Text("Write your text here") },
                minLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = viewModel::insertPageBreak,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Insert page break at end")
            }

            SectionHeader("Style")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormatChip("Bold", options.bold, { viewModel.updateOptions { it.copy(bold = !it.bold) } })
                FormatChip("Italic", options.italic, { viewModel.updateOptions { it.copy(italic = !it.italic) } })
                FormatChip("Underline", options.underline, { viewModel.updateOptions { it.copy(underline = !it.underline) } })
            }

            Text("Font size: ${options.fontSize} pt", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = options.fontSize.toFloat(),
                onValueChange = { v -> viewModel.updateOptions { it.copy(fontSize = v.roundToInt()) } },
                valueRange = 8f..36f,
                steps = 27
            )

            OptionChips(
                items = TextAlignOption.values().toList(),
                selected = options.align,
                label = { it.label },
                onSelect = { v -> viewModel.updateOptions { it.copy(align = v) } }
            )
            OptionChips(
                items = ListStyle.values().toList(),
                selected = options.listStyle,
                label = { it.label },
                onSelect = { v -> viewModel.updateOptions { it.copy(listStyle = v) } }
            )

            SectionHeader("Page")
            OptionChips(
                items = PageSizeOption.values().toList(),
                selected = options.pageSize,
                label = { it.label },
                onSelect = { v -> viewModel.updateOptions { it.copy(pageSize = v) } }
            )
            OptionChips(
                items = listOf(false, true),
                selected = options.landscape,
                label = { if (it) "Landscape" else "Portrait" },
                onSelect = { v -> viewModel.updateOptions { it.copy(landscape = v) } }
            )
            OptionChips(
                items = MarginOption.values().toList(),
                selected = options.margin,
                label = { "${it.label} margins" },
                onSelect = { v -> viewModel.updateOptions { it.copy(margin = v) } }
            )

            Button(
                onClick = viewModel::generate,
                enabled = !state.isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Generate PDF")
                }
            }

            val file = state.generatedFile
            val preview = state.previewBitmap
            if (file != null && preview != null) {
                SectionHeader("Preview")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = "PDF preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!FileUtils.openFile(context, file, "application/pdf")) {
                                viewModel.showMessage("No app found to open PDFs")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Open") }
                    OutlinedButton(
                        onClick = {
                            if (!FileUtils.shareFile(context, file, "application/pdf")) {
                                viewModel.showMessage("Couldn't open the share sheet")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Share") }
                    OutlinedButton(
                        onClick = { saveLauncher.launch(file.name) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save as") }
                }
                Text(
                    text = "Saved in the app's Documents folder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun <T> OptionChips(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            FormatChip(
                label = label(item),
                selected = item == selected,
                onClick = { onSelect(item) }
            )
        }
    }
}
