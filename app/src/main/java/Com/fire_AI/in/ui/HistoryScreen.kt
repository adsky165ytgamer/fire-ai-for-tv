package Com.fire_AI.`in`.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import Com.fire_AI.`in`.data.ConversationSummary
import Com.fire_AI.`in`.ui.components.focusRing
import java.text.DateFormat
import java.util.Date

private val PillShape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    items: List<ConversationSummary>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { c ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onOpen(c.id) },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f).heightIn(min = 64.dp).focusRing(PillShape),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(c.title.ifBlank { "Untitled chat" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    dateFormat.format(Date(c.updatedAt)),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        Button(
                            onClick = { onDelete(c.id) },
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.errorContainer,
                                contentColor = scheme.onErrorContainer,
                            ),
                            modifier = Modifier.heightIn(min = 64.dp).focusRing(PillShape),
                        ) { Text("Delete") }
                    }
                }
            }
        }
    }
}
