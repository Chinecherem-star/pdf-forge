package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.ConversionFormat
import com.nexe.pdfforge.data.model.TextPdfOptions
import com.nexe.pdfforge.data.repository.SettingsRepository
import com.nexe.pdfforge.util.DocumentFormatter
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

data class TextConverterUiState(
    val title: String = "",
    val body: String = "",
    val format: ConversionFormat = ConversionFormat.PDF,
    val isBusy: Boolean = false,
    val resultFile: File? = null,
    val message: String? = null
)

class TextConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TextConverterUiState())
    val uiState: StateFlow<TextConverterUiState> = _uiState.asStateFlow()

    fun setTitle(text: String) {
        _uiState.update { it.copy(title = text) }
    }

    fun setBody(text: String) {
        _uiState.update { it.copy(body = text) }
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

    fun convert() {
        val state = _uiState.value
        if (state.isBusy) return
        if (state.title.isBlank() && state.body.isBlank()) {
            showMessage("Add a title or some text first.")
            return
        }
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val file = withContext(Dispatchers.IO) {
                    if (!FileUtils.hasFreeSpace(context)) {
                        throw IOException("No space")
                    }
                    val out = FileUtils.newOutputFile(context, state.title, state.format.ext)
                    when (state.format) {
                        ConversionFormat.PDF -> {
                            val settings = SettingsRepository(context).settings.first()
                            PdfUtils.generateTextPdf(
                                TextPdfOptions(
                                    title = state.title,
                                    body = state.body,
                                    fontSize = settings.defaultFontSize,
                                    pageSize = settings.defaultPageSize,
                                    landscape = settings.defaultLandscape
                                ),
                                out
                            )
                        }
                        ConversionFormat.TXT ->
                            out.writeText(DocumentFormatter.toTxt(state.title, state.body))
                        ConversionFormat.MARKDOWN ->
                            out.writeText(DocumentFormatter.toMarkdown(state.title, state.body))
                        ConversionFormat.HTML ->
                            out.writeText(DocumentFormatter.toHtml(state.title, state.body))
                        ConversionFormat.DOCX ->
                            DocumentFormatter.writeDocx(state.title, state.body, out)
                    }
                    out
                }
                _uiState.update {
                    it.copy(isBusy = false, resultFile = file, message = "${state.format.label} file created")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isBusy = false, message = friendlyMessage(e, "converting the text"))
                }
            }
        }
    }
}
