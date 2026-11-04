package com.dontsitdown.ui.checkin

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dontsitdown.app
import com.dontsitdown.checkin.CheckinData
import com.dontsitdown.checkin.CheckinJson
import com.dontsitdown.ui.components.OutlineButton
import com.dontsitdown.ui.theme.LocalPaper

/**
 * Bring check-ins over from the browser app: pick the file it exported, or paste what its
 * "copy data" button copied. Either way you see what's in it before anything is replaced.
 */
@Composable
fun CheckinImportButtons(modifier: Modifier = Modifier, onImported: () -> Unit = {}) {
    val context = LocalContext.current
    val store = context.app.checkin
    val clipboard = LocalClipboardManager.current
    var pending by remember { mutableStateOf<CheckinData?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val parsed = runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
            .getOrNull()?.let { CheckinJson.decode(it) }
        if (parsed == null) Toast.makeText(context, "That file doesn't look like check-in data", Toast.LENGTH_LONG).show()
        else pending = parsed
    }

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlineButton("Open file", onClick = { picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) }, modifier = Modifier.weight(1f))
        OutlineButton("Paste", onClick = {
            val text = clipboard.getText()?.text
            val parsed = text?.let { CheckinJson.decode(it) }
            when {
                text.isNullOrBlank() -> Toast.makeText(context, "The clipboard is empty. In the browser app: Progress → Copy data", Toast.LENGTH_LONG).show()
                parsed == null -> Toast.makeText(context, "What's on the clipboard isn't check-in data", Toast.LENGTH_LONG).show()
                else -> pending = parsed
            }
        }, modifier = Modifier.weight(1f))
    }

    pending?.let { d ->
        val current = store.current
        val replacing = current.days.isNotEmpty() || current.log.isNotEmpty()
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Import this?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "${d.days.size} days, ${d.goals.size} goals, ${d.log.size} logged, ${d.boundaries.size} boundaries." +
                        if (replacing) "\n\nThis replaces the check-ins and goals on this phone. your chores and tasks aren't touched."
                        else "\n\nYour chores and tasks aren't touched.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.replaceAll(d)
                    pending = null
                    Toast.makeText(context, "Imported. Welcome back.", Toast.LENGTH_SHORT).show()
                    onImported()
                }) { Text("Import", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel", style = MaterialTheme.typography.labelLarge) } },
            containerColor = LocalPaper.current.card,
        )
    }
}
