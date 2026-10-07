package com.nexe.pdfforge.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.nexe.pdfforge.data.model.ListStyle
import com.nexe.pdfforge.data.model.TextAlignOption
import com.nexe.pdfforge.data.model.TextPdfOptions
import java.io.File
import java.io.FileOutputStream

object PdfUtils {

    const val PAGE_BREAK_MARKER = "[page-break]"
    private const val MAX_CHARS = 1_000_000
    private val pageBreakRegex = Regex("(?m)^[ \\t]*\\[page-break\\][ \\t]*$")

    /**
     * Builds a PDF from plain text with simple whole-document formatting.
     * Throws IllegalArgumentException with a user-friendly message for bad input.
     */
    fun generateTextPdf(options: TextPdfOptions, outFile: File) {
        require(options.body.isNotBlank() || options.title.isNotBlank()) {
            "Add a title or some text first."
        }
        require(options.body.length <= MAX_CHARS) {
            "That text is too large to convert."
        }

        val baseW = options.pageSize.widthPt
        val baseH = options.pageSize.heightPt
        val pageW = if (options.landscape) baseH else baseW
        val pageH = if (options.landscape) baseW else baseH
        val margin = options.margin.points
        val contentW = pageW - margin * 2
        val contentBottom = (pageH - margin).toFloat()

        val style = when {
            options.bold && options.italic -> Typeface.BOLD_ITALIC
            options.bold -> Typeface.BOLD
            options.italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = options.fontSize.toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, style)
            isUnderlineText = options.underline
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = (options.fontSize + 8).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val sections = options.body
            .split(pageBreakRegex)
            .map { it.trim('\n', '\r') }

        val doc = PdfDocument()
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f

        fun startNewPage() {
            page?.let { doc.finishPage(it) }
            pageNumber++
            val newPage = doc.startPage(
                PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create()
            )
            page = newPage
            canvas = newPage.canvas
            y = margin.toFloat()
        }

        fun drawLayout(layout: StaticLayout, spaceAfter: Float) {
            for (i in 0 until layout.lineCount) {
                val top = layout.getLineTop(i)
                val bottom = layout.getLineBottom(i)
                val lineHeight = (bottom - top).toFloat()
                if (y + lineHeight > contentBottom && y > margin) {
                    startNewPage()
                }
                val c = canvas!!
                c.save()
                c.translate(margin.toFloat(), y - top)
                c.clipRect(0f, top.toFloat(), contentW.toFloat(), bottom.toFloat())
                layout.draw(c)
                c.restore()
                y += lineHeight
            }
            y += spaceAfter
        }

        try {
            startNewPage()

            if (options.title.isNotBlank()) {
                drawLayout(
                    buildLayout(options.title.trim(), titlePaint, contentW, TextAlignOption.LEFT),
                    12f
                )
            }

            sections.forEachIndexed { index, section ->
                if (index > 0) startNewPage()
                var counter = 0
                section.split("\n").forEach { raw ->
                    val line = raw.trimEnd('\r')
                    val text = when {
                        line.isBlank() -> ""
                        options.listStyle == ListStyle.BULLET -> "\u2022  $line"
                        options.listStyle == ListStyle.NUMBERED -> {
                            counter++
                            "$counter.  $line"
                        }
                        else -> line
                    }
                    drawLayout(
                        buildLayout(text, bodyPaint, contentW, options.align),
                        if (line.isBlank()) 0f else 4f
                    )
                }
            }

            page?.let { doc.finishPage(it) }
            outFile.parentFile?.mkdirs()
            FileOutputStream(outFile).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    private fun buildLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        align: TextAlignOption
    ): StaticLayout {
        val alignment = when (align) {
            TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignOption.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        val builder = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(alignment)
            .setLineSpacing(0f, 1.25f)
            .setIncludePad(false)
        if (align == TextAlignOption.JUSTIFY) {
            builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
        }
        return builder.build()
    }

    /** Renders one page of a PDF to a bitmap scaled to [targetWidth] pixels wide. */
    fun renderPage(file: File, pageIndex: Int, targetWidth: Int): Bitmap {
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                require(pageIndex in 0 until renderer.pageCount) { "Page not found." }
                renderer.openPage(pageIndex).use { page ->
                    val scale = targetWidth.toFloat() / page.width
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }

    fun pageCount(file: File): Int {
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { it.pageCount }
        }
    }
}
