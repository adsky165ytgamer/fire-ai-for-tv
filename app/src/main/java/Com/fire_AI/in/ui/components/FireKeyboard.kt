package Com.fire_AI.`in`.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LETTER_ROWS = listOf(
    "qwertyuiop".map { it.toString() },
    "asdfghjkl".map { it.toString() },
    "zxcvbnm".map { it.toString() },
)
private val SYMBOL_ROWS = listOf(
    "1234567890".map { it.toString() },
    listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")"),
    listOf("!", "\"", "'", ":", ";", "/", "?", "_", "="),
)

/**
 * In-app keyboard for remotes, air-mouse and touch. It is plain composables (no animation, one
 * focus state per key) so it stays light on weak TV boxes. It is not a system keyboard.
 */
@Composable
fun FireKeyboard(
    isTv: Boolean,
    submitLabel: String,
    firstKeyFocus: FocusRequester,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onPaste: () -> Unit,
    onSubmit: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shift by remember { mutableIntStateOf(0) } // 0 off, 1 next letter, 2 caps lock
    var symbols by remember { mutableStateOf(false) }
    val keyHeight = if (isTv) 46.dp else 40.dp
    val rows = if (symbols) SYMBOL_ROWS else LETTER_ROWS

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEachIndexed { i, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!symbols && i == 2) {
                    KeyCap(
                        label = when (shift) { 0 -> "Shift"; 1 -> "SHIFT"; else -> "CAPS" },
                        modifier = Modifier.weight(1.6f),
                        height = keyHeight,
                        accent = shift > 0,
                    ) { shift = (shift + 1) % 3 }
                }
                row.forEach { key ->
                    val label = if (!symbols && shift > 0) key.uppercase() else key
                    KeyCap(
                        label = label,
                        modifier = Modifier.weight(1f),
                        height = keyHeight,
                        requester = if (!symbols && i == 1 && key == "g") firstKeyFocus else null,
                    ) {
                        onChar(label)
                        if (shift == 1) shift = 0
                    }
                }
                if (i == 2) KeyCap("Del", Modifier.weight(1.6f), keyHeight, onClick = onBackspace)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyCap(if (symbols) "ABC" else "?123", Modifier.weight(1.4f), keyHeight) { symbols = !symbols }
            KeyCap("Paste", Modifier.weight(1.4f), keyHeight, onClick = onPaste)
            KeyCap("Hide", Modifier.weight(1.4f), keyHeight, onClick = onHide)
            KeyCap(",", Modifier.weight(1f), keyHeight) { onChar(",") }
            KeyCap("Space", Modifier.weight(3.5f), keyHeight) { onChar(" ") }
            KeyCap(".", Modifier.weight(1f), keyHeight) { onChar(".") }
            KeyCap(submitLabel, Modifier.weight(1.8f), keyHeight, accent = true, onClick = onSubmit)
        }
    }
}

@Composable
private fun KeyCap(
    label: String,
    modifier: Modifier,
    height: Dp,
    accent: Boolean = false,
    requester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val showFocus = LocalShowFocus.current
    val scheme = MaterialTheme.colorScheme
    val highlighted = focused && showFocus
    val bg = when {
        highlighted -> scheme.primary
        accent -> scheme.primaryContainer
        else -> scheme.surfaceContainerHighest
    }
    val fg = when {
        highlighted -> scheme.onPrimary
        accent -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
    }
}
