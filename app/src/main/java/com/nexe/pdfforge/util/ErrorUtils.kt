package com.nexe.pdfforge.util

import java.io.IOException

/** Turns any failure into a short message that is safe to show to the user. */
fun friendlyMessage(error: Throwable, action: String): String = when {
    error is IllegalArgumentException ->
        error.message ?: "Please check your input and try again."
    error is OutOfMemoryError ->
        "That file is too large to process on this device."
    error is SecurityException ->
        "That PDF is password protected or can't be opened."
    error.javaClass.simpleName == "InvalidPasswordException" ->
        "That PDF is password protected."
    error is IOException && (error.message == "No space" || error.message?.contains("ENOSPC") == true) ->
        "Not enough storage space. Free up some space and try again."
    error is IOException ->
        "Couldn't read or write the file. It may be corrupted or in an unsupported format."
    else ->
        "Something went wrong while $action."
}
