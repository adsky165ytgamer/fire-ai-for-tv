package com.mangaauthority.tvai.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var model: String
        get() = prefs.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) { prefs.edit().putString("model", value).apply() }

    var temperature: Float
        get() = prefs.getFloat("temperature", 0.7f)
        set(value) { prefs.edit().putFloat("temperature", value).apply() }

    var streaming: Boolean
        get() = prefs.getBoolean("streaming", true)
        set(value) { prefs.edit().putBoolean("streaming", value).apply() }

    companion object {
        /** Used until the user picks another model. The available list is fetched from the API in Settings. */
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }
}
