package com.nexe.pdfforge.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

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

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

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
