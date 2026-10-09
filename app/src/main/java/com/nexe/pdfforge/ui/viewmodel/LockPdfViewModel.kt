package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.PdfEntry
import com.nexe.pdfforge.data.repository.PdfRepository
import com.nexe.pdfforge.util.FileHandoff
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
import java.io.File
import java.io.IOException

data class LockPdfUiState(
    val source: PdfEntry? = null,
    val encrypted: Boolean = false,
    val password: String = "",
    val confirm: String = "",
    val isBusy: Boolean = false,
    val resultFile: File? = null,
    val message: String? = null
)

class LockPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LockPdfUiState())
    val uiState: StateFlow<LockPdfUiState> = _uiState.asStateFlow()

    fun onPdfPicked(uri: Uri) {
        load { app -> FileUtils.copyUriToCache(app, uri) to FileUtils.displayName(app, uri) }
    }

    /** Opens a file sent from My Files, if there is one waiting for this tool. */
    fun consumeHandoff() {
        val file = FileHandoff.take("lock_pdf") ?: return
        load { _ -> file to file.name }
    }

    private fun load(block: (Application) -> Pair<File, String>) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val (entry, encrypted) = withContext(Dispatchers.IO) {
                    val (file, name) = block(app)
                    val locked = PdfRepository(app).isEncrypted(file)
                    PdfEntry(0, file, name, 0, file.length()) to locked
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        source = entry,
                        encrypted = encrypted,
                        password = "",
                        confirm = "",
                        resultFile = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isBusy = false, message = friendlyMessage(e, "opening the PDF"))
                }
            }
        }
    }

    fun setPassword(text: String) {
        _uiState.update { it.copy(password = text) }
    }

    fun setConfirm(text: String) {
        _uiState.update { it.copy(confirm = text) }
    }

    fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun lock() {
        val state = _uiState.value
        if (state.isBusy) return
        val source = state.source
        if (source == null) {
            showMessage("Choose a PDF first.")
            return
        }
        if (state.password.length < 4) {
            showMessage("Use a password with at least 4 characters.")
            return
        }
        if (state.password != state.confirm) {
            showMessage("The two passwords don't match.")
            return
        }
        val password = state.password
        process(source, "locking the PDF", "_locked", "PDF locked") { repo, out ->
            repo.protect(source.file, out, password)
        }
    }

    fun unlock() {
        val state = _uiState.value
        if (state.isBusy) return
        val source = state.source
        if (source == null) {
            showMessage("Choose a PDF first.")
            return
        }
        val password = state.password
        process(source, "unlocking the PDF", "_unlocked", "Password removed") { repo, out ->
            repo.unlock(source.file, password, out)
        }
    }

    private fun process(
        source: PdfEntry,
        action: String,
        suffix: String,
        successText: String,
        block: (PdfRepository, File) -> Unit
    ) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val file = withContext(Dispatchers.IO) {
                    if (!FileUtils.hasFreeSpace(app)) {
                        throw IOException("No space")
                    }
                    val base = source.name.substringBeforeLast('.', source.name)
                    val out = FileUtils.newOutputFile(app, base + suffix, "pdf")
                    block(PdfRepository(app), out)
                    out
                }
                // The password only lives in memory and is cleared as soon as it has been used.
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        resultFile = file,
                        password = "",
                        confirm = "",
                        message = successText
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val text = if (e.javaClass.simpleName == "InvalidPasswordException") {
                    "Wrong password. Try again."
                } else {
                    friendlyMessage(e, action)
                }
                _uiState.update { it.copy(isBusy = false, message = text) }
            }
        }
    }
}
