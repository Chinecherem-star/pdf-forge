package com.nexe.pdfforge.data.repository

import android.content.Context
import com.nexe.pdfforge.data.model.PdfInfo
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.io.File
import kotlin.math.roundToInt

/** Merge / extract / inspect / lock PDFs using the Android port of PDFBox. */
class PdfRepository(context: Context) {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    fun merge(inputs: List<File>, output: File) {
        require(inputs.size >= 2) { "Add at least two PDFs to merge." }
        val opened = mutableListOf<PDDocument>()
        val destination = PDDocument()
        try {
            for (file in inputs) {
                val source = PDDocument.load(file)
                opened.add(source)
                for (page in source.pages) {
                    destination.importPage(page)
                }
            }
            output.parentFile?.mkdirs()
            destination.save(output)
        } finally {
            opened.forEach { runCatching { it.close() } }
            runCatching { destination.close() }
        }
    }

    fun extractPages(input: File, pageIndices: List<Int>, output: File) {
        require(pageIndices.isNotEmpty()) { "Choose at least one page." }
        PDDocument.load(input).use { source ->
            val total = source.numberOfPages
            require(pageIndices.all { it in 0 until total }) {
                "Some pages are outside this document (1-$total)."
            }
            PDDocument().use { destination ->
                pageIndices.forEach { destination.importPage(source.getPage(it)) }
                output.parentFile?.mkdirs()
                destination.save(output)
            }
        }
    }

    fun inspect(file: File, displayName: String): PdfInfo {
        return PDDocument.load(file).use { doc ->
            val info = doc.documentInformation
            val box = if (doc.numberOfPages > 0) doc.getPage(0).mediaBox else null
            PdfInfo(
                name = displayName,
                sizeBytes = file.length(),
                pageCount = doc.numberOfPages,
                pdfVersion = doc.version.toString(),
                encrypted = doc.isEncrypted,
                firstPageSize = box?.let { "${it.width.roundToInt()} \u00D7 ${it.height.roundToInt()} pt" },
                title = info.title.clean(),
                author = info.author.clean(),
                subject = info.subject.clean(),
                creator = info.creator.clean(),
                producer = info.producer.clean(),
                keywords = info.keywords.clean(),
                created = info.creationDate?.timeInMillis,
                modified = info.modificationDate?.timeInMillis
            )
        }
    }

    /** True if the PDF needs a password to open (or has any encryption). */
    fun isEncrypted(file: File): Boolean {
        return try {
            PDDocument.load(file).use { it.isEncrypted }
        } catch (e: InvalidPasswordException) {
            true
        }
    }

    /** Writes a copy of [input] that asks for [password] to open (AES 128-bit). */
    fun protect(input: File, output: File, password: String) {
        require(password.length >= 4) { "Use a password with at least 4 characters." }
        PDDocument.load(input).use { doc ->
            require(!doc.isEncrypted) { "This PDF is already password protected." }
            val policy = StandardProtectionPolicy(password, password, AccessPermission())
            policy.encryptionKeyLength = 128
            policy.setPreferAES(true)
            doc.protect(policy)
            output.parentFile?.mkdirs()
            doc.save(output)
        }
    }

    /** Writes a copy of [input] with the password removed. Needs the correct password. */
    fun unlock(input: File, password: String, output: File) {
        PDDocument.load(input, password).use { doc ->
            require(doc.isEncrypted) { "This PDF isn't password protected." }
            doc.isAllSecurityToBeRemoved = true
            output.parentFile?.mkdirs()
            doc.save(output)
        }
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
