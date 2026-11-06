package com.mangaauthority.tvai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mangaauthority.tvai.ui.components.focusRing

private val PillShape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onTest: () -> Unit,
    onSelectModel: (String) -> Unit,
    onTemperature: (Float) -> Unit,
    onStreaming: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var keyInput by remember { mutableStateOf("") } // kept out of saved state on purpose
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            FilledTonalButton(
                onClick = onBack,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
            ) { Text("Back") }
            Text("Settings", style = MaterialTheme.typography.headlineLargeEmphasized, color = scheme.onSurface)
        }

        SettingsCard("Gemini API key") {
            Text(
                if (state.hasKey) "A key is configured." else "No key yet. Create one in Google AI Studio and paste it here.",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
            )
            val fieldShape = MaterialTheme.shapes.large
            TextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                modifier = Modifier.fillMaxWidth().focusRing(fieldShape),
                label = { Text(if (state.hasKey) "Paste a new key to replace it" else "API key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onSaveKey(keyInput)
                    keyInput = ""
                }),
                shape = fieldShape,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        onSaveKey(keyInput)
                        keyInput = ""
                    },
                    enabled = keyInput.isNotBlank(),
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
                ) { Text("Save key") }
                FilledTonalButton(
                    onClick = onTest,
                    enabled = state.hasKey && !state.busy,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
                ) { Text("Test connection") }
                FilledTonalButton(
                    onClick = onClearKey,
                    enabled = state.hasKey,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
                ) { Text("Remove key") }
            }
            state.status?.let {
                Text(
                    it.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (it.isError) scheme.error else scheme.primary,
                )
            }
        }

        SettingsCard("Model") {
            Text("Current: ${state.model}", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            if (state.models.isEmpty()) {
                Text(
                    "Use Test connection to load the models your key can use.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            } else {
                state.models.take(40).forEach { m ->
                    val label = if (m.displayName == m.id) m.id else "${m.displayName}  (${m.id})"
                    if (m.id == state.model) {
                        Button(
                            onClick = { onSelectModel(m.id) },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).focusRing(PillShape),
                        ) { Text("\u2713  $label") }
                    } else {
                        FilledTonalButton(
                            onClick = { onSelectModel(m.id) },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).focusRing(PillShape),
                        ) { Text(label) }
                    }
                }
            }
        }

        SettingsCard("Chat") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Creativity: ${"%.1f".format(state.temperature)}", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                FilledTonalButton(
                    onClick = { onTemperature(-0.1f) },
                    enabled = state.temperature > 0f,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 52.dp).focusRing(PillShape),
                ) { Text("Lower") }
                FilledTonalButton(
                    onClick = { onTemperature(0.1f) },
                    enabled = state.temperature < 2f,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 52.dp).focusRing(PillShape),
                ) { Text("Higher") }
            }
            val shape = MaterialTheme.shapes.large
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRing(shape)
                    .toggleable(value = state.streaming, role = Role.Switch, onValueChange = onStreaming)
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Switch(checked = state.streaming, onCheckedChange = null)
                Text("Show replies as they are written (streaming)", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            }
        }

        SettingsCard("About and privacy") {
            Text("AndroidTVAI 1.0", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            Text(
                "Your key is encrypted with the Android Keystore and stays on this device. Messages go straight from this " +
                    "device to Google's Gemini API; there is no other server. A key stored in an app can still be extracted " +
                    "from a rooted or compromised device, so use a key you can revoke. Usage counts against your Google " +
                    "account's free quota or billing.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}
