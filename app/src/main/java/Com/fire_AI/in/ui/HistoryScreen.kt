package Com.fire_AI.`in`.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import Com.fire_AI.`in`.data.ConversationSummary
import Com.fire_AI.`in`.ui.components.TextEntry
import Com.fire_AI.`in`.ui.components.focusRing
import java.text.DateFormat
import java.util.Date

private val PillShape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    items: List<ConversationSummary>,
    isTv: Boolean,
    useFireKeyboard: Boolean,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    var renaming by remember { mutableStateOf<ConversationSummary?>(null) }
    var draft by remember { mutableStateOf("") }
    var kbOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    // Back closes the rename panel before leaving History (the keyboard handles its own Back first).
    BackHandler(enabled = renaming != null) {
        renaming = null
        kbOpen = false
    }

    fun saveRename() {
        val target = renaming
        if (target != null && draft.isNotBlank()) {
            onRename(target.id, draft)
            renaming = null
            kbOpen = false
        }
    }

    Column(modifier = modifier.imePadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            FilledTonalButton(
                onClick = onBack,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
            ) { Text("Back") }
            Text("History", style = MaterialTheme.typography.headlineLargeEmphasized, color = scheme.onSurface)
        }

        if (items.isEmpty()) {
            Text("No saved conversations yet.", style = MaterialTheme.typography.titleLarge, color = scheme.onSurfaceVariant)
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.id }) { c ->
                    val confirming = confirmDelete == c.id
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { confirmDelete = null; onOpen(c.id) },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f).heightIn(min = 64.dp).focusRing(PillShape),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(c.title.ifBlank { "Untitled chat" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(dateFormat.format(Date(c.updatedAt)), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                confirmDelete = null
                                renaming = c
                                draft = c.title
                            },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.heightIn(min = 64.dp).focusRing(PillShape),
                        ) { Text("Rename") }
                        Button(
                            onClick = {
                                if (confirming) { confirmDelete = null; onDelete(c.id) } else confirmDelete = c.id
                            },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.buttonColors(containerColor = scheme.errorContainer, contentColor = scheme.onErrorContainer),
                            modifier = Modifier.heightIn(min = 64.dp).focusRing(PillShape),
                        ) { Text(if (confirming) "Confirm delete" else "Delete") }
                    }
                }
            }
        }

        renaming?.let {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Rename conversation", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                TextEntry(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = "Conversation name",
                    useFireKeyboard = useFireKeyboard,
                    isTv = isTv,
                    keyboardVisible = kbOpen,
                    onKeyboardVisibleChange = { kbOpen = it },
                    onSubmit = ::saveRename,
                    submitLabel = "Save",
                    singleLine = true,
                    trailing = {
                        Button(
                            onClick = ::saveRename,
                            enabled = draft.isNotBlank(),
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
                        ) { Text("Save") }
                        FilledTonalButton(
                            onClick = { renaming = null; kbOpen = false },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
                        ) { Text("Cancel") }
                    },
                )
            }
        }
    }
}
