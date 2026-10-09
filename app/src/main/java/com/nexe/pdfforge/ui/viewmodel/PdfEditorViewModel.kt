package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.EditPageItem
import com.nexe.pdfforge.data.model.Overlay
import com.nexe.pdfforge.data.model.OverlayColor
import com.nexe.pdfforge.data.model.PageEdit
import com.nexe.pdfforge.data.model.PdfEntry
import com.nexe.pdfforge.util.FileHandoff
import com.nexe.pdfforge.util.FileUtils
import com.nexe.pdfforge.util.PdfEditUtils
import com.nexe.pdfforge.util.PdfThumbs
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
import kotlin.math.min

data class PdfEditorUiState(
    val source: PdfEntry? = null,
    val pages: List<EditPageItem> = emptyList(),
    val overlays: List<Overlay> = emptyList(),
    val pageNumbers: Boolean = false,
    val isBusy: Boolean = false,
    val resultFile: File? = null,
    val previewBitmap: Bitmap? = null,
    val message: String? = null
)

class PdfEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PdfEditorUiState())
    val uiState: StateFlow<PdfEditorUiState> = _uiState.asStateFlow()
    private var nextId = 0L

    fun onPdfPicked(uri: Uri) {
        load { app -> FileUtils.copyUriToCache(app, uri) to FileUtils.displayName(app, uri) }
    }

    /** Opens a file sent from My Files, if there is one waiting for this tool. */
    fun consumeHandoff() {
        val file = FileHandoff.take("pdf_editor") ?: return
        load { _ -> file to file.name }
    }

    private fun load(block: (Application) -> Pair<File, String>) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val entry = withContext(Dispatchers.IO) {
                    val (file, name) = block(app)
                    val pages = PdfUtils.pageCount(file)
                    require(pages > 0) { "This PDF has no pages." }
                    PdfEntry(0, file, name, pages, file.length())
                }
                val items = (0 until entry.pageCount).map { EditPageItem(nextId++, it) }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        source = entry,
                        pages = items,
                        overlays = emptyList(),
                        pageNumbers = false,
                        resultFile = null,
                        previewBitmap = null
                    )
                }
                loadThumbnails(entry)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        message = "That PDF couldn't be opened. If it has a password, remove it first with Lock PDF."
                    )
                }
            }
        }
    }

    private fun loadThumbnails(entry: PdfEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val indices = (0 until min(entry.pageCount, 40)).toList()
                PdfThumbs.render(entry.file, indices, 180) { index, bitmap ->
                    _uiState.update { state ->
                        state.copy(
                            pages = state.pages.map { page ->
                                if (page.originalIndex == index) page.copy(thumbnail = bitmap) else page
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                // thumbnails are optional
            }
        }
    }

    /** Any edit makes the previous result out of date. */
    private fun change(transform: (PdfEditorUiState) -> PdfEditorUiState) {
        _uiState.update { transform(it).copy(resultFile = null, previewBitmap = null) }
    }

    fun movePage(index: Int, delta: Int) = change { state ->
        val target = index + delta
        if (index !in state.pages.indices || target !in state.pages.indices) {
            state
        } else {
            val list = state.pages.toMutableList()
            val item = list.removeAt(index)
            list.add(target, item)
            state.copy(pages = list)
        }
    }

    fun rotatePage(index: Int) = change { state ->
        state.copy(
            pages = state.pages.mapIndexed { i, page ->
                if (i == index) page.copy(rotation = (page.rotation + 90) % 360) else page
            }
        )
    }

    fun deletePage(index: Int) {
        if (_uiState.value.pages.size <= 1) {
            showMessage("A PDF needs at least one page.")
            return
        }
        change { state ->
            state.copy(pages = state.pages.filterIndexed { i, _ -> i != index })
        }
    }

    fun setPageNumbers(enabled: Boolean) = change { it.copy(pageNumbers = enabled) }

    fun removeOverlay(index: Int) = change { state ->
        state.copy(overlays = state.overlays.filterIndexed { i, _ -> i != index })
    }

    /** Blank means all pages (0). Returns null when the text isn't a valid page. */
    private fun parsePage(pageText: String): Int? {
        val trimmed = pageText.trim()
        if (trimmed.isEmpty()) return 0
        val number = trimmed.toIntOrNull() ?: return null
        return if (number in 1.._uiState.value.pages.size) number else null
    }

    private fun badPageMessage() =
        "Page must be between 1 and ${_uiState.value.pages.size}, or blank for all pages."

    fun addText(text: String, sizePt: Int, color: OverlayColor, xPct: Float, yPct: Float, pageText: String) {
        if (text.isBlank()) {
            showMessage("Type the text you want to add.")
            return
        }
        val page = parsePage(pageText)
        if (page == null) {
            showMessage(badPageMessage())
            return
        }
        change { it.copy(overlays = it.overlays + Overlay.Text(text.trim(), sizePt, color, xPct, yPct, page)) }
    }

    fun addWatermark(text: String, color: OverlayColor, pageText: String) {
        if (text.isBlank()) {
            showMessage("Type the watermark text.")
            return
        }
        val page = parsePage(pageText)
        if (page == null) {
            showMessage(badPageMessage())
            return
        }
        change { it.copy(overlays = it.overlays + Overlay.Watermark(text.trim(), color, page)) }
    }

    fun addPicture(uri: Uri, widthPct: Float, xPct: Float, yPct: Float, pageText: String) {
        val page = parsePage(pageText)
        if (page == null) {
            showMessage(badPageMessage())
            return
        }
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val file = withContext(Dispatchers.IO) { FileUtils.copyUriToCache(app, uri) }
                change { it.copy(overlays = it.overlays + Overlay.Picture(file, widthPct, xPct, yPct, page)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showMessage(friendlyMessage(e, "adding the image"))
            }
        }
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
        val source = state.source
        if (source == null) {
            showMessage("Choose a PDF first.")
            return
        }
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val (file, preview) = withContext(Dispatchers.IO) {
                    if (!FileUtils.hasFreeSpace(app)) {
                        throw IOException("No space")
                    }
                    val base = source.name.substringBeforeLast('.', source.name)
                    val out = FileUtils.newOutputFile(app, base + "_edited", "pdf")
                    val edits = state.pages.map { PageEdit(it.originalIndex, it.rotation) }
                    PdfEditUtils.applyEdits(app, source.file, edits, state.overlays, state.pageNumbers, out)
                    out to PdfUtils.renderPage(out, 0, 900)
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        resultFile = file,
                        previewBitmap = preview,
                        message = "Edited PDF saved"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isBusy = false, message = friendlyMessage(e, "saving the PDF"))
                }
            }
        }
    }
}
