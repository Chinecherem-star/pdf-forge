package com.nexe.pdfforge.util

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocumentFormatter {

    private val pageBreakLine = Regex("(?m)^[ \\t]*\\[page-break\\][ \\t]*\\r?\\n?")

    private fun cleanBody(body: String): String = body.replace(pageBreakLine, "")

    fun toTxt(title: String, body: String): String {
        val text = cleanBody(body)
        return if (title.isBlank()) text else title.trim() + "\n\n" + text
    }

    fun toMarkdown(title: String, body: String): String {
        val text = cleanBody(body)
        return if (title.isBlank()) text else "# " + title.trim() + "\n\n" + text
    }

    fun toHtml(title: String, body: String): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
        sb.append("<meta charset=\"utf-8\">\n")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
        sb.append("<title>").append(escapeXml(title.ifBlank { "Document" })).append("</title>\n")
        sb.append("<style>body{font-family:sans-serif;max-width:720px;margin:2rem auto;padding:0 1rem;line-height:1.6}</style>\n")
        sb.append("</head>\n<body>\n")
        if (title.isNotBlank()) {
            sb.append("<h1>").append(escapeXml(title.trim())).append("</h1>\n")
        }
        cleanBody(body)
            .split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { paragraph ->
                sb.append("<p>")
                    .append(escapeXml(paragraph).replace("\n", "<br>\n"))
                    .append("</p>\n")
            }
        sb.append("</body>\n</html>\n")
        return sb.toString()
    }

    fun writeDocx(title: String, body: String, outFile: File) {
        outFile.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry(
                "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
                    "</Types>"
            )
            entry(
                "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
                    "</Relationships>"
            )
            entry("word/document.xml", documentXml(title, body))
        }
    }

    private fun documentXml(title: String, body: String): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>")
        if (title.isNotBlank()) {
            sb.append("<w:p><w:r><w:rPr><w:b/><w:sz w:val=\"40\"/></w:rPr><w:t xml:space=\"preserve\">")
                .append(escapeXml(title.trim()))
                .append("</w:t></w:r></w:p>")
        }
        body.split("\n").forEach { raw ->
            val line = raw.trimEnd('\r')
            if (line.trim() == PdfUtils.PAGE_BREAK_MARKER) {
                sb.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
            } else {
                sb.append("<w:p><w:r><w:t xml:space=\"preserve\">")
                    .append(escapeXml(line))
                    .append("</w:t></w:r></w:p>")
            }
        }
        sb.append("</w:body></w:document>")
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        val sb = StringBuilder(text.length + 16)
        for (ch in text) {
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch == '\'' -> sb.append("&apos;")
                ch == '\n' || ch == '\t' || ch >= ' ' -> sb.append(ch)
                else -> Unit
            }
        }
        return sb.toString()
    }
}
