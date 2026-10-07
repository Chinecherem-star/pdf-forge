package com.nexe.pdfforge.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nexe.pdfforge.data.model.ImageFit
import com.nexe.pdfforge.data.model.ImageMargin
import com.nexe.pdfforge.data.model.PageSizeOption
import com.nexe.pdfforge.ui.components.ChipRow
import com.nexe.pdfforge.ui.components.FormatChip
import com.nexe.pdfforge.ui.components.PdfPreviewCard
import com.nexe.pdfforge.ui.components.ResultActions
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.components.ToolScaffold
import com.nexe.pdfforge.ui.viewmodel.ImageToPdfViewModel
import com.nexe.pdfforge.util.FileUtils

@Composable
fun ImageToPdfScreen(
    onBack: () -> Unit,
    viewModel: ImageToPdfViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> viewModel.addImages(uris) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) viewModel.addImages(listOf(uri))
        pendingCameraUri = null
    }

    ToolScaffold(
        title = "Image to PDF",
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::consumeMessage
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { pickLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) { Text("Choose images") }
            OutlinedButton(
                onClick = {
                    try {
                        val file = FileUtils.newCameraFile(context)
                        val uri = FileUtils.uriFor(context, file)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        viewModel.showMessage("No camera app available")
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Take photo") }
        }

        if (state.images.isEmpty()) {
            Text(
                text = "Pick one or more photos, or take a new one. You can reorder and rotate them before creating the PDF.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.images.forEachIndexed { index, item ->
            key(item.id) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .rotate(item.rotation.toFloat())
                        )
                        Text(
                            text = "Page ${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        )
                        IconButton(
                            onClick = { viewModel.move(index, -1) },
                            enabled = index > 0,
                            modifier = Modifier.size(40.dp)
                        ) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up") }
                        IconButton(
                            onClick = { viewModel.move(index, 1) },
                            enabled = index < state.images.size - 1,
                            modifier = Modifier.size(40.dp)
                        ) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down") }
                        IconButton(
                            onClick = { viewModel.rotate(index) },
                            modifier = Modifier.size(40.dp)
                        ) { Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate") }
                        IconButton(
                            onClick = { viewModel.remove(index) },
                            modifier = Modifier.size(40.dp)
                        ) { Icon(Icons.Rounded.Delete, contentDescription = "Remove") }
                    }
                }
            }
        }

        val options = state.options
        SectionHeader("Options")
        ChipRow(
            items = ImageFit.values().toList(),
            selected = options.fit,
            label = { it.label },
            onSelect = { v -> viewModel.updateOptions { it.copy(fit = v) } }
        )
        if (options.fit != ImageFit.MATCH) {
            ChipRow(
                items = PageSizeOption.values().toList(),
                selected = options.pageSize,
                label = { it.label },
                onSelect = { v -> viewModel.updateOptions { it.copy(pageSize = v) } }
            )
            ChipRow(
                items = ImageMargin.values().toList(),
                selected = options.margin,
                label = { it.label },
                onSelect = { v -> viewModel.updateOptions { it.copy(margin = v) } }
            )
            Row {
                FormatChip(
                    label = "Auto orientation",
                    selected = options.autoOrientation,
                    onClick = { viewModel.updateOptions { it.copy(autoOrientation = !it.autoOrientation) } }
                )
            }
        }

        Button(
            onClick = viewModel::generate,
            enabled = !state.isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Create PDF")
            }
        }

        val file = state.generatedFile
        val preview = state.previewBitmap
        if (file != null && preview != null) {
            SectionHeader("Preview")
            PdfPreviewCard(bitmap = preview)
            ResultActions(file = file, mime = "application/pdf", onMessage = viewModel::showMessage)
        }
    }
}
