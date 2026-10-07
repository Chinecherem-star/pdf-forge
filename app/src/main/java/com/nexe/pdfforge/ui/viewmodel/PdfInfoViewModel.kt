package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.PdfInfo
import com.nexe.pdfforge.data.repository.PdfRepository
import com.nexe.pdfforge.util.FileUtils
import com.nexe.pdfforge.util.friendlyMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PdfInfoUiState(
    val info: PdfInfo? = null,
    val isBusy: Boolean = false,
    val message: String? = null
)

class PdfInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PdfInfoUiState())
    val uiState: StateFlow<PdfInfoUiState> = _uiState.asStateFlow()

    fun onPdfPicked(uri: Uri) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val info = withContext(Dispatchers.IO) {
                    val file = FileUtils.copyUriToCache(context, uri)
                    PdfRepository(context).inspect(file, FileUtils.displayName(context, uri))
                }
                _uiState.update { it.copy(isBusy = false, info = info) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isBusy = false, message = friendlyMessage(e, "reading the PDF"))
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
