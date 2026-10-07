package com.nexe.pdfforge.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexe.pdfforge.ui.components.PdfPreviewCard
import com.nexe.pdfforge.ui.components.ResultActions
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.components.ToolScaffold
import com.nexe.pdfforge.ui.viewmodel.SplitPdfViewModel
import com.nexe.pdfforge.util.FileUtils

@Composable
fun SplitPdfScreen(
    onBack: () -> Unit,
    viewModel: SplitPdfViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.onPdfPicked(uri) }

    ToolScaffold(
        title = "Split PDF",
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
                text = "Choose a PDF, then enter the pages you want to keep as a new PDF.",
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

            SectionHeader("Pages to keep")
            OutlinedTextField(
                value = state.rangeText,
                onValueChange = viewModel::setRange,
                label = { Text("For example 1-3, 5, 8-10") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::split,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create new PDF")
                }
            }
        }

        val file = state.resultFile
        val preview = state.previewBitmap
        if (file != null && preview != null) {
            SectionHeader("Result")
            PdfPreviewCard(bitmap = preview)
            ResultActions(file = file, mime = "application/pdf", onMessage = viewModel::showMessage)
        }
    }
}
