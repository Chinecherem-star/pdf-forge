package com.nexe.pdfforge.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.nexe.pdfforge.data.model.Overlay
import com.nexe.pdfforge.data.model.PageEdit
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** Page reorder / rotate / delete plus text, image, watermark and page-number overlays. */
object PdfEditUtils {

    private class Prepared(val image: PDImageXObject, val widthPt: Float, val heightPt: Float)

    fun applyEdits(
        context: Context,
        input: File,
        pageEdits: List<PageEdit>,
        overlays: List<Overlay>,
        addPageNumbers: Boolean,
        output: File
    ) {
        require(pageEdits.isNotEmpty()) { "Keep at least one page." }
        PDFBoxResourceLoader.init(context.applicationContext)

        val source = PDDocument.load(input)
        val dest = PDDocument()
        try {
            val pages = ArrayList<PDPage>()
            for (edit in pageEdits) {
                val page = dest.importPage(source.getPage(edit.originalIndex))
                page.rotation = normalize(page.rotation + edit.rotationDelta)
                pages.add(page)
            }

            val cache = HashMap<String, Prepared>()
            val pictures = HashMap<File, Bitmap>()
            try {
                pages.forEachIndexed { index, page ->
                    decorate(
                        context, dest, page, index + 1, overlays, addPageNumbers, cache, pictures
                    )
                }
            } finally {
                pictures.values.forEach { it.recycle() }
            }

            output.parentFile?.mkdirs()
            dest.save(output)
        } finally {
            runCatching { source.close() }
            runCatching { dest.close() }
        }
    }

    private fun decorate(
        context: Context,
        dest: PDDocument,
        page: PDPage,
        pageNo: Int,
        overlays: List<Overlay>,
        addPageNumbers: Boolean,
        cache: HashMap<String, Prepared>,
        pictures: HashMap<File, Bitmap>
    ) {
        val rot = normalize(page.rotation)
        val box = page.cropBox
        val sideways = rot == 90 || rot == 270
        val visualW = if (sideways) box.height else box.width
        val visualH = if (sideways) box.width else box.height

        val applicable = overlays.withIndex().filter { it.value.page == 0 || it.value.page == pageNo }
        if (applicable.isEmpty() && !addPageNumbers) return

        PDPageContentStream(dest, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            for ((i, overlay) in applicable) {
                val key = "$i|$rot|${visualW.toInt()}x${visualH.toInt()}"
                val prepared = cache.getOrPut(key) {
                    prepare(context, dest, overlay, rot, visualW, visualH, pictures)
                }
                when (overlay) {
                    is Overlay.Text -> place(cs, prepared, rot, box, visualW, visualH, overlay.xPct, overlay.yPct)
                    is Overlay.Picture -> place(cs, prepared, rot, box, visualW, visualH, overlay.xPct, overlay.yPct)
                    is Overlay.Watermark -> place(cs, prepared, rot, box, visualW, visualH, 50f, 50f)
                }
            }
            if (addPageNumbers) {
                val bitmap = OverlayBitmaps.text("$pageNo", 10f, 0xFF000000.toInt(), 1f, visualW * 0.5f, 0f)
                val w = bitmap.width / OverlayBitmaps.SCALE
                val h = bitmap.height / OverlayBitmaps.SCALE
                val prepared = fromTemp(dest, bitmap, w, h, rot)
                place(cs, prepared, rot, box, visualW, visualH, 50f, 97f)
            }
        }
    }

    private fun prepare(
        context: Context,
        dest: PDDocument,
        overlay: Overlay,
        rot: Int,
        visualW: Float,
        visualH: Float,
        pictures: HashMap<File, Bitmap>
    ): Prepared {
        return when (overlay) {
            is Overlay.Text -> {
                val bitmap = OverlayBitmaps.text(
                    overlay.text, overlay.sizePt.toFloat(), overlay.color.argb, 1f, visualW * 0.9f, 0f
                )
                val w = bitmap.width / OverlayBitmaps.SCALE
                val h = bitmap.height / OverlayBitmaps.SCALE
                fromTemp(dest, bitmap, w, h, rot)
            }
            is Overlay.Watermark -> {
                val bitmap = OverlayBitmaps.text(
                    overlay.text, 64f, overlay.color.argb, 0.25f, visualW * 0.8f, -45f
                )
                val w = bitmap.width / OverlayBitmaps.SCALE
                val h = bitmap.height / OverlayBitmaps.SCALE
                val fit = min(1f, min(visualW * 0.95f / w, visualH * 0.95f / h))
                fromTemp(dest, bitmap, w * fit, h * fit, rot)
            }
            is Overlay.Picture -> {
                val original = pictures.getOrPut(overlay.file) {
                    ImageUtils.decodeBitmap(context, Uri.fromFile(overlay.file), 1200, 0)
                }
                var w = visualW * overlay.widthPct / 100f
                var h = w * original.height / original.width
                val fit = min(1f, visualH * 0.95f / h)
                w *= fit
                h *= fit
                val toDraw = if (rot == 0) {
                    original
                } else {
                    OverlayBitmaps.rotate(original, ((360 - rot) % 360).toFloat())
                }
                val image = LosslessFactory.createFromImage(dest, toDraw)
                if (toDraw !== original) toDraw.recycle()
                Prepared(image, w, h)
            }
        }
    }

    /** Turns an upright bitmap into a PDF image, pre-rotated so it looks upright on a rotated page. */
    private fun fromTemp(dest: PDDocument, bitmap: Bitmap, widthPt: Float, heightPt: Float, rot: Int): Prepared {
        val toDraw = if (rot == 0) bitmap else OverlayBitmaps.rotate(bitmap, ((360 - rot) % 360).toFloat())
        val image = LosslessFactory.createFromImage(dest, toDraw)
        if (toDraw !== bitmap) toDraw.recycle()
        bitmap.recycle()
        return Prepared(image, widthPt, heightPt)
    }

    private fun place(
        cs: PDPageContentStream,
        prepared: Prepared,
        rot: Int,
        box: PDRectangle,
        visualW: Float,
        visualH: Float,
        xPct: Float,
        yPct: Float
    ) {
        val vx = max(0f, visualW - prepared.widthPt) * xPct / 100f
        val vy = max(0f, visualH - prepared.heightPt) * yPct / 100f
        val a = mapPoint(rot, box, vx, vy)
        val b = mapPoint(rot, box, vx + prepared.widthPt, vy + prepared.heightPt)
        val left = min(a.first, b.first)
        val right = max(a.first, b.first)
        val bottom = min(a.second, b.second)
        val top = max(a.second, b.second)
        cs.drawImage(prepared.image, left, bottom, right - left, top - bottom)
    }

    /** Converts a point on the displayed page (origin top-left, y down) into PDF page space. */
    private fun mapPoint(rot: Int, box: PDRectangle, x: Float, y: Float): Pair<Float, Float> {
        val w0 = box.width
        val h0 = box.height
        val (px, py) = when (rot) {
            90 -> y to x
            180 -> (w0 - x) to y
            270 -> (w0 - y) to (h0 - x)
            else -> x to (h0 - y)
        }
        return Pair(px + box.lowerLeftX, py + box.lowerLeftY)
    }

    private fun normalize(rotation: Int): Int = ((rotation % 360) + 360) % 360
}
