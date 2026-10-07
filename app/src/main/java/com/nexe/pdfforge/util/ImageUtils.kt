package com.nexe.pdfforge.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.net.Uri
import com.nexe.pdfforge.data.model.ImageFit
import com.nexe.pdfforge.data.model.ImageItem
import com.nexe.pdfforge.data.model.ImagePdfOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

object ImageUtils {

    private const val MAX_IMAGE_DIM = 1800

    fun decodeBitmap(context: Context, uri: Uri, maxDim: Int, extraRotation: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: throw IOException("Couldn't open an image.")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("One of the files is not a supported image.")
        }

        val longest = max(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longest / (sample * 2) >= maxDim) sample *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val stream = resolver.openInputStream(uri) ?: throw IOException("Couldn't open an image.")
        val decoded = stream.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalArgumentException("One of the images could not be read.")

        val total = (readExifRotation(context, uri) + extraRotation) % 360
        if (total == 0) return decoded
        val matrix = Matrix().apply { postRotate(total.toFloat()) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }

    private fun readExifRotation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun generateImagePdf(
        context: Context,
        items: List<ImageItem>,
        options: ImagePdfOptions,
        outFile: File
    ) {
        require(items.isNotEmpty()) { "Add at least one image first." }
        val doc = PdfDocument()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        try {
            items.forEachIndexed { index, item ->
                val bitmap = decodeBitmap(context, item.uri, MAX_IMAGE_DIM, item.rotation)
                try {
                    val (pageW, pageH) = if (options.fit == ImageFit.MATCH) {
                        val scale = min(1f, 842f / max(bitmap.width, bitmap.height))
                        Pair(
                            (bitmap.width * scale).toInt().coerceAtLeast(1),
                            (bitmap.height * scale).toInt().coerceAtLeast(1)
                        )
                    } else {
                        var w = options.pageSize.widthPt
                        var h = options.pageSize.heightPt
                        if (options.autoOrientation && bitmap.width > bitmap.height) {
                            val t = w
                            w = h
                            h = t
                        }
                        Pair(w, h)
                    }

                    val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create())
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)

                    val m = if (options.fit == ImageFit.MATCH) 0 else options.margin.points
                    val areaW = (pageW - 2 * m).toFloat()
                    val areaH = (pageH - 2 * m).toFloat()
                    val scale = if (options.fit == ImageFit.FILL) {
                        max(areaW / bitmap.width, areaH / bitmap.height)
                    } else {
                        min(areaW / bitmap.width, areaH / bitmap.height)
                    }
                    val drawW = bitmap.width * scale
                    val drawH = bitmap.height * scale
                    val left = m + (areaW - drawW) / 2f
                    val top = m + (areaH - drawH) / 2f

                    canvas.save()
                    canvas.clipRect(m.toFloat(), m.toFloat(), (pageW - m).toFloat(), (pageH - m).toFloat())
                    canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), paint)
                    canvas.restore()
                    doc.finishPage(page)
                } finally {
                    bitmap.recycle()
                }
            }
            outFile.parentFile?.mkdirs()
            FileOutputStream(outFile).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }
}
