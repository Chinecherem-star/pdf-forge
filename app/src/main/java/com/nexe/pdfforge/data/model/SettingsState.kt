package com.nexe.pdfforge.data.model

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultPageSize: PageSizeOption = PageSizeOption.A4,
    val defaultLandscape: Boolean = false,
    val defaultFontSize: Int = 12
)
