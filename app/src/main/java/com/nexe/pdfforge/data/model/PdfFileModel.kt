package com.nexe.pdfforge.data.model

import java.io.File

enum class FileCategory(val label: String) {
    PDF("PDFs"),
    DOCUMENT("Documents"),
    IMAGE("Images"),
    OTHER("Other")
}

data class PdfFileModel(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val modified: Long,
    val category: FileCategory
) {
    companion object {
        fun from(file: File) = PdfFileModel(
            file = file,
            name = file.name,
            sizeBytes = file.length(),
            modified = file.lastModified(),
            category = categoryFor(file)
        )
    }
}

fun categoryFor(file: File): FileCategory = when (file.extension.lowercase()) {
    "pdf" -> FileCategory.PDF
    "txt", "docx", "doc", "html", "htm", "md" -> FileCategory.DOCUMENT
    "png", "jpg", "jpeg", "webp" -> FileCategory.IMAGE
    else -> FileCategory.OTHER
}

/** A PDF the user picked, copied into the app cache. */
data class PdfEntry(
    val id: Long,
    val file: File,
    val name: String,
    val pageCount: Int,
    val sizeBytes: Long
)

data class PdfInfo(
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val pdfVersion: String,
    val encrypted: Boolean,
    val firstPageSize: String?,
    val title: String?,
    val author: String?,
    val subject: String?,
    val creator: String?,
    val producer: String?,
    val keywords: String?,
    val created: Long?,
    val modified: Long?
)
