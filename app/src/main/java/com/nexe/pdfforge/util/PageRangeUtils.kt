package com.nexe.pdfforge.util

object PageRangeUtils {

    /**
     * Parses text like "1-3, 5, 8-10" into zero-based page indices, in the order written.
     * Throws IllegalArgumentException with a friendly message on bad input.
     */
    fun parse(input: String, total: Int): List<Int> {
        val text = input.trim()
        require(text.isNotEmpty()) { "Enter the pages you want, for example 1-3, 5." }
        val result = mutableListOf<Int>()
        for (part in text.split(",")) {
            val piece = part.trim()
            if (piece.isEmpty()) continue
            val bits = piece.split("-").map { it.trim() }
            val (start, end) = when (bits.size) {
                1 -> {
                    val single = bits[0].toIntOrNull()
                        ?: throw IllegalArgumentException("\"$piece\" is not a valid page number.")
                    single to single
                }
                2 -> {
                    val first = bits[0].toIntOrNull()
                        ?: throw IllegalArgumentException("\"$piece\" is not a valid range.")
                    val last = bits[1].toIntOrNull()
                        ?: throw IllegalArgumentException("\"$piece\" is not a valid range.")
                    first to last
                }
                else -> throw IllegalArgumentException("\"$piece\" is not a valid range.")
            }
            require(start >= 1 && end >= start && end <= total) {
                "Pages must be between 1 and $total."
            }
            for (i in start..end) result.add(i - 1)
        }
        require(result.isNotEmpty()) { "Enter the pages you want, for example 1-3, 5." }
        return result
    }
}
