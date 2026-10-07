package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.PdfEntry
import com.nexe.pdfforge.data.repository.PdfRepository
import com.nexe.pdfforge.util.FileUtils
import com.nexe.pdfforge.util.PdfUtils
import com.nexe.pdfforge.util.friendlyMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class MergePdfUiState(
    val entries: List<PdfEntry> = emptyList(),
    val isBusy: Boolean = false,
    val resultFile: File? = null,
    val previewBitmap: Bitmap? = null,
    val message: String? = null
)

class MergePdfViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MergePdfUiState())
    val uiState: StateFlow<MergePdfUiState> = _uiState.asStateFlow()
    private var nextId = 0L

    fun addPdfs(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val (added, failed) = withContext(Dispatchers.IO) {
                val ok = mutableListOf<PdfEntry>()
                var bad = 0
                for (uri in uris) {
                    try {
                        val file = FileUtils.copyUriToCache(context, uri)
                        val pages = PdfUtils.pageCount(file)
                        ok.add(PdfEntry(nextId++, file, FileUtils.displayName(context, uri), pages, file.length()))
                    } catch (e: Throwable) {
                        bad++
                    }
                }
                ok to bad
            }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    entries = it.entries + added,
                    resultFile = null,
                    previewBitmap = null,
                    message = if (failed > 0) {
                        "$failed file(s) couldn't be read (corrupted or password protected)."
                    } else null
                )
            }
        }
    }

    fun move(index: Int, delta: Int) {
        _uiState.update { state ->
            val target = index + delta
            if (index !in state.entries.indices || target !in state.entries.indices) {
                state
            } else {
                val list = state.entries.toMutableList()
                val item = list.removeAt(index)
                list.add(target, item)
                state.copy(entries = list, resultFile = null, previewBitmap = null)
            }
        }
    }

    fun remove(index: Int) {
        _uiState.update { state ->
            state.copy(
                entries = state.entries.filterIndexed { i, _ -> i != index },
                resultFile = null,
                previewBitmap = null
            )
        }
    }

    fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun merge() {
        val state = _uiState.value
        if (state.isBusy) return
        if (state.entries.size < 2) {
            showMessage("Add at least two PDFs to merge.")
            return
        }
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val (file, preview, pages) = withContext(Dispatchers.IO) {
                    if (!FileUtils.hasFreeSpace(context)) {
                        throw IOException("No space")
                    }
                    val outFile = FileUtils.newOutputFile(context, "Merged", "pdf")
                    PdfRepository(context).merge(state.entries.map { it.file }, outFile)
                    Triple(outFile, PdfUtils.renderPage(outFile, 0, 900), PdfUtils.pageCount(outFile))
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        resultFile = file,
                        previewBitmap = preview,
                        message = "Merged into one PDF ($pages pages)"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isBusy = false, message = friendlyMessage(e, "merging the PDFs"))
                }
            }
        }
    }
}
