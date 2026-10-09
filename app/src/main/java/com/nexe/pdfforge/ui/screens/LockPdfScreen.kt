package com.nexe.pdfforge.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexe.pdfforge.ui.components.ResultActions
import com.nexe.pdfforge.ui.components.SectionHeader
import com.nexe.pdfforge.ui.components.ToolScaffold
import com.nexe.pdfforge.ui.viewmodel.LockPdfViewModel
import com.nexe.pdfforge.util.FileUtils

@Composable
fun LockPdfScreen(
    onBack: () -> Unit,
    viewModel: LockPdfViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.consumeHandoff() }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.onPdfPicked(uri) }

    ToolScaffold(
        title = "Lock PDF",
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::consumeMessage
    ) {
        OutlinedButton(
            onClick = { pickLauncher.launch("application/pdf") },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.source == null) "Choose a PDF" else "Choose a different PDF") }

        val source = state.source
        if (source == null) {
            Text(
                text = "Choose a PDF to protect it with a password, or to remove the password from a protected one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(source.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${FileUtils.formatSize(source.sizeBytes)} \u2022 " +
                            if (state.encrypted) "Password protected" else "Not protected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.encrypted) {
                SectionHeader("Remove the password")
                PasswordField(
                    value = state.password,
                    onValueChange = viewModel::setPassword,
                    label = "Current password"
                )
                Button(
                    onClick = viewModel::unlock,
                    enabled = !state.isBusy && state.password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Unlock PDF")
                    }
                }
            } else {
                SectionHeader("Set a password")
                PasswordField(
                    value = state.password,
                    onValueChange = viewModel::setPassword,
                    label = "Password"
                )
                PasswordField(
                    value = state.confirm,
                    onValueChange = viewModel::setConfirm,
                    label = "Confirm password"
                )
                Text(
                    text = "Anyone opening the new file will need this password. If you forget it, the file can't be recovered, and PDF Forge never stores it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = viewModel::lock,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Lock PDF")
                    }
                }
            }
        }

        val file = state.resultFile
        if (file != null) {
            SectionHeader("Result")
            Text(
                text = "${file.name} \u2022 ${FileUtils.formatSize(file.length())}",
                style = MaterialTheme.typography.bodyMedium
            )
            ResultActions(file = file, mime = "application/pdf", onMessage = viewModel::showMessage)
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
