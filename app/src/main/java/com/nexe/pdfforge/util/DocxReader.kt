package com.nexe.pdfforge.util

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/** Reads the plain text of a .docx file, one entry per paragraph. */
object DocxReader {

    private const val MAX_XML_BYTES = 50L * 1024 * 1024

    fun readParagraphs(file: File): List<String> {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("word/document.xml")
                ?: throw IllegalArgumentException("That doesn't look like a Word (.docx) document.")
            if (entry.size > MAX_XML_BYTES) {
                throw IllegalArgumentException("That document is too large to edit here.")
            }
            zip.getInputStream(entry).use { stream ->
                return parse(stream)
            }
        }
    }

    private fun parse(stream: InputStream): List<String> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(stream, "UTF-8")

        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()
        var inParagraph = false
        var inText = false
        var pageBreak = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "p" -> {
                            inParagraph = true
                            current.setLength(0)
                        }
                        "t" -> inText = true
                        "tab" -> if (inParagraph) current.append('\t')
                        "br" -> if (inParagraph) {
                            if (parser.getAttributeValue(null, "type") == "page") {
                                pageBreak = true
                            } else {
                                current.append(' ')
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> inText = false
                        "p" -> if (inParagraph) {
                            paragraphs.add(current.toString())
                            if (pageBreak) {
                                paragraphs.add(PdfUtils.PAGE_BREAK_MARKER)
                                pageBreak = false
                            }
                            inParagraph = false
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inText && inParagraph) current.append(parser.text)
                }
            }
            event = parser.next()
        }
        return paragraphs
    }
}
