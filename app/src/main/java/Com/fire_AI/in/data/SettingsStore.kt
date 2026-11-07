package Com.fire_AI.`in`.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Empty until the user picks a model from the verified list in Models. */
    var model: String
        get() = prefs.getString("model", "") ?: ""
        set(value) { prefs.edit().putString("model", value).apply() }

    var temperature: Float
        get() = prefs.getFloat("temperature", 0.7f)
        set(value) { prefs.edit().putFloat("temperature", value).apply() }

    var streaming: Boolean
        get() = prefs.getBoolean("streaming", true)
        set(value) { prefs.edit().putBoolean("streaming", value).apply() }

    /** "auto" (Fire AI keyboard on TV, system keyboard on phones), "fire" or "system". */
    var keyboardMode: String
        get() = prefs.getString("keyboardMode", "auto") ?: "auto"
        set(value) { prefs.edit().putString("keyboardMode", value).apply() }

    var autoSendVoice: Boolean
        get() = prefs.getBoolean("autoSendVoice", false)
        set(value) { prefs.edit().putBoolean("autoSendVoice", value).apply() }
}
