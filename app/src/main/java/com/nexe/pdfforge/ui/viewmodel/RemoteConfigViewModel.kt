package com.nexe.pdfforge.ui.viewmodel

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexe.pdfforge.data.remote.RemoteConfig
import com.nexe.pdfforge.data.remote.RemoteConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RemoteConfigState(
    val config: RemoteConfig = RemoteConfig(),
    val versionCode: Long = 1L,
    val versionName: String = "1.0.0"
) {
    val updateRequired: Boolean
        get() = versionCode < config.minVersionCode
}

class RemoteConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RemoteConfigRepository(application)

    private val _state = MutableStateFlow(
        run {
            var code = 1L
            var name = "1.0.0"
            try {
                val info = application.packageManager.getPackageInfo(application.packageName, 0)
                code = PackageInfoCompat.getLongVersionCode(info)
                name = info.versionName ?: "1.0.0"
            } catch (e: Exception) {
                // keep defaults
            }
            RemoteConfigState(config = repository.cached(), versionCode = code, versionName = name)
        }
    )
    val state: StateFlow<RemoteConfigState> = _state.asStateFlow()

    private var lastRefresh = 0L

    init {
        refresh()
        track("app_open")
    }

    /** Re-downloads the config, at most once a minute. */
    fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefresh < 60_000L) return
        lastRefresh = now
        viewModelScope.launch {
            val fresh = repository.fetchAndCache()
            if (fresh != null) {
                _state.update { it.copy(config = fresh) }
            }
        }
    }

    fun track(tool: String) {
        val version = _state.value.versionName
        viewModelScope.launch {
            repository.trackUsage(tool, version)
        }
    }
}
