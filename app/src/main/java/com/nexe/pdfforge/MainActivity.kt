package com.nexe.pdfforge

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexe.pdfforge.data.model.ThemeMode
import com.nexe.pdfforge.ui.navigation.AppNavGraph
import com.nexe.pdfforge.ui.screens.UpdateRequiredScreen
import com.nexe.pdfforge.ui.theme.PdfForgeTheme
import com.nexe.pdfforge.ui.viewmodel.RemoteConfigViewModel
import com.nexe.pdfforge.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val remoteViewModel: RemoteConfigViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        // Pick up admin changes when the app comes back to the foreground.
        remoteViewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            val remote by remoteViewModel.state.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Keep status/navigation bar icon colours readable when the chosen theme
            // differs from the system theme.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
                onDispose { }
            }

            PdfForgeTheme(darkTheme = dark) {
                if (remote.updateRequired) {
                    UpdateRequiredScreen(
                        message = remote.config.updateMessage,
                        latestVersion = remote.config.latestVersionName,
                        updateUrl = remote.config.updateUrl
                    )
                } else {
                    AppNavGraph(
                        config = remote.config,
                        onToolOpened = remoteViewModel::track
                    )
                }
            }
        }
    }
}
