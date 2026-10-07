package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.PdfFileModel
import com.nexe.pdfforge.data.repository.FileRepository
import com.nexe.pdfforge.util.friendlyMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOption(val label: String) {
    DATE("Newest"),
    NAME("Name"),
    SIZE("Largest")
}

data class FileManagerUiState(
    val files: List<PdfFileModel> = emptyList(),
    val query: String = "",
    val sort: SortOption = SortOption.DATE,
    val renameTarget: PdfFileModel? = null,
    val deleteTarget: PdfFileModel? = null,
    val message: String? = null
)

/** Files after applying the search text and sort order. */
fun FileManagerUiState.visibleFiles(): List<PdfFileModel> {
    val q = query.trim().lowercase()
    val filtered = if (q.isEmpty()) files else files.filter { it.name.lowercase().contains(q) }
    return when (sort) {
        SortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
        SortOption.DATE -> filtered.sortedByDescending { it.modified }
        SortOption.SIZE -> filtered.sortedByDescending { it.sizeBytes }
    }
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(application)
    private val _uiState = MutableStateFlow(FileManagerUiState())
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) { repository.listFiles() }
                _uiState.update { it.copy(files = list) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update { it.copy(message = "Couldn't load your files.") }
            }
        }
    }

    fun setQuery(text: String) {
        _uiState.update { it.copy(query = text) }
    }

    fun setSort(sort: SortOption) {
        _uiState.update { it.copy(sort = sort) }
    }

    fun askRename(target: PdfFileModel?) {
        _uiState.update { it.copy(renameTarget = target) }
    }

    fun askDelete(target: PdfFileModel?) {
        _uiState.update { it.copy(deleteTarget = target) }
    }

    fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun rename(newName: String) {
        val target = _uiState.value.renameTarget ?: return
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    repository.rename(target.file, newName)
                    repository.listFiles()
                }
                _uiState.update { it.copy(files = list, renameTarget = null, message = "Renamed") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update { it.copy(message = friendlyMessage(e, "renaming the file")) }
            }
        }
    }

    fun delete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    val deleted = repository.delete(target.file)
                    if (!deleted) throw IllegalArgumentException("Couldn't delete that file.")
                    repository.listFiles()
                }
                _uiState.update { it.copy(files = list, deleteTarget = null, message = "Deleted") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(deleteTarget = null, message = friendlyMessage(e, "deleting the file"))
                }
            }
        }
    }
}
