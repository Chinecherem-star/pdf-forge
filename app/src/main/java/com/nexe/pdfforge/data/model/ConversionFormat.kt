package com.nexe.pdfforge.data.model

import android.graphics.Bitmap

enum class ConversionFormat(val label: String, val ext: String, val mime: String) {
    PDF("PDF", "pdf", "application/pdf"),
    TXT("TXT", "txt", "text/plain"),
    DOCX("DOCX", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    HTML("HTML", "html", "text/html"),
    MARKDOWN("Markdown", "md", "text/markdown")
}

enum class ExportImageFormat(val label: String, val ext: String, val compressFormat: Bitmap.CompressFormat) {
    PNG("PNG", "png", Bitmap.CompressFormat.PNG),
    JPG("JPG", "jpg", Bitmap.CompressFormat.JPEG)
}

enum class ExportQuality(val label: String, val dpi: Int) {
    LOW("Low (96 dpi)", 96),
    MEDIUM("Medium (150 dpi)", 150),
    HIGH("High (220 dpi)", 220)
}
