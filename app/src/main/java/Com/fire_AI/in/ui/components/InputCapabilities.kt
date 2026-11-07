package Com.fire_AI.`in`.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.view.InputDevice
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.utf16CodePoint

/** What the device actually reports, read from Android's input system rather than guessed from screen size. */
data class InputCaps(
    val touchscreen: Boolean,
    val mouse: Boolean,
    val keyboard: Boolean,
    val dpad: Boolean,
    val gamepad: Boolean,
)

fun detectInputCaps(context: Context): InputCaps {
    var touch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    var mouse = false
    var keyboard = false
    var dpad = false
    var gamepad = false
    for (id in InputDevice.getDeviceIds()) {
        val d = InputDevice.getDevice(id) ?: continue
        val s = d.sources
        fun has(mask: Int) = (s and mask) == mask
        if (has(InputDevice.SOURCE_TOUCHSCREEN)) touch = true
        if (has(InputDevice.SOURCE_MOUSE)) mouse = true
        if (has(InputDevice.SOURCE_DPAD)) dpad = true
        if (has(InputDevice.SOURCE_GAMEPAD)) gamepad = true
        if (has(InputDevice.SOURCE_KEYBOARD) && d.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC) keyboard = true
    }
    return InputCaps(touch, mouse, keyboard, dpad, gamepad)
}

/** True only for a real typing keyboard. A TV remote that reports as a keyboard is not alphabetic. */
fun KeyEvent.fromAlphabeticKeyboard(): Boolean =
    utf16CodePoint != 0 || key == Key.Backspace || key == Key.Enter || key == Key.NumPadEnter
