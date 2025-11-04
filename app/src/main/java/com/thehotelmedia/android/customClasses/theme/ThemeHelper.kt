package com.thehotelmedia.android.customClasses.theme

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {

    private const val PREF_NAME = "Thm_theme_pref"
    private const val KEY_THEME = "Thm_key_theme"
    private const val KEY_THEME_SET_ONCE = "Thm_key_theme_set_once"

    fun applyTheme(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val isThemeSetOnce = sharedPreferences.getBoolean(KEY_THEME_SET_ONCE, false)

        if (!isThemeSetOnce) {
            // Pehli baar system theme check karo
            val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isSystemDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES

            // System theme ko save karo
            sharedPreferences.edit()
                .putBoolean(KEY_THEME, isSystemDarkMode)
                .putBoolean(KEY_THEME_SET_ONCE, true) // Ab ek baar theme set ho chuki hai
                .apply()
        }

        val isDarkMode = sharedPreferences.getBoolean(KEY_THEME, false)
        val mode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun toggleTheme(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean(KEY_THEME, false)

        val newMode = !isDarkMode
        sharedPreferences.edit()
            .putBoolean(KEY_THEME, newMode)
            .apply()

        return newMode
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_THEME, false)
    }
}
