package com.nexe.pdfforge.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nexe.pdfforge.data.model.ExportImageFormat
import com.nexe.pdfforge.data.model.ExportQuality
import com.nexe.pdfforge.ui.components.ChipRow
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.components.ToolScaffold
import com.nexe.pdfforge.ui.viewmodel.PdfToImageViewModel
import com.nexe.pdfforge.util.FileUtils

@Composable
fun PdfToImageScreen(
    onBack: () -> Unit,
    viewModel: PdfToImageViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.onPdfPicked(uri) }

    ToolScaffold(
        title = "PDF to Image",
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::consumeMessage
    ) {
        OutlinedButton(
            onClick = { pickLauncher.launch("application/pdf") },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.source == null) "Choose a PDF" else "Choose a different PDF") }

        val source = state.source
        if (source == null) {
            Text(
                text = "Choose a PDF, then export all pages or just the ones you pick as PNG or JPG images.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(source.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${source.pageCount} pages \u2022 ${FileUtils.formatSize(source.sizeBytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionHeader("Pages")
            ChipRow(
                items = listOf(true, false),
                selected = state.allPages,
                label = { if (it) "All pages" else "Select pages" },
                onSelect = viewModel::setAllPages
            )
            if (!state.allPages) {
                OutlinedTextField(
                    value = state.rangeText,
                    onValueChange = viewModel::setRange,
                    label = { Text("For example 1-3, 5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionHeader("Format and quality")
            ChipRow(
                items = ExportImageFormat.values().toList(),
                selected = state.format,
                label = { it.label },
                onSelect = viewModel::setFormat
            )
            ChipRow(
                items = ExportQuality.values().toList(),
                selected = state.quality,
                label = { it.label },
                onSelect = viewModel::setQuality
            )

            Button(
                onClick = viewModel::export,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Export images")
                }
            }
        }

        if (state.results.isNotEmpty()) {
            SectionHeader("Exported images")
            state.results.forEach { file ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = file,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )
                        TextButton(onClick = {
                            if (!FileUtils.openFile(context, file, FileUtils.mimeFor(file))) {
                                viewModel.showMessage("No app found to open images")
                            }
                        }) { Text("Open") }
                        TextButton(onClick = {
                            if (!FileUtils.shareFile(context, file, FileUtils.mimeFor(file))) {
                                viewModel.showMessage("Couldn't open the share sheet")
                            }
                        }) { Text("Share") }
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    if (!FileUtils.shareFiles(context, state.results, "image/*")) {
                        viewModel.showMessage("Couldn't open the share sheet")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Share all") }
            Text(
                text = "Images are saved in the app's Documents folder and listed in My Files.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
