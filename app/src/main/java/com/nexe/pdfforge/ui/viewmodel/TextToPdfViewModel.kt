package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.TextPdfOptions
import com.nexe.pdfforge.util.FileUtils
import com.nexe.pdfforge.util.PdfUtils
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

data class TextToPdfUiState(
    val options: TextPdfOptions = TextPdfOptions(),
    val isGenerating: Boolean = false,
    val generatedFile: File? = null,
    val previewBitmap: Bitmap? = null,
    val message: String? = null
)

class TextToPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TextToPdfUiState())
    val uiState: StateFlow<TextToPdfUiState> = _uiState.asStateFlow()

    fun updateOptions(transform: (TextPdfOptions) -> TextPdfOptions) {
        _uiState.update { it.copy(options = transform(it.options)) }
    }

    /** Adds a page-break marker line at the end of the current text. */
    fun insertPageBreak() {
        updateOptions { it.copy(body = it.body.trimEnd('\n') + "\n" + PdfUtils.PAGE_BREAK_MARKER + "\n") }
    }

    fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun generate() {
        if (_uiState.value.isGenerating) return
        val options = _uiState.value.options
        _uiState.update { it.copy(isGenerating = true) }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val (file, preview) = withContext(Dispatchers.IO) {
                    if (!FileUtils.hasFreeSpace(context)) {
                        throw IOException("No space")
                    }
                    val outFile = FileUtils.newOutputFile(context, options.title, "pdf")
                    PdfUtils.generateTextPdf(options, outFile)
                    val bitmap = PdfUtils.renderPage(outFile, 0, 900)
                    outFile to bitmap
                }
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generatedFile = file,
                        previewBitmap = preview,
                        message = "PDF created"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                fail(e.message ?: "Please check your input.")
            } catch (e: IOException) {
                fail("Couldn't save the PDF. Check that your phone has free storage and try again.")
            } catch (e: OutOfMemoryError) {
                fail("That document is too large to process.")
            } catch (e: Exception) {
                fail("Something went wrong while creating the PDF.")
            }
        }
    }

    private fun fail(text: String) {
        _uiState.update { it.copy(isGenerating = false, message = text) }
    }
}
