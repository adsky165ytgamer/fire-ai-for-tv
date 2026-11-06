package Com.fire_AI.`in`.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------- Parsing (pure Kotlin, runs off the main thread) ----------

sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class ListItem(val marker: String, val indent: Int, val text: String) : MdBlock
    data class Code(val code: String) : MdBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
    data class Quote(val text: String) : MdBlock
    data object Rule : MdBlock
}

private val headingRe = Regex("^(#{1,6})\\s+(.*)$")
private val ruleRe = Regex("^(-{3,}|\\*{3,}|_{3,})$")
private val listRe = Regex("^(\\s*)([-*+]|\\d{1,3}[.)])\\s+(.*)$")
private val tableSepRe = Regex("^\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)*\\|?$")

private fun splitRow(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split('|').map { it.trim() }

private fun isTableStart(lines: List<String>, i: Int): Boolean =
    lines[i].contains('|') && i + 1 < lines.size &&
        lines[i + 1].contains('-') && tableSepRe.matches(lines[i + 1].trim())

private fun isBlockStart(lines: List<String>, i: Int): Boolean {
    val t = lines[i].trim()
    return t.isEmpty() || t.startsWith("```") || headingRe.matches(t) || ruleRe.matches(t) ||
        listRe.matches(lines[i]) || t.startsWith(">") || isTableStart(lines, i)
}

fun parseMarkdown(src: String): List<MdBlock> {
    val lines = src.replace("\r\n", "\n").split('\n')
    val out = ArrayList<MdBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val t = line.trim()
        when {
            t.isEmpty() -> i++
            t.startsWith("```") -> {
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    if (code.isNotEmpty()) code.append('\n')
                    code.append(lines[i])
                    i++
                }
                i++ // closing fence (or end of text while still streaming)
                out.add(MdBlock.Code(code.toString()))
            }
            headingRe.matches(t) -> {
                val m = headingRe.find(t)!!
                out.add(MdBlock.Heading(m.groupValues[1].length, m.groupValues[2]))
                i++
            }
            ruleRe.matches(t) -> { out.add(MdBlock.Rule); i++ }
            isTableStart(lines, i) -> {
                val header = splitRow(line)
                i += 2
                val rows = ArrayList<List<String>>()
                while (i < lines.size && lines[i].contains('|') && lines[i].isNotBlank()) {
                    rows.add(splitRow(lines[i]))
                    i++
                }
                out.add(MdBlock.Table(header, rows))
            }
            listRe.matches(line) -> {
                val m = listRe.find(line)!!
                val marker = m.groupValues[2].let { if (it[0].isDigit()) it else "\u2022" }
                out.add(MdBlock.ListItem(marker, m.groupValues[1].length / 2, m.groupValues[3]))
                i++
            }
            t.startsWith(">") -> {
                val sb = StringBuilder()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    if (sb.isNotEmpty()) sb.append('\n')
                    sb.append(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                out.add(MdBlock.Quote(sb.toString()))
            }
            else -> {
                val sb = StringBuilder(t)
                i++
                while (i < lines.size && !isBlockStart(lines, i)) {
                    sb.append('\n').append(lines[i].trim())
                    i++
                }
                out.add(MdBlock.Paragraph(sb.toString()))
            }
        }
    }
    return out
}

// ---------- Inline formatting ----------

private class InlineColors(val codeBg: Color, val codeFg: Color, val link: Color)

private fun inlineText(text: String, c: InlineColors, onLink: (String) -> Unit): AnnotatedString =
    buildAnnotatedString { appendInline(text, c, onLink) }

private fun AnnotatedString.Builder.appendInline(text: String, c: InlineColors, onLink: (String) -> Unit) {
    val plain = StringBuilder()
    fun flush() {
        if (plain.isNotEmpty()) {
            append(plain.toString())
            plain.setLength(0)
        }
    }
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        if (ch == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i) {
                flush()
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = c.codeBg, color = c.codeFg)) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        } else if (ch == '*' && text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end > i + 2) {
                flush()
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(text.substring(i + 2, end), c, onLink) }
                i = end + 2
                continue
            }
        } else if (ch == '*' && i + 1 < text.length && !text[i + 1].isWhitespace()) {
            val end = text.indexOf('*', i + 1)
            if (end > i + 1 && !text[end - 1].isWhitespace()) {
                flush()
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInline(text.substring(i + 1, end), c, onLink) }
                i = end + 1
                continue
            }
        } else if (ch == '[') {
            val close = text.indexOf("](", i + 1)
            if (close > 0) {
                val urlEnd = text.indexOf(')', close + 2)
                if (urlEnd > close) {
                    flush()
                    val label = text.substring(i + 1, close)
                    val url = text.substring(close + 2, urlEnd)
                    val link = LinkAnnotation.Url(
                        url,
                        TextLinkStyles(SpanStyle(color = c.link, textDecoration = TextDecoration.Underline)),
                    ) { onLink(url) }
                    withLink(link) { append(label) }
                    i = urlEnd + 1
                    continue
                }
            }
        }
        plain.append(ch)
        i++
    }
    flush()
}

// ---------- Rendering ----------

/** Renders a finished model reply. Parsing happens once, off the main thread. */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks by produceState<List<MdBlock>?>(initialValue = null, key1 = text) {
        value = withContext(Dispatchers.Default) { parseMarkdown(text) }
    }
    val parsed = blocks
    if (parsed == null) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = modifier)
        return
    }
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val inlineColors = remember(colors) { InlineColors(colors.surfaceContainerHighest, colors.onSurface, colors.primary) }
    val onLink: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        parsed.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = inlineText(block.text, inlineColors, onLink),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                is MdBlock.Paragraph -> Text(
                    text = inlineText(block.text, inlineColors, onLink),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                )
                is MdBlock.ListItem -> Row(Modifier.padding(start = (block.indent * 20).dp)) {
                    Text(
                        block.marker,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.width(32.dp),
                    )
                    Text(
                        text = inlineText(block.text, inlineColors, onLink),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
                is MdBlock.Code -> Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = colors.surfaceContainerLowest,
                ) {
                    // Wraps instead of scrolling sideways: a D-pad remote can't scroll horizontally.
                    Text(
                        text = block.code,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurface,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                is MdBlock.Table -> TableBlock(block, inlineColors, onLink)
                is MdBlock.Quote -> Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.width(4.dp).fillMaxHeight().background(colors.primary))
                    Text(
                        text = inlineText(block.text, inlineColors, onLink),
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 14.dp),
                    )
                }
                MdBlock.Rule -> HorizontalDivider(color = colors.outlineVariant)
            }
        }
    }
}

@Composable
private fun TableBlock(table: MdBlock.Table, colors: InlineColors, onLink: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val columns = table.header.size
    Surface(shape = MaterialTheme.shapes.medium, color = scheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
        Column {
            @Composable
            fun RowOf(cells: List<String>, header: Boolean) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    for (c in 0 until columns) {
                        Text(
                            text = inlineText(cells.getOrElse(c) { "" }, colors, onLink),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                            color = scheme.onSurface,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                        )
                    }
                }
            }
            RowOf(table.header, true)
            HorizontalDivider(color = scheme.outlineVariant)
            table.rows.forEach { RowOf(it, false) }
        }
    }
}
