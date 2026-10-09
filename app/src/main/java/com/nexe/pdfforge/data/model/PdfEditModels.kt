package com.nexe.pdfforge.data.model

import android.graphics.Bitmap
import java.io.File

/** One page of the output PDF: which original page it came from and how much to rotate it. */
data class PageEdit(val originalIndex: Int, val rotationDelta: Int)

/** A page row in the editor list. */
data class EditPageItem(
    val id: Long,
    val originalIndex: Int,
    val rotation: Int = 0,
    val thumbnail: Bitmap? = null
)

enum class OverlayColor(val label: String, val argb: Int) {
    BLACK("Black", 0xFF000000.toInt()),
    RED("Red", 0xFFD32F2F.toInt()),
    BLUE("Blue", 0xFF1565C0.toInt()),
    GRAY("Gray", 0xFF616161.toInt())
}

/** Something drawn on top of PDF pages. [page] is 1-based in the final order; 0 means all pages. */
sealed class Overlay {
    abstract val page: Int

    data class Text(
        val text: String,
        val sizePt: Int,
        val color: OverlayColor,
        val xPct: Float,
        val yPct: Float,
        override val page: Int
    ) : Overlay()

    data class Picture(
        val file: File,
        val widthPct: Float,
        val xPct: Float,
        val yPct: Float,
        override val page: Int
    ) : Overlay()

    data class Watermark(
        val text: String,
        val color: OverlayColor,
        override val page: Int
    ) : Overlay()
}
