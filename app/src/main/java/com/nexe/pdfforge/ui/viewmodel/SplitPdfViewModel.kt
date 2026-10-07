package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.PdfEntry
import com.nexe.pdfforge.data.repository.PdfRepository
import com.nexe.pdfforge.util.FileUtils
import com.nexe.pdfforge.util.PageRangeUtils
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

data class SplitPdfUiState(
    val source: PdfEntry? = null,
    val rangeText: String = "",
    val isBusy: Boolean = false,
    val resultFile: File? = null,
    val previewBitmap: Bitmap? = null,
    val message: String? = null
)

class SplitPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SplitPdfUiState())
    val uiState: StateFlow<SplitPdfUiState> = _uiState.asStateFlow()

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
                    it.copy(
                        isBusy = false,
                        source = entry,
                        rangeText = "",
                        resultFile = null,
                        previewBitmap = null
                    )
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

    fun setRange(text: String) {
        _uiState.update { it.copy(rangeText = text) }
    }

    fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun split() {
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
                val indices = PageRangeUtils.parse(state.rangeText, source.pageCount)
                val context = getApplication<Application>()
                val (file, preview) = withContext(Dispatchers.IO) {
                    if (!FileUtils.hasFreeSpace(context)) {
                        throw IOException("No space")
                    }
                    val base = source.name.substringBeforeLast('.', source.name)
                    val outFile = FileUtils.newOutputFile(context, "${base}_pages", "pdf")
                    PdfRepository(context).extractPages(source.file, indices, outFile)
                    outFile to PdfUtils.renderPage(outFile, 0, 900)
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        resultFile = file,
                        previewBitmap = preview,
                        message = "New PDF created with ${indices.size} page(s)"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isBusy = false, message = friendlyMessage(e, "splitting the PDF"))
                }
            }
        }
    }
}
