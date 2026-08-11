package de.simon.dankelmann.bluetoothlespam.Helpers

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsKeys
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository
import de.simon.dankelmann.bluetoothlespam.R

class ThemeManager private constructor() {

    companion object {
        const val THEME_MODE_KEY = "theme_mode"
        const val THEME_MODE_DEFAULT = "default"
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
        return SettingsRepository.getInstance(context).current[SettingsKeys.THEME_SEED_COLOR]
            ?: THEME_SEED_COLOR_DEVICE
    }

    fun setSeedColor(context: Context, argb: Int) {
        SettingsRepository.getInstance(context).setThemeSeedColorAsync(argb)
    }

    fun applyTheme(context: Context) {
        applyThemeMode(getTheme(context))
    }

    private fun applyThemeMode(themeMode: String) {
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
        SettingsRepository.getInstance(context).setThemeModeAsync(themeMode)
        // Apply the new mode directly rather than re-reading — the DataStore write above is
        // async, so a read-back here could still observe the pre-write value.
        applyThemeMode(themeMode)
    }

    fun getTheme(context: Context): String {
        return SettingsRepository.getInstance(context).current[SettingsKeys.THEME_MODE] ?: THEME_MODE_DEFAULT
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

    fun isDynamicColorEnabled(context: Context): Boolean {
        return SettingsRepository.getInstance(context).current[SettingsKeys.DYNAMIC_COLOR_ENABLED] ?: true
    }

    fun setDynamicColorEnabled(context: Context, enabled: Boolean) {
        SettingsRepository.getInstance(context).setDynamicColorEnabledAsync(enabled)
    }

    fun isBlurEnabled(context: Context): Boolean {
        return SettingsRepository.getInstance(context).current[SettingsKeys.BLUR_ENABLED] ?: true
    }

    fun setBlurEnabled(context: Context, enabled: Boolean) {
        SettingsRepository.getInstance(context).setBlurEnabledAsync(enabled)
    }
}
