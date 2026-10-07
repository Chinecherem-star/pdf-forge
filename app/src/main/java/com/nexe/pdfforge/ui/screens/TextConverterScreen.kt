package com.nexe.pdfforge.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.nexe.pdfforge.ui.viewmodel.TextConverterViewModel
import com.nexe.pdfforge.util.FileUtils

@Composable
fun TextConverterScreen(
    onBack: () -> Unit,
    viewModel: TextConverterViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ToolScaffold(
        title = "Text Converter",
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::consumeMessage
    ) {
        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::setTitle,
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.body,
            onValueChange = viewModel::setBody,
            label = { Text("Paste or write your text") },
            minLines = 8,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Tip: a line that only says [page-break] starts a new page in PDF and DOCX.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionHeader("Convert to")
        ChipRow(
            items = ConversionFormat.values().toList(),
            selected = state.format,
            label = { it.label },
            onSelect = viewModel::setFormat
        )

        Button(
            onClick = viewModel::convert,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Convert")
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
