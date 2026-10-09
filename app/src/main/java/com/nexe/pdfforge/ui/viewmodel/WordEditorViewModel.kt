package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.ConversionFormat
import com.nexe.pdfforge.data.model.TextPdfOptions
import com.nexe.pdfforge.data.repository.SettingsRepository
import com.nexe.pdfforge.util.DocumentFormatter
import com.nexe.pdfforge.util.DocxReader
import com.nexe.pdfforge.util.FileHandoff
import com.nexe.pdfforge.util.FileUtils
import com.nexe.pdfforge.util.PdfUtils
import com.nexe.pdfforge.util.friendlyMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class WordEditorUiState(
    val fileName: String = "",
    val body: String = "",
    val format: ConversionFormat = ConversionFormat.DOCX,
    val isBusy: Boolean = false,
    val resultFile: File? = null,
    val message: String? = null
)

class WordEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WordEditorUiState())
    val uiState: StateFlow<WordEditorUiState> = _uiState.asStateFlow()

    fun onFilePicked(uri: Uri) {
        load { app -> FileUtils.displayName(app, uri) to { FileUtils.copyUriToCache(app, uri) } }
    }

    /** Opens a .docx sent from My Files, if there is one waiting for this tool. */
    fun consumeHandoff() {
        val file = FileHandoff.take("word_editor") ?: return
        load { _ -> file.name to { file } }
    }

    private fun load(block: (Application) -> Pair<String, () -> File>) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val (name, text) = withContext(Dispatchers.IO) {
                    val (displayName, getFile) = block(app)
                    require(displayName.endsWith(".docx", ignoreCase = true)) {
                        "Only .docx files can be opened. For an older .doc file, save it as .docx in Word or Google Docs first."
                    }
                    val paragraphs = DocxReader.readParagraphs(getFile())
                    displayName to paragraphs.joinToString("\n")
                }
                _uiState.update {
                    it.copy(isBusy = false, fileName = name, body = text, resultFile = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isBusy = false, message = friendlyMessage(e, "opening the document"))
                }
            }
        }
    }

    fun setBody(text: String) {
        _uiState.update { it.copy(body = text, resultFile = null) }
    }

    fun setFormat(format: ConversionFormat) {
        _uiState.update { it.copy(format = format, resultFile = null) }
    }

    fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isBusy) return
        if (state.body.isBlank()) {
            showMessage("There is no text to save.")
            return
        }
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val file = withContext(Dispatchers.IO) {
                    if (!FileUtils.hasFreeSpace(app)) {
                        throw IOException("No space")
                    }
                    val base = if (state.fileName.isBlank()) {
                        "Document"
                    } else {
                        state.fileName.substringBeforeLast('.') + "_edited"
                    }
                    val out = FileUtils.newOutputFile(app, base, state.format.ext)
                    when (state.format) {
                        ConversionFormat.PDF -> {
                            val settings = SettingsRepository(app).settings.first()
                            PdfUtils.generateTextPdf(
                                TextPdfOptions(
                                    body = state.body,
                                    fontSize = settings.defaultFontSize,
                                    pageSize = settings.defaultPageSize,
                                    landscape = settings.defaultLandscape
                                ),
                                out
                            )
                        }
                        ConversionFormat.TXT ->
                            out.writeText(DocumentFormatter.toTxt("", state.body))
                        ConversionFormat.MARKDOWN ->
                            out.writeText(DocumentFormatter.toMarkdown("", state.body))
                        ConversionFormat.HTML ->
                            out.writeText(DocumentFormatter.toHtml("", state.body))
                        ConversionFormat.DOCX ->
                            DocumentFormatter.writeDocx("", state.body, out)
                    }
                    out
                }
                _uiState.update {
                    it.copy(isBusy = false, resultFile = file, message = "${state.format.label} file saved")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isBusy = false, message = friendlyMessage(e, "saving the document"))
                }
            }
        }
    }
}
