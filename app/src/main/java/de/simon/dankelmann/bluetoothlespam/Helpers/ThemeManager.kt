package de.simon.dankelmann.bluetoothlespam.Helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

class ThemeManager private constructor() {
    companion object {
        private const val THEME_MODE_KEY = "theme_mode"
        private const val THEME_MODE_DEFAULT = "default"
        private const val THEME_MODE_LIGHT = "light"
        private const val THEME_MODE_DARK = "dark"
        
        private var instance: ThemeManager? = null
        
        fun getInstance(): ThemeManager {
            if (instance == null) {
                instance = ThemeManager()
            }
            return instance!!
        }
    }
    
    fun applyTheme(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val themeMode = preferences.getString(THEME_MODE_KEY, THEME_MODE_DEFAULT)
        
        when (themeMode) {
            THEME_MODE_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_MODE_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    fun setTheme(context: Context, themeMode: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString(THEME_MODE_KEY, themeMode).apply()
        applyTheme(context)
    }
}