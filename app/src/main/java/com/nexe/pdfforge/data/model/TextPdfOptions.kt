package com.nexe.pdfforge.data.model

enum class PageSizeOption(val label: String, val widthPt: Int, val heightPt: Int) {
    A4("A4", 595, 842),
    A5("A5", 420, 595),
    LETTER("Letter", 612, 792),
    LEGAL("Legal", 612, 1008)
}

enum class TextAlignOption(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right"),
    JUSTIFY("Justify")
}

enum class ListStyle(val label: String) {
    NONE("Plain"),
    BULLET("Bullets"),
    NUMBERED("Numbered")
}

enum class MarginOption(val label: String, val points: Int) {
    NARROW("Narrow", 36),
    NORMAL("Normal", 54),
    WIDE("Wide", 72)
}

data class TextPdfOptions(
    val title: String = "",
    val body: String = "",
    val fontSize: Int = 12,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val align: TextAlignOption = TextAlignOption.LEFT,
    val listStyle: ListStyle = ListStyle.NONE,
    val pageSize: PageSizeOption = PageSizeOption.A4,
    val landscape: Boolean = false,
    val margin: MarginOption = MarginOption.NORMAL
)
