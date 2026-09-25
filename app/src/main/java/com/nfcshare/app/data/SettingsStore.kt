package com.nfcshare.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Settings(val theme: String = "Sistema", val vibration: Boolean = true, val sound: Boolean = false, val history: Boolean = true, val dynamicColor: Boolean = false, val advanced: Boolean = false)
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(Settings(prefs.getString("theme", "Sistema")!!, prefs.getBoolean("vibration", true), prefs.getBoolean("sound", false), prefs.getBoolean("history", true), prefs.getBoolean("dynamic", false), prefs.getBoolean("advanced", false)))
    val settings = state.asStateFlow()
    fun update(value: Settings) {
        prefs.edit().putString("theme", value.theme).putBoolean("vibration", value.vibration).putBoolean("sound", value.sound).putBoolean("history", value.history).putBoolean("dynamic", value.dynamicColor).putBoolean("advanced", value.advanced).apply()
        state.value = value
    }
}
