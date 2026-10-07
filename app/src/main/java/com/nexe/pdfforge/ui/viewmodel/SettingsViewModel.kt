package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.model.PageSizeOption
import com.nexe.pdfforge.data.model.SettingsState
import com.nexe.pdfforge.data.model.ThemeMode
import com.nexe.pdfforge.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val state: StateFlow<SettingsState> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDefaultPageSize(size: PageSizeOption) {
        viewModelScope.launch { repository.setDefaultPageSize(size) }
    }

    fun setDefaultLandscape(landscape: Boolean) {
        viewModelScope.launch { repository.setDefaultLandscape(landscape) }
    }

    fun setDefaultFontSize(size: Int) {
        viewModelScope.launch { repository.setDefaultFontSize(size) }
    }
}
