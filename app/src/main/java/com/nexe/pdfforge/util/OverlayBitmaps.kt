package com.nexe.pdfforge.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.ceil
import kotlin.math.min

/** Draws text into transparent bitmaps so any script (including accented letters) can be stamped on a PDF. */
object OverlayBitmaps {

    const val SCALE = 3f

    fun text(
        text: String,
        sizePt: Float,
        argb: Int,
        opacity: Float,
        maxWidthPt: Float,
        rotationDeg: Float
    ): Bitmap {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.color = argb
        paint.alpha = (255 * opacity).toInt().coerceIn(0, 255)
        paint.textSize = sizePt * SCALE

        val maxPx = (maxWidthPt * SCALE).toInt().coerceAtLeast(48)
        val desired = ceil(Layout.getDesiredWidth(text, paint).toDouble()).toInt() + 4
        val widthPx = min(desired, maxPx).coerceAtLeast(1)
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, widthPx)
            .setIncludePad(false)
            .build()
        val bitmap = Bitmap.createBitmap(widthPx, layout.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        layout.draw(Canvas(bitmap))
        if (rotationDeg == 0f) return bitmap
        val rotated = rotate(bitmap, rotationDeg)
        bitmap.recycle()
        return rotated
    }

    /** Returns a rotated copy; the source is left untouched. */
    fun rotate(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
