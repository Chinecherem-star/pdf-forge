package com.nexe.pdfforge.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexe.pdfforge.data.model.EditPageItem
import com.nexe.pdfforge.data.model.Overlay
import com.nexe.pdfforge.data.model.OverlayColor
import com.nexe.pdfforge.ui.components.ChipRow
import com.nexe.pdfforge.ui.components.FormatChip
import com.nexe.pdfforge.ui.components.PdfPreviewCard
import com.nexe.pdfforge.ui.components.ResultActions
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.components.ToolScaffold
import com.nexe.pdfforge.ui.viewmodel.PdfEditorViewModel
import kotlin.math.roundToInt

private enum class EditTool(val label: String) {
    TEXT("Add text"),
    IMAGE("Add image"),
    WATERMARK("Watermark")
}

@Composable
fun PdfEditorScreen(
    onBack: () -> Unit,
    viewModel: PdfEditorViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.consumeHandoff() }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.onPdfPicked(uri) }

    var pendingImage by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) pendingImage = uri }

    var tool by rememberSaveable { mutableStateOf(EditTool.TEXT) }
    var textInput by rememberSaveable { mutableStateOf("") }
    var sizePt by rememberSaveable { mutableStateOf(14) }
    var color by rememberSaveable { mutableStateOf(OverlayColor.BLACK) }
    var xPct by rememberSaveable { mutableStateOf(50f) }
    var yPct by rememberSaveable { mutableStateOf(50f) }
    var widthPct by rememberSaveable { mutableStateOf(30f) }
    var pageText by rememberSaveable { mutableStateOf("") }

    ToolScaffold(
        title = "PDF Editor",
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::consumeMessage
    ) {
        OutlinedButton(
            onClick = { pdfPicker.launch("application/pdf") },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.source == null) "Choose a PDF" else "Choose a different PDF") }

        val source = state.source
        if (source == null) {
            Text(
                text = "Reorder, rotate or delete pages, then add text, images, a watermark or page numbers. Existing text inside a PDF can't be changed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "${source.name} \u2022 ${state.pages.size} pages",
                style = MaterialTheme.typography.titleMedium
            )

            SectionHeader("Pages")
            state.pages.forEachIndexed { index, page ->
                key(page.id) {
                    PageRow(
                        index = index,
                        total = state.pages.size,
                        page = page,
                        onUp = { viewModel.movePage(index, -1) },
                        onDown = { viewModel.movePage(index, 1) },
                        onRotate = { viewModel.rotatePage(index) },
                        onDelete = { viewModel.deletePage(index) }
                    )
                }
            }

            SectionHeader("Add to pages")
            ChipRow(
                items = EditTool.values().toList(),
                selected = tool,
                label = { it.label },
                onSelect = { tool = it }
            )

            when (tool) {
                EditTool.TEXT -> {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Text to add") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChipRow(
                        items = listOf(10, 14, 20, 32, 48),
                        selected = sizePt,
                        label = { "$it pt" },
                        onSelect = { sizePt = it }
                    )
                    ChipRow(
                        items = OverlayColor.values().toList(),
                        selected = color,
                        label = { it.label },
                        onSelect = { color = it }
                    )
                    PercentSlider("Horizontal position", xPct) { xPct = it }
                    PercentSlider("Vertical position", yPct) { yPct = it }
                    PageField(pageText) { pageText = it }
                    Button(
                        onClick = { viewModel.addText(textInput, sizePt, color, xPct, yPct, pageText) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add text") }
                }
                EditTool.IMAGE -> {
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (pendingImage == null) "Choose an image or signature" else "Image selected (tap to change)")
                    }
                    PercentSlider("Width (of the page)", widthPct, 5f..100f) { widthPct = it }
                    PercentSlider("Horizontal position", xPct) { xPct = it }
                    PercentSlider("Vertical position", yPct) { yPct = it }
                    PageField(pageText) { pageText = it }
                    Button(
                        onClick = {
                            val uri = pendingImage
                            if (uri != null) {
                                viewModel.addPicture(uri, widthPct, xPct, yPct, pageText)
                                pendingImage = null
                            }
                        },
                        enabled = pendingImage != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add image") }
                }
                EditTool.WATERMARK -> {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Watermark text, for example CONFIDENTIAL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChipRow(
                        items = OverlayColor.values().toList(),
                        selected = color,
                        label = { it.label },
                        onSelect = { color = it }
                    )
                    PageField(pageText) { pageText = it }
                    Button(
                        onClick = { viewModel.addWatermark(textInput, color, pageText) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add watermark") }
                }
            }

            Row {
                FormatChip(
                    label = "Add page numbers",
                    selected = state.pageNumbers,
                    onClick = { viewModel.setPageNumbers(!state.pageNumbers) }
                )
            }

            if (state.overlays.isNotEmpty()) {
                SectionHeader("Additions")
                state.overlays.forEachIndexed { index, overlay ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = describe(overlay),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.removeOverlay(index) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save edited PDF")
                }
            }

            val file = state.resultFile
            val preview = state.previewBitmap
            if (file != null && preview != null) {
                SectionHeader("Result")
                PdfPreviewCard(bitmap = preview)
                ResultActions(file = file, mime = "application/pdf", onMessage = viewModel::showMessage)
                Text(
                    text = "Text and images you add are drawn onto the page as pictures, so they can't be selected or searched.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PageRow(
    index: Int,
    total: Int,
    page: EditPageItem,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRotate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                val thumbnail = page.thumbnail
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(64.dp)
                            .rotate(page.rotation.toFloat())
                    )
                } else {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text("Page ${index + 1}", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Original page ${page.originalIndex + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onUp, enabled = index > 0, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onDown, enabled = index < total - 1, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down")
            }
            IconButton(onClick = onRotate, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate")
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete page")
            }
        }
    }
}

@Composable
private fun PercentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    onChange: (Float) -> Unit
) {
    Column {
        Text("$label: ${value.roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun PageField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() }) },
        label = { Text("Page number (blank = all pages)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun describe(overlay: Overlay): String {
    val where = if (overlay.page == 0) "all pages" else "page ${overlay.page}"
    return when (overlay) {
        is Overlay.Text -> "Text: ${overlay.text.take(24)} (on $where)"
        is Overlay.Picture -> "Image (on $where)"
        is Overlay.Watermark -> "Watermark: ${overlay.text.take(24)} (on $where)"
    }
}
