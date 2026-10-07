package com.nexe.pdfforge.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexe.pdfforge.ui.components.FileItemCard
import com.nexe.pdfforge.ui.components.PdfPreviewCard
import com.nexe.pdfforge.ui.components.ResultActions
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.components.ToolScaffold
import com.nexe.pdfforge.ui.viewmodel.MergePdfViewModel
import com.nexe.pdfforge.util.FileUtils

@Composable
fun MergePdfScreen(
    onBack: () -> Unit,
    viewModel: MergePdfViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> viewModel.addPdfs(uris) }

    ToolScaffold(
        title = "Merge PDF",
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::consumeMessage
    ) {
        OutlinedButton(
            onClick = { pickLauncher.launch("application/pdf") },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Add PDFs") }

        if (state.entries.isEmpty()) {
            Text(
                text = "Add two or more PDFs, put them in the order you want, then merge.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.entries.forEachIndexed { index, entry ->
            key(entry.id) {
                FileItemCard(
                    title = "${index + 1}. ${entry.name}",
                    subtitle = "${entry.pageCount} pages \u2022 ${FileUtils.formatSize(entry.sizeBytes)}",
                    icon = Icons.Rounded.PictureAsPdf,
                    onClick = {},
                    trailing = {
                        IconButton(
                            onClick = { viewModel.move(index, -1) },
                            enabled = index > 0,
                            modifier = Modifier.size(40.dp)
                        ) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up") }
                        IconButton(
                            onClick = { viewModel.move(index, 1) },
                            enabled = index < state.entries.size - 1,
                            modifier = Modifier.size(40.dp)
                        ) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down") }
                        IconButton(
                            onClick = { viewModel.remove(index) },
                            modifier = Modifier.size(40.dp)
                        ) { Icon(Icons.Rounded.Delete, contentDescription = "Remove") }
                    }
                )
            }
        }

        Button(
            onClick = viewModel::merge,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Merge PDFs")
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
