package de.simon.dankelmann.bluetoothlespam.Helpers

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.R

class ThemeManager private constructor() {

    companion object {
        const val THEME_MODE_KEY = "theme_mode"
        private const val THEME_MODE_DEFAULT = "default"
        private const val THEME_MODE_LIGHT = "light"
        private const val THEME_MODE_DARK = "dark"
        // OLED forces night mode (same as Dark) plus pure-black surfaces — see isOledActive().
        const val THEME_MODE_OLED = "oled"

        // 0 is a sentinel meaning "use the device's Material You color" (the default) —
        // never a real seed since alpha=0 isn't a usable color anyway.
        const val THEME_SEED_COLOR_KEY = "theme_seed_color"
        const val THEME_SEED_COLOR_DEVICE = 0

        private var instance: ThemeManager? = null

        fun getInstance(): ThemeManager {
            if (instance == null) {
                instance = ThemeManager()
            }
            return instance!!
        }
    }

    fun getSeedColor(context: Context): Int {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getInt(THEME_SEED_COLOR_KEY, THEME_SEED_COLOR_DEVICE)
    }

    fun setSeedColor(context: Context, argb: Int) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putInt(THEME_SEED_COLOR_KEY, argb).apply()
    }

    fun applyTheme(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val themeMode = preferences.getString(THEME_MODE_KEY, THEME_MODE_DEFAULT)

        val mode = when (themeMode) {
            THEME_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_MODE_DARK, THEME_MODE_OLED -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun isOledActive(context: Context): Boolean {
        return getTheme(context) == THEME_MODE_OLED
    }

    fun setTheme(context: Context, themeMode: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString(THEME_MODE_KEY, themeMode).apply()
        applyTheme(context)
    }

    fun getTheme(context: Context): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(THEME_MODE_KEY, THEME_MODE_DEFAULT) ?: THEME_MODE_DEFAULT
    }

    fun getThemeString(context: Context): String {
        val current = getTheme(context)
        val resId = when (current) {
            THEME_MODE_LIGHT -> R.string.preference_theme_mode_light
            THEME_MODE_DARK -> R.string.preference_theme_mode_dark
            THEME_MODE_OLED -> R.string.preference_theme_mode_oled
            else -> R.string.preference_theme_mode_follow_system
        }
        return context.getString(resId)
    }
}
