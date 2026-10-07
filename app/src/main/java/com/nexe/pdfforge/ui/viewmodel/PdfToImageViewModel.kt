package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.ExportImageFormat
import com.nexe.pdfforge.data.model.ExportQuality
import com.nexe.pdfforge.data.model.PdfEntry
import com.nexe.pdfforge.util.FileUtils
import com.nexe.pdfforge.util.PageRangeUtils
import com.nexe.pdfforge.util.PdfExportUtils
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

data class PdfToImageUiState(
    val source: PdfEntry? = null,
    val allPages: Boolean = true,
    val rangeText: String = "",
    val format: ExportImageFormat = ExportImageFormat.PNG,
    val quality: ExportQuality = ExportQuality.MEDIUM,
    val isBusy: Boolean = false,
    val results: List<File> = emptyList(),
    val message: String? = null
)

class PdfToImageViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PdfToImageUiState())
    val uiState: StateFlow<PdfToImageUiState> = _uiState.asStateFlow()

    fun onPdfPicked(uri: Uri) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val entry = withContext(Dispatchers.IO) {
                    val file = FileUtils.copyUriToCache(context, uri)
                    val pages = PdfUtils.pageCount(file)
                    PdfEntry(0, file, FileUtils.displayName(context, uri), pages, file.length())
                }
                _uiState.update {
                    it.copy(isBusy = false, source = entry, rangeText = "", results = emptyList())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        message = "That PDF couldn't be opened. It may be corrupted or password protected."
                    )
                }
            }
        }
    }

    fun setAllPages(all: Boolean) {
        _uiState.update { it.copy(allPages = all) }
    }

    fun setRange(text: String) {
        _uiState.update { it.copy(rangeText = text) }
    }

    fun setFormat(format: ExportImageFormat) {
        _uiState.update { it.copy(format = format) }
    }

    fun setQuality(quality: ExportQuality) {
        _uiState.update { it.copy(quality = quality) }
    }

    fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun export() {
        val state = _uiState.value
        if (state.isBusy) return
        val source = state.source
        if (source == null) {
            showMessage("Choose a PDF first.")
            return
        }
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val indices = if (state.allPages) {
                    (0 until source.pageCount).toList()
                } else {
                    PageRangeUtils.parse(state.rangeText, source.pageCount)
                }
                val context = getApplication<Application>()
                val files = withContext(Dispatchers.IO) {
                    if (!FileUtils.hasFreeSpace(context)) {
                        throw IOException("No space")
                    }
                    val base = source.name.substringBeforeLast('.', source.name)
                    PdfExportUtils.exportPages(
                        context, source.file, indices, state.format, state.quality.dpi, base
                    )
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        results = files,
                        message = "Exported ${files.size} image(s)"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isBusy = false, message = friendlyMessage(e, "exporting the pages"))
                }
            }
        }
    }
}
