package Com.fire_AI.`in`.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * One text input with two interchangeable keyboards:
 *  - Fire AI keyboard: a focusable box plus an in-app keyboard that never triggers the system IME
 *    (the layout stays put; only the space the caller gives this component changes).
 *  - System keyboard: a normal TextField. Callers apply imePadding() once, on the container that should shrink.
 * Pressing select/tap on the Fire AI box opens its keyboard; Back or Hide closes it and focus returns to the box.
 * A real USB/Bluetooth keyboard types straight into the box without any on-screen keyboard.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TextEntry(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    useFireKeyboard: Boolean,
    isTv: Boolean,
    keyboardVisible: Boolean,
    onKeyboardVisibleChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    submitLabel: String,
    modifier: Modifier = Modifier,
    secret: Boolean = false,
    singleLine: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val inputRequester = remember { FocusRequester() }
    val keyRequester = remember { FocusRequester() }
    val shape = MaterialTheme.shapes.extraLarge
    var keyboardWasShown by remember { mutableStateOf(false) }

    // Move focus into the keyboard when it opens, and back to the input box when it closes.
    LaunchedEffect(keyboardVisible, useFireKeyboard) {
        if (useFireKeyboard) {
            if (keyboardVisible) {
                keyboardWasShown = true
                runCatching { keyRequester.requestFocus() }
            } else if (keyboardWasShown) {
                runCatching { inputRequester.requestFocus() }
            }
        }
    }

    // System keyboard: read the real IME state from window insets and restore focus when it closes.
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (!useFireKeyboard) {
            if (imeWasVisible && !imeVisible) runCatching { inputRequester.requestFocus() }
            imeWasVisible = imeVisible
        }
    }

    // Back closes the in-app keyboard first, before anything else.
    BackHandler(enabled = useFireKeyboard && keyboardVisible) { onKeyboardVisibleChange(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (useFireKeyboard) {
                FireInputBox(
                    value = value,
                    placeholder = placeholder,
                    secret = secret,
                    isTv = isTv,
                    shape = shape,
                    requester = inputRequester,
                    onChange = onValueChange,
                    onOpen = { onKeyboardVisibleChange(true) },
                    onSubmit = onSubmit,
                    modifier = Modifier.weight(1f),
                )
            } else {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { e -> handlePhysicalKey(e, value, onValueChange, onSubmit, typeChars = false) }
                        .focusRing(shape)
                        .focusRequester(inputRequester),
                    placeholder = { Text(placeholder) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = shape,
                    singleLine = singleLine,
                    maxLines = if (singleLine) 1 else 4,
                    visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (secret) KeyboardType.Password else KeyboardType.Text,
                        autoCorrectEnabled = !secret,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }
            trailing()
        }

        if (useFireKeyboard && keyboardVisible) {
            FireKeyboard(
                isTv = isTv,
                submitLabel = submitLabel,
                firstKeyFocus = keyRequester,
                onChar = { onValueChange(value + it) },
                onBackspace = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) },
                onPaste = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = cm.primaryClip
                    val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(context).toString() else ""
                    if (text.isNotEmpty()) onValueChange(value + text.replace('\n', ' '))
                },
                onSubmit = {
                    onSubmit()
                    onKeyboardVisibleChange(false)
                },
                onHide = { onKeyboardVisibleChange(false) },
            )
        }
    }
}

@Composable
private fun FireInputBox(
    value: String,
    placeholder: String,
    secret: Boolean,
    isTv: Boolean,
    shape: Shape,
    requester: FocusRequester,
    onChange: (String) -> Unit,
    onOpen: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .heightIn(min = if (isTv) 64.dp else 56.dp)
            .clip(shape)
            .background(if (focused) scheme.surfaceContainerHigh else scheme.surfaceContainer)
            .onPreviewKeyEvent { e -> handlePhysicalKey(e, value, onChange, onSubmit, typeChars = true) }
            .focusRing(shape)
            .onFocusChanged { focused = it.isFocused }
            .focusRequester(requester)
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
        } else {
            val tail = if (value.length > 240) value.takeLast(240) else value
            val shown = if (secret) "\u2022".repeat(tail.length) else tail
            Text(
                text = (if (value.length > 240) "..." else "") + shown + if (focused) "|" else "",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
                maxLines = 4,
            )
        }
    }
}

/**
 * Physical keyboards only (checked through the device's reported keyboard type, so a TV remote never types).
 * Enter submits; when [typeChars] is true letters are appended too. Consumes both key-down and key-up for the keys
 * it handles so the box's click handler doesn't also fire.
 */
private fun handlePhysicalKey(
    e: KeyEvent,
    value: String,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
    typeChars: Boolean,
): Boolean {
    if (!e.fromAlphabeticKeyboard()) return false
    val down = e.type == KeyEventType.KeyDown
    val code = e.utf16CodePoint
    return when {
        e.key == Key.Enter || e.key == Key.NumPadEnter -> { if (down) onSubmit(); true }
        !typeChars -> false
        e.key == Key.Backspace -> { if (down && value.isNotEmpty()) onChange(value.dropLast(1)); true }
        e.isCtrlPressed || e.isAltPressed || e.isMetaPressed -> false
        code >= 32 && code != 127 -> {
            if (down) onChange(value + String(Character.toChars(code)))
            true
        }
        else -> false
    }
}
