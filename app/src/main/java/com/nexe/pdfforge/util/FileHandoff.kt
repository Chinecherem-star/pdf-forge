package com.nexe.pdfforge.util

import java.io.File

/** Lets My Files hand a file to another tool screen without copying it. */
object FileHandoff {

    private var targetRoute: String? = null
    private var pendingFile: File? = null

    fun put(route: String, file: File) {
        targetRoute = route
        pendingFile = file
    }

    /** Returns the waiting file only if it was meant for [route], and clears it. */
    fun take(route: String): File? {
        if (targetRoute != route) return null
        val file = pendingFile
        targetRoute = null
        pendingFile = null
        return file
    }
}
