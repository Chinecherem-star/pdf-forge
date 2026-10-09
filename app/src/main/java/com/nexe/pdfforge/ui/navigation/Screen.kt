package com.nexe.pdfforge.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nexe.pdfforge.ui.theme.Amber
import com.nexe.pdfforge.ui.theme.Coral
import com.nexe.pdfforge.ui.theme.Cyan
import com.nexe.pdfforge.ui.theme.Mint
import com.nexe.pdfforge.ui.theme.Pink
import com.nexe.pdfforge.ui.theme.Sky
import com.nexe.pdfforge.ui.theme.Violet

sealed class Screen(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentStart: Color = Violet,
    val accentEnd: Color = Cyan
) {
    object Home : Screen("home", "Home", "", Icons.Rounded.Home)
    object MyFiles : Screen("my_files", "My Files", "Your generated files", Icons.Rounded.Folder)
    object Settings : Screen("settings", "Settings", "Theme, defaults and about", Icons.Rounded.Settings)

    object TextToPdf : Screen(
        "text_to_pdf", "Text to PDF", "Write and format a document", Icons.Rounded.EditNote, Violet, Sky
    )
    object TextConverter : Screen(
        "text_converter", "Text Converter", "PDF, TXT, DOCX, HTML, MD", Icons.Rounded.SwapHoriz, Sky, Cyan
    )
    object ImageToPdf : Screen(
        "image_to_pdf", "Image to PDF", "Turn photos into a PDF", Icons.Rounded.Image, Pink, Violet
    )

    object PdfEditor : Screen(
        "pdf_editor", "PDF Editor", "Pages, text, images, watermark", Icons.Rounded.Edit, Amber, Coral
    )
    object WordEditor : Screen(
        "word_editor", "Word Editor", "Edit the text of .docx files", Icons.Rounded.Description, Sky, Violet
    )
    object LockPdf : Screen(
        "lock_pdf", "Lock PDF", "Add or remove a password", Icons.Rounded.Lock, Mint, Cyan
    )

    object MergePdf : Screen(
        "merge_pdf", "Merge PDF", "Combine several PDFs", Icons.Rounded.Layers, Coral, Pink
    )
    object SplitPdf : Screen(
        "split_pdf", "Split PDF", "Extract pages or ranges", Icons.Rounded.ContentCut, Mint, Sky
    )
    object PdfToImage : Screen(
        "pdf_to_image", "PDF to Image", "Export pages as PNG/JPG", Icons.Rounded.PhotoLibrary, Cyan, Violet
    )

    object PdfInfo : Screen(
        "pdf_info", "PDF Info", "Size, pages and metadata", Icons.Rounded.Info, Violet, Pink
    )
}

val bottomNavItems: List<Screen>
    get() = listOf(Screen.Home, Screen.MyFiles, Screen.Settings)

val toolSections: List<Pair<String, List<Screen>>>
    get() = listOf(
        "Create" to listOf(Screen.TextToPdf, Screen.TextConverter, Screen.ImageToPdf),
        "Edit and protect" to listOf(Screen.PdfEditor, Screen.WordEditor, Screen.LockPdf),
        "Organize" to listOf(Screen.MergePdf, Screen.SplitPdf, Screen.PdfToImage),
        "Inspect" to listOf(Screen.PdfInfo)
    )
