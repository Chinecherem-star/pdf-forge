package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.ImageItem
import com.nexe.pdfforge.data.model.ImagePdfOptions
import com.nexe.pdfforge.data.repository.SettingsRepository
import com.nexe.pdfforge.util.FileUtils
import com.nexe.pdfforge.util.ImageUtils
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

data class ImageToPdfUiState(
    val images: List<ImageItem> = emptyList(),
    val options: ImagePdfOptions = ImagePdfOptions(),
    val isGenerating: Boolean = false,
    val generatedFile: File? = null,
    val previewBitmap: Bitmap? = null,
    val message: String? = null
)

class ImageToPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ImageToPdfUiState())
    val uiState: StateFlow<ImageToPdfUiState> = _uiState.asStateFlow()
    private var nextId = 0L

    init {
        viewModelScope.launch {
            val settings = SettingsRepository(application).settings.first()
            _uiState.update { it.copy(options = it.options.copy(pageSize = settings.defaultPageSize)) }
        }
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val items = uris.map { ImageItem(nextId++, it) }
        _uiState.update {
            it.copy(images = it.images + items, generatedFile = null, previewBitmap = null)
        }
    }

    fun move(index: Int, delta: Int) {
        _uiState.update { state ->
            val target = index + delta
            if (index !in state.images.indices || target !in state.images.indices) {
                state
            } else {
                val list = state.images.toMutableList()
                val item = list.removeAt(index)
                list.add(target, item)
                state.copy(images = list, generatedFile = null, previewBitmap = null)
            }
        }
    }

    fun remove(index: Int) {
        _uiState.update { state ->
            if (index !in state.images.indices) {
                state
            } else {
                state.copy(
                    images = state.images.filterIndexed { i, _ -> i != index },
                    generatedFile = null,
                    previewBitmap = null
                )
            }
        }
    }

    fun rotate(index: Int) {
        _uiState.update { state ->
            if (index !in state.images.indices) {
                state
            } else {
                state.copy(
                    images = state.images.mapIndexed { i, item ->
                        if (i == index) item.copy(rotation = (item.rotation + 90) % 360) else item
                    },
                    generatedFile = null,
                    previewBitmap = null
                )
            }
        }
    }

    fun updateOptions(transform: (ImagePdfOptions) -> ImagePdfOptions) {
        _uiState.update { it.copy(options = transform(it.options)) }
    }

    fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun generate() {
        val state = _uiState.value
        if (state.isGenerating) return
        if (state.images.isEmpty()) {
            showMessage("Add at least one image first.")
            return
        }
        _uiState.update { it.copy(isGenerating = true) }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val (file, preview) = withContext(Dispatchers.IO) {
                    if (!FileUtils.hasFreeSpace(context)) {
                        throw IOException("No space")
                    }
                    val outFile = FileUtils.newOutputFile(context, "Images", "pdf")
                    ImageUtils.generateImagePdf(context, state.images, state.options, outFile)
                    outFile to PdfUtils.renderPage(outFile, 0, 900)
                }
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generatedFile = file,
                        previewBitmap = preview,
                        message = "PDF created with ${state.images.size} page(s)"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isGenerating = false, message = friendlyMessage(e, "creating the PDF"))
                }
            }
        }
    }
}
