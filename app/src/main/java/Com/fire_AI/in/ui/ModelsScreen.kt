package Com.fire_AI.`in`.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import Com.fire_AI.`in`.data.FreeTier
import Com.fire_AI.`in`.data.ModelInfo
import Com.fire_AI.`in`.ui.components.focusRing

private val PillShape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModelsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val all = state.models
    val free = all.orEmpty().filter { FreeTier.isFree(it.id) }
    val hidden = all.orEmpty().size - free.size
    val returnedIds = all.orEmpty().map { it.id }.toSet()
    val notAvailable = if (all == null) emptyList() else FreeTier.chatModels.filter { it !in returnedIds }

    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            FilledTonalButton(
                onClick = onBack,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
            ) { Text("Back") }
            Text("Models", style = MaterialTheme.typography.headlineLargeEmphasized, color = scheme.onSurface)
            FilledTonalButton(
                onClick = onRefresh,
                enabled = state.hasKey && !state.busy,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
            ) { Text(if (state.busy) "Loading..." else "Refresh") }
        }

        state.status?.let {
            Text(it.text, style = MaterialTheme.typography.bodyLarge, color = if (it.isError) scheme.error else scheme.primary)
        }

        if (!state.hasKey) {
            ModelCard("No API key") {
                Text("Add your Gemini API key in Settings to see the models your key can use.", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                Button(
                    onClick = onOpenSettings,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 56.dp).focusRing(PillShape),
                ) { Text("Open Settings") }
            }
        } else if (all == null) {
            Text(if (state.busy) "Loading models..." else "Select Refresh to load models.", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
        } else {
            ModelCard("Free tier (${free.size})") {
                Text("Current: ${state.model.ifBlank { "none selected" }}", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                if (state.model.isNotBlank() && !FreeTier.isFree(state.model)) {
                    Text(
                        "\"${state.model}\" isn't on the free tier list and may be billed or unavailable. Pick a free model below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.error,
                    )
                }
                if (free.isEmpty()) {
                    Text("None of the models your key can use are on the free tier list.", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
                free.forEach { m -> ModelRow(m, selected = m.id == state.model, onSelect = onSelect) }
            }

            if (hidden > 0 || notAvailable.isNotEmpty()) {
                ModelCard("Not shown") {
                    if (hidden > 0) {
                        Text(
                            "$hidden other models are hidden because they aren't on Google's published free tier (paid, preview-only or not text chat).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    if (notAvailable.isNotEmpty()) {
                        Text(
                            "Free-tier models your key can't use right now: ${notAvailable.joinToString(", ")}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Text(
            "The free list comes from Google's pricing page (ai.google.dev/gemini-api/docs/pricing, last updated ${FreeTier.VERIFIED_ON}). " +
                "Free access can differ by region and account and can change, and the API's model list doesn't say what is free. " +
                "If a quota or rate limit is hit, Fire AI shows the error and never switches models for you.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModelRow(m: ModelInfo, selected: Boolean, onSelect: (String) -> Unit) {
    val details = buildList {
        if (m.inputTokenLimit > 0) add("input up to %,d tokens".format(m.inputTokenLimit))
        if (m.outputTokenLimit > 0) add("output up to %,d".format(m.outputTokenLimit))
        if (m.thinking) add("thinking")
        add("streaming")
    }.joinToString("  \u2022  ")
    val content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        Column(Modifier.fillMaxWidth()) {
            Text((if (selected) "\u2713  " else "") + m.displayName + "  (" + m.id + ")")
            Text(details, style = MaterialTheme.typography.labelMedium)
        }
    }
    val mod = Modifier.fillMaxWidth().heightIn(min = 64.dp).focusRing(PillShape)
    if (selected) Button(onClick = { onSelect(m.id) }, shapes = ButtonDefaults.shapes(), modifier = mod, content = content)
    else FilledTonalButton(onClick = { onSelect(m.id) }, shapes = ButtonDefaults.shapes(), modifier = mod, content = content)
}

@Composable
private fun ModelCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}
