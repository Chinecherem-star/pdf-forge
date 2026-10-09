package com.nexe.pdfforge.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexe.pdfforge.data.model.ConversionFormat
import com.nexe.pdfforge.ui.components.ChipRow
import com.nexe.pdfforge.ui.components.ResultActions
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.components.ToolScaffold
import com.nexe.pdfforge.ui.viewmodel.WordEditorViewModel
import com.nexe.pdfforge.util.FileUtils

@Composable
fun WordEditorScreen(
    onBack: () -> Unit,
    viewModel: WordEditorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.consumeHandoff() }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.onFilePicked(uri) }

    ToolScaffold(
        title = "Word Editor",
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::consumeMessage
    ) {
        OutlinedButton(
            onClick = { pickLauncher.launch("*/*") },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Open a .docx file")
            }
        }

        if (state.fileName.isNotBlank()) {
            Text(
                text = "Editing: ${state.fileName}",
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            Text(
                text = "Open a Word (.docx) file to edit its text, or just type below to write a new document.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = state.body,
            onValueChange = viewModel::setBody,
            label = { Text("Document text (one paragraph per line)") },
            minLines = 14,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Only the text is kept. Fonts, images, tables and other formatting from the original file are not carried over. A line that only says [page-break] starts a new page.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionHeader("Save as")
        ChipRow(
            items = ConversionFormat.values().toList(),
            selected = state.format,
            label = { it.label },
            onSelect = viewModel::setFormat
        )

        Button(
            onClick = viewModel::save,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Save")
            }
        }

        val file = state.resultFile
        if (file != null) {
            SectionHeader("Result")
            Text(
                text = "${file.name} \u2022 ${FileUtils.formatSize(file.length())}",
                style = MaterialTheme.typography.bodyMedium
            )
            ResultActions(file = file, mime = state.format.mime, onMessage = viewModel::showMessage)
        }
    }
}
