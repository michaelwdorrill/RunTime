package com.runtime.app.data

import android.content.Context
import androidx.core.content.edit

class RunTimeSettings(context: Context) {
    val preferences = context.getSharedPreferences("runtime_settings", Context.MODE_PRIVATE)
    var consent: Boolean
        get() = preferences.getBoolean("accessibility_disclosure_v1", false)
        set(value) { preferences.edit { putBoolean("accessibility_disclosure_v1", value) } }
    var enabled: Boolean
        get() = preferences.getBoolean("protection_enabled", false)
        set(value) { preferences.edit { putBoolean("protection_enabled", value) } }
    var youtubeEnabled: Boolean
        get() = preferences.getBoolean("youtube_enabled", true)
        set(value) { preferences.edit { putBoolean("youtube_enabled", value) } }
    var haptics: Boolean
        get() = preferences.getBoolean("haptics", false)
        set(value) { preferences.edit { putBoolean("haptics", value) } }
    val shouldProtect get() = consent && enabled && youtubeEnabled
}
