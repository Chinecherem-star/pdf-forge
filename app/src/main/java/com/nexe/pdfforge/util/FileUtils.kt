package com.nexe.pdfforge.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.Locale

object FileUtils {

    private const val MIN_FREE_BYTES = 5L * 1024 * 1024

    fun outputDir(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        return File(base, "PDFForge").apply { mkdirs() }
    }

    fun hasFreeSpace(context: Context): Boolean = outputDir(context).usableSpace >= MIN_FREE_BYTES

    fun newOutputFile(context: Context, baseName: String, extension: String): File {
        val clean = baseName
            .replace(Regex("[^A-Za-z0-9 _.-]"), "")
            .trim()
            .ifBlank { "Document" }
            .take(60)
        val dir = outputDir(context)
        var file = File(dir, "$clean.$extension")
        if (file.exists()) {
            file = File(dir, "${clean}_${System.currentTimeMillis()}.$extension")
        }
        return file
    }

    fun newCameraFile(context: Context): File {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
        file.createNewFile()
        return file
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun displayName(context: Context, uri: Uri): String {
        var name: String? = null
        try {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) name = cursor.getString(index)
                }
            }
        } catch (e: Exception) {
            // fall back below
        }
        return name ?: uri.lastPathSegment ?: "file"
    }

    /** Copies a picked document into the app cache so PDF/image libraries can read it as a File. */
    fun copyUriToCache(context: Context, uri: Uri): File {
        if (context.cacheDir.usableSpace < MIN_FREE_BYTES) throw IOException("No space")
        val dir = File(context.cacheDir, "picked").apply { mkdirs() }
        val safe = displayName(context, uri).replace(Regex("[^A-Za-z0-9 _.-]"), "_").take(80)
        val file = File(dir, "${System.nanoTime()}_$safe")
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Couldn't open that file.")
        input.use { source ->
            file.outputStream().use { target -> source.copyTo(target) }
        }
        return file
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
        return String.format(Locale.US, "%.2f GB", mb / 1024.0)
    }

    fun mimeFor(file: File): String = when (file.extension.lowercase()) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "txt" -> "text/plain"
        "html", "htm" -> "text/html"
        "md" -> "text/markdown"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        else -> "*/*"
    }

    fun openFile(context: Context, file: File, mime: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uriFor(context, file), mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun shareFile(context: Context, file: File, mime: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun shareFiles(context: Context, files: List<File>, mime: String): Boolean {
        if (files.isEmpty()) return false
        val uris = ArrayList<Uri>(files.map { uriFor(context, it) })
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mime
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    /** Copies [file] to a location picked through the system file picker. */
    fun copyToUri(context: Context, file: File, destination: Uri): Boolean {
        return try {
            val out = context.contentResolver.openOutputStream(destination) ?: return false
            out.use { stream ->
                file.inputStream().use { input -> input.copyTo(stream) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
