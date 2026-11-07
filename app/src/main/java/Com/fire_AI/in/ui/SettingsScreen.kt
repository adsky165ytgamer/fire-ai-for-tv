package Com.fire_AI.`in`.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import Com.fire_AI.`in`.ui.components.TextEntry
import Com.fire_AI.`in`.ui.components.detectInputCaps
import Com.fire_AI.`in`.ui.components.focusRing

private val PillShape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    isTv: Boolean,
    useFireKeyboard: Boolean,
    onBack: () -> Unit,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onTest: () -> Unit,
    onKeyboardMode: (String) -> Unit,
    onAutoSendVoice: (Boolean) -> Unit,
    onTemperature: (Float) -> Unit,
    onStreaming: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var keyInput by remember { mutableStateOf("") } // kept out of saved state on purpose
    var kbOpen by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val caps = remember { detectInputCaps(context) }

    Column(
        modifier = modifier.imePadding().verticalScroll(rememberScrollState()),
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
                if (state.hasKey) "A key is configured." else "No key yet. Create one in Google AI Studio, then type or paste it here.",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
            )
            TextEntry(
                value = keyInput,
                onValueChange = { keyInput = it },
                placeholder = if (state.hasKey) "Enter a new key to replace it" else "API key",
                useFireKeyboard = useFireKeyboard,
                isTv = isTv,
                keyboardVisible = kbOpen,
                onKeyboardVisibleChange = { kbOpen = it },
                onSubmit = {
                    if (keyInput.isNotBlank()) {
                        onSaveKey(keyInput)
                        keyInput = ""
                    }
                },
                submitLabel = "Save",
                secret = true,
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onSaveKey(keyInput); keyInput = "" },
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
                Text(it.text, style = MaterialTheme.typography.bodyLarge, color = if (it.isError) scheme.error else scheme.primary)
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
            SwitchRow("Show replies as they are written (streaming)", state.streaming, onStreaming)
        }

        SettingsCard("Keyboard") {
            Text(
                "Fire AI has its own on-screen keyboard that stays inside the app. The system keyboard is still available.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("auto" to "Automatic", "fire" to "Fire AI keyboard", "system" to "System keyboard").forEach { (mode, label) ->
                    val mod = Modifier.heightIn(min = 52.dp).focusRing(PillShape)
                    if (state.keyboardMode == mode) {
                        Button(onClick = { onKeyboardMode(mode) }, shapes = ButtonDefaults.shapes(), modifier = mod) { Text("\u2713  $label") }
                    } else {
                        FilledTonalButton(onClick = { onKeyboardMode(mode) }, shapes = ButtonDefaults.shapes(), modifier = mod) { Text(label) }
                    }
                }
            }
            Text(
                "Automatic uses the Fire AI keyboard on TV and the system keyboard on phones. Now using: " +
                    if (useFireKeyboard) "Fire AI keyboard." else "system keyboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }

        SettingsCard("Voice typing") {
            Text(
                "The Mic button turns speech into text you can edit before sending. It needs a speech service on the device, " +
                    "and many TV boxes without Google services don't have one.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            SwitchRow("Send voice messages automatically", state.autoSendVoice, onAutoSendVoice)
        }

        SettingsCard("Detected input devices") {
            Text("Read from Android's input system on this device.", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            listOf(
                "Touchscreen" to caps.touchscreen,
                "Mouse or air mouse" to caps.mouse,
                "Physical keyboard" to caps.keyboard,
                "D-pad or remote" to caps.dpad,
                "Gamepad" to caps.gamepad,
            ).forEach { (name, present) ->
                Text("$name: ${if (present) "yes" else "not detected"}", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            }
        }

        SettingsCard("About and privacy") {
            Text("Fire AI 0.2", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            Text(
                "Your key is encrypted with the Android Keystore and stays on this device. Messages go straight from this " +
                    "device to Google's Gemini API; there is no other server. A key stored in an app can still be extracted " +
                    "from a rooted or compromised device, so use a key you can revoke. Usage counts against your Google " +
                    "account's free quota or billing, and Google may use free-tier content to improve its products.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRing(shape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Switch(checked = checked, onCheckedChange = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}
