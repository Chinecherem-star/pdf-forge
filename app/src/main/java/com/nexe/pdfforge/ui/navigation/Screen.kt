package com.nexe.pdfforge.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "Home", "", Icons.Rounded.Home)
    object MyFiles : Screen("my_files", "My Files", "Your generated files", Icons.Rounded.Folder)
    object Settings : Screen("settings", "Settings", "Theme, defaults and about", Icons.Rounded.Settings)

    object TextToPdf : Screen("text_to_pdf", "Text to PDF", "Write and format a document", Icons.Rounded.EditNote)
    object TextConverter : Screen("text_converter", "Text Converter", "PDF, TXT, DOCX, HTML, MD", Icons.Rounded.SwapHoriz)
    object ImageToPdf : Screen("image_to_pdf", "Image to PDF", "Turn photos into a PDF", Icons.Rounded.Image)

    object MergePdf : Screen("merge_pdf", "Merge PDF", "Combine several PDFs", Icons.Rounded.Layers)
    object SplitPdf : Screen("split_pdf", "Split PDF", "Extract pages or ranges", Icons.Rounded.ContentCut)
    object PdfToImage : Screen("pdf_to_image", "PDF to Image", "Export pages as PNG/JPG", Icons.Rounded.PhotoLibrary)

    object PdfInfo : Screen("pdf_info", "PDF Info", "Size, pages and metadata", Icons.Rounded.Info)
}

val bottomNavItems: List<Screen>
    get() = listOf(Screen.Home, Screen.MyFiles, Screen.Settings)

val toolSections: List<Pair<String, List<Screen>>>
    get() = listOf(
        "Create" to listOf(Screen.TextToPdf, Screen.TextConverter, Screen.ImageToPdf),
        "Organize" to listOf(Screen.MergePdf, Screen.SplitPdf, Screen.PdfToImage),
        "Inspect" to listOf(Screen.PdfInfo)
    )
