package com.nexe.pdfforge.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexe.pdfforge.util.FileUtils
import java.io.File

/** Open / Share / Save as buttons for a generated file. */
@Composable
fun ResultActions(
    file: File,
    mime: String,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mime)
    ) { uri ->
        if (uri != null) {
            val ok = FileUtils.copyToUri(context, file, uri)
            onMessage(if (ok) "Saved" else "Couldn't save to that location")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                if (!FileUtils.openFile(context, file, mime)) {
                    onMessage("No app found to open this file")
                }
            },
            modifier = Modifier.weight(1f)
        ) { Text("Open") }
        OutlinedButton(
            onClick = {
                if (!FileUtils.shareFile(context, file, mime)) {
                    onMessage("Couldn't open the share sheet")
                }
            },
            modifier = Modifier.weight(1f)
        ) { Text("Share") }
        OutlinedButton(
            onClick = { saveLauncher.launch(file.name) },
            modifier = Modifier.weight(1f)
        ) { Text("Save as") }
    }
}
