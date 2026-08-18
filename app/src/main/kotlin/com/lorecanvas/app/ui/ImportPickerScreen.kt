package com.lorecanvas.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorecanvas.app.ui.theme.Ok
import com.lorecanvas.app.ui.theme.Pending
import java.io.File

/** Import — LCD-009 Ch.15's "Select File" step. No system file picker (see README's scoping note); lists previously-exported files. */
@Composable
fun ImportPickerScreen(
    exportFiles: List<File>,
    statusMessage: String?,
    isError: Boolean,
    onSelectFile: (File) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("← Back to Workspace") }
        Spacer(Modifier.height(16.dp))
        Text("Import", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        if (statusMessage != null) {
            Text(statusMessage, style = MaterialTheme.typography.bodyMedium, color = if (isError) Pending else Ok)
            Spacer(Modifier.height(16.dp))
        }

        if (exportFiles.isEmpty()) {
            Text("No exported project files found. Use Export from the Workspace first.", style = MaterialTheme.typography.bodyMedium)
        } else {
            exportFiles.forEach { file ->
                OutlinedButton(
                    onClick = { onSelectFile(file) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text(file.name) }
            }
        }
    }
}
