package com.nexe.pdfforge.data.repository

import android.content.Context
import com.nexe.pdfforge.data.model.PdfFileModel
import com.nexe.pdfforge.util.FileUtils
import java.io.File
import java.io.IOException

class FileRepository(private val context: Context) {

    fun listFiles(): List<PdfFileModel> =
        FileUtils.outputDir(context)
            .walkTopDown()
            .filter { it.isFile }
            .map { PdfFileModel.from(it) }
            .toList()

    fun rename(file: File, newBaseName: String): File {
        val clean = newBaseName.replace(Regex("[^A-Za-z0-9 _.-]"), "").trim().trim('.')
        require(clean.isNotEmpty()) { "Enter a valid file name." }
        val name = if (file.extension.isEmpty()) clean else "$clean.${file.extension}"
        val target = File(file.parentFile, name)
        if (target == file) return file
        require(!target.exists()) { "A file with that name already exists." }
        if (!file.renameTo(target)) throw IOException("Couldn't rename the file.")
        return target
    }

    fun delete(file: File): Boolean = file.delete()
}
