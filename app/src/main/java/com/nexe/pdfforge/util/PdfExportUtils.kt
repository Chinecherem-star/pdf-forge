package com.nexe.pdfforge.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.nexe.pdfforge.data.model.ExportImageFormat
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object PdfExportUtils {

    private const val MAX_SIDE_PX = 3000

    /** Renders the given zero-based pages to image files and returns them. */
    fun exportPages(
        context: Context,
        pdf: File,
        indices: List<Int>,
        format: ExportImageFormat,
        dpi: Int,
        baseName: String
    ): List<File> {
        return ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val files = mutableListOf<File>()
                for (index in indices) {
                    require(index in 0 until renderer.pageCount) { "Page ${index + 1} doesn't exist." }
                    renderer.openPage(index).use { page ->
                        val scale = dpi / 72f
                        var w = (page.width * scale).toInt().coerceAtLeast(1)
                        var h = (page.height * scale).toInt().coerceAtLeast(1)
                        val longest = max(w, h)
                        if (longest > MAX_SIDE_PX) {
                            val factor = MAX_SIDE_PX.toFloat() / longest
                            w = (w * factor).toInt().coerceAtLeast(1)
                            h = (h * factor).toInt().coerceAtLeast(1)
                        }
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val out = FileUtils.newOutputFile(context, "${baseName}_page_${index + 1}", format.ext)
                        FileOutputStream(out).use { bitmap.compress(format.compressFormat, 92, it) }
                        bitmap.recycle()
                        files.add(out)
                    }
                }
                files
            }
        }
    }
}
