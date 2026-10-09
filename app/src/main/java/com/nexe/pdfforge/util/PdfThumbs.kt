package com.nexe.pdfforge.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

object PdfThumbs {

    /** Renders small previews of the given zero-based pages, calling [onEach] for every page. */
    fun render(file: File, indices: List<Int>, targetWidth: Int, onEach: (Int, Bitmap) -> Unit) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (index in indices) {
                    if (index !in 0 until renderer.pageCount) continue
                    renderer.openPage(index).use { page ->
                        val scale = targetWidth.toFloat() / page.width
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        onEach(index, bitmap)
                    }
                }
            }
        }
    }
}
