package com.nexe.pdfforge.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexe.pdfforge.ui.components.ToolScaffold
import com.nexe.pdfforge.ui.viewmodel.PdfInfoViewModel
import com.nexe.pdfforge.util.FileUtils
import java.text.DateFormat
import java.util.Date

@Composable
fun PdfInfoScreen(
    onBack: () -> Unit,
    viewModel: PdfInfoViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.onPdfPicked(uri) }

    ToolScaffold(
        title = "PDF Info",
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::consumeMessage
    ) {
        OutlinedButton(
            onClick = { pickLauncher.launch("application/pdf") },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (state.info == null) "Choose a PDF" else "Choose a different PDF")
            }
        }

        val info = state.info
        if (info == null) {
            Text(
                text = "Choose a PDF to see its size, page count and metadata.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoRow("File name", info.name)
                    InfoRow("Size", FileUtils.formatSize(info.sizeBytes))
                    InfoRow("Pages", info.pageCount.toString())
                    info.firstPageSize?.let { InfoRow("Page size", it) }
                    InfoRow("PDF version", info.pdfVersion)
                    InfoRow("Encrypted", if (info.encrypted) "Yes" else "No")
                    info.title?.let { InfoRow("Title", it) }
                    info.author?.let { InfoRow("Author", it) }
                    info.subject?.let { InfoRow("Subject", it) }
                    info.keywords?.let { InfoRow("Keywords", it) }
                    info.creator?.let { InfoRow("Creator", it) }
                    info.producer?.let { InfoRow("Producer", it) }
                    info.created?.let { InfoRow("Created", dateFormat.format(Date(it))) }
                    info.modified?.let { InfoRow("Modified", dateFormat.format(Date(it))) }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f)
        )
    }
}
