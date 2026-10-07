package com.nexe.pdfforge.data.model

import android.net.Uri

data class ImageItem(
    val id: Long,
    val uri: Uri,
    val rotation: Int = 0
)

enum class ImageFit(val label: String) {
    FIT("Fit to page"),
    FILL("Fill page"),
    MATCH("Match image size")
}

enum class ImageMargin(val label: String, val points: Int) {
    NONE("No margin", 0),
    SMALL("Small margin", 18),
    NORMAL("Normal margin", 36)
}

data class ImagePdfOptions(
    val pageSize: PageSizeOption = PageSizeOption.A4,
    val fit: ImageFit = ImageFit.FIT,
    val margin: ImageMargin = ImageMargin.SMALL,
    val autoOrientation: Boolean = true
)
