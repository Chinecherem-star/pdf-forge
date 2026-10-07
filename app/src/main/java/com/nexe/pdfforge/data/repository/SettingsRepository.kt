package com.nexe.pdfforge.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexe.pdfforge.data.model.PageSizeOption
import com.nexe.pdfforge.data.model.SettingsState
import com.nexe.pdfforge.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "pdf_forge_settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private val themeKey = stringPreferencesKey("theme_mode")
    private val pageSizeKey = stringPreferencesKey("page_size")
    private val landscapeKey = booleanPreferencesKey("landscape")
    private val fontSizeKey = intPreferencesKey("font_size")

    val settings: Flow<SettingsState> = store.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            SettingsState(
                themeMode = ThemeMode.values().firstOrNull { it.name == prefs[themeKey] }
                    ?: ThemeMode.SYSTEM,
                defaultPageSize = PageSizeOption.values().firstOrNull { it.name == prefs[pageSizeKey] }
                    ?: PageSizeOption.A4,
                defaultLandscape = prefs[landscapeKey] ?: false,
                defaultFontSize = prefs[fontSizeKey] ?: 12
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[themeKey] = mode.name }
    }

    suspend fun setDefaultPageSize(size: PageSizeOption) {
        store.edit { it[pageSizeKey] = size.name }
    }

    suspend fun setDefaultLandscape(landscape: Boolean) {
        store.edit { it[landscapeKey] = landscape }
    }

    suspend fun setDefaultFontSize(size: Int) {
        store.edit { it[fontSizeKey] = size }
    }
}
