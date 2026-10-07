package com.nexe.pdfforge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexe.pdfforge.data.model.FileCategory
import com.nexe.pdfforge.data.model.PdfFileModel
import com.nexe.pdfforge.ui.components.ChipRow
import com.nexe.pdfforge.ui.components.FileItemCard
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.viewmodel.FileManagerViewModel
import com.nexe.pdfforge.ui.viewmodel.SortOption
import com.nexe.pdfforge.ui.viewmodel.visibleFiles
import com.nexe.pdfforge.util.FileUtils
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFilesScreen(viewModel: FileManagerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val visible = remember(state.files, state.query, state.sort) { state.visibleFiles() }
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(state.message) {
        val text = state.message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("My Files") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    label = { Text("Search files") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ChipRow(
                    items = SortOption.values().toList(),
                    selected = state.sort,
                    label = { "Sort: ${it.label}" },
                    onSelect = viewModel::setSort
                )
            }
            if (visible.isEmpty()) {
                item {
                    Text(
                        text = if (state.files.isEmpty()) {
                            "No files yet. Files you create with PDF Forge show up here."
                        } else {
                            "No files match your search."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FileCategory.values().forEach { category ->
                val group = visible.filter { it.category == category }
                if (group.isNotEmpty()) {
                    item(key = "header_${category.name}") { SectionHeader(category.label) }
                    items(group, key = { it.file.absolutePath }) { model ->
                        FileRow(
                            model = model,
                            dateText = dateFormat.format(Date(model.modified)),
                            onOpen = {
                                if (!FileUtils.openFile(context, model.file, FileUtils.mimeFor(model.file))) {
                                    viewModel.showMessage("No app found to open this file")
                                }
                            },
                            onShare = {
                                if (!FileUtils.shareFile(context, model.file, FileUtils.mimeFor(model.file))) {
                                    viewModel.showMessage("Couldn't open the share sheet")
                                }
                            },
                            onRename = { viewModel.askRename(model) },
                            onDelete = { viewModel.askDelete(model) }
                        )
                    }
                }
            }
        }
    }

    state.renameTarget?.let { target ->
        var text by remember(target) { mutableStateOf(target.file.nameWithoutExtension) }
        AlertDialog(
            onDismissRequest = { viewModel.askRename(null) },
            title = { Text("Rename file") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.rename(text) }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.askRename(null) }) { Text("Cancel") }
            }
        )
    }

    state.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { viewModel.askDelete(null) },
            title = { Text("Delete file?") },
            text = { Text("${target.name} will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.askDelete(null) }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FileRow(
    model: PdfFileModel,
    dateText: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    FileItemCard(
        title = model.name,
        subtitle = "${FileUtils.formatSize(model.sizeBytes)} \u2022 $dateText",
        icon = iconFor(model.category),
        onClick = onOpen,
        trailing = {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        onClick = { expanded = false; onOpen() }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { expanded = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { expanded = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { expanded = false; onDelete() }
                    )
                }
            }
        }
    )
}

private fun iconFor(category: FileCategory): ImageVector = when (category) {
    FileCategory.PDF -> Icons.Rounded.PictureAsPdf
    FileCategory.DOCUMENT -> Icons.Rounded.Description
    FileCategory.IMAGE -> Icons.Rounded.Image
    FileCategory.OTHER -> Icons.Rounded.AttachFile
}
