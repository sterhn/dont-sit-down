package com.dontsitdown.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.dontsitdown.app
import com.dontsitdown.data.model.Frequency
import com.dontsitdown.data.model.Task
import com.dontsitdown.ui.components.BigButton
import com.dontsitdown.ui.components.QuietAction
import com.dontsitdown.ui.components.SectionLabel
import com.dontsitdown.ui.components.Stepper
import com.dontsitdown.ui.components.ToggleRow
import com.dontsitdown.ui.components.TopBar
import kotlinx.coroutines.launch

@Composable
fun AddEditTaskScreen(id: String?, onBack: () -> Unit, onDeleted: () -> Unit) {
    val repo = LocalContext.current.app.repository
    val scope = rememberCoroutineScope()
    var original by remember { mutableStateOf<Task?>(null) }
    var name by remember { mutableStateOf("") }
    var estimate by remember { mutableIntStateOf(5) }
    var frequency by remember { mutableStateOf(Frequency.WEEKLY) }
    var active by remember { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null) repo.task(id)?.let {
            original = it
            name = it.name
            estimate = it.estimateMinutes
            frequency = it.frequency
            active = it.isActive
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(if (id == null) "new task" else "edit", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).imePadding().navigationBarsPadding(),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )

            SectionLabel("estimate")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(estimate, { estimate = it }, 1..120, suffix = " min")
            }
            original?.takeIf { it.actualTimes.isNotEmpty() }?.let {
                Text(
                    "chains use your real average (${String.format(java.util.Locale.US, "%.1f", it.effectiveMinutes)} min) once there is one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            SectionLabel("how often")
            Frequency.entries.forEach { f ->
                Row(
                    Modifier.fillMaxWidth().clickable { frequency = f }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = frequency == f,
                        onClick = { frequency = f },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                    )
                    Text(f.label, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(8.dp))
            ToggleRow("active", active, { active = it }, subtitle = "off = never shows up, history kept")

            Spacer(Modifier.height(28.dp))
            BigButton("save", enabled = name.isNotBlank(), onClick = {
                scope.launch {
                    val base = original ?: Task(name = name.trim(), estimateMinutes = estimate, frequency = frequency)
                    repo.save(base.copy(name = name.trim(), estimateMinutes = estimate, frequency = frequency, isActive = active))
                    onBack()
                }
            })
            if (original != null) {
                QuietAction("delete", onClick = { confirmDelete = true }, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("delete ${original?.name}?") },
            text = { Text("its history in past chains stays. switching it off is usually enough.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        original?.let { repo.delete(it) }
                        onDeleted()
                    }
                }) { Text("delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("keep") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}
