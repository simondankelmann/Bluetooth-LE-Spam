package de.simon.dankelmann.bluetoothlespam.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamicColorScheme
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager

/**
 * @param seedColorArgb [ThemeManager.THEME_SEED_COLOR_DEVICE] (0, the default) follows the
 * device's real Material You color. Any other ARGB value pins the theme to that seed instead —
 * set via the theme picker (Settings → Theme).
 * @param amoled forces background/surface/surfaceDim/all surfaceContainer* roles to pure black,
 * on top of whichever color scheme was otherwise resolved. Only meaningful with [darkTheme] true.
 */
@Composable
fun SpecterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColorArgb: Int = ThemeManager.THEME_SEED_COLOR_DEVICE,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamic = dynamicColor &&
        seedColorArgb == ThemeManager.THEME_SEED_COLOR_DEVICE &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = remember(darkTheme, useDynamic, seedColorArgb, amoled) {
        val base = if (seedColorArgb != ThemeManager.THEME_SEED_COLOR_DEVICE) {
            // User picked a fixed color in the theme picker — same algorithm as the device
            // dynamic-color path (material-kolor mirrors dynamicLightColorScheme's generator),
            // just seeded from their choice instead of the wallpaper.
            dynamicColorScheme(seedColor = Color(seedColorArgb), isDark = darkTheme)
        } else if (useDynamic) {
            try {
                // Real Material You: the device's wallpaper-derived scheme flows through
                // unmodified, including background/surfaceDim — no brand-navy override here.
                // Forcing a fixed navy on this path would defeat the point of dynamic color
                // (user explicitly wants the real device palette to drive the background).
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (e: Exception) {
                // Dynamic color extraction can fail on some OEM skins — fall back to the
                // material-kolor-derived static scheme rather than crashing (plan Failure Modes).
                if (darkTheme) DarkColors else LightColors
            }
        } else {
            // No real device palette available below API 31 — the static brand-seeded scheme
            // (with its navy background) is the only sensible fallback here.
            if (darkTheme) DarkColors else LightColors
        }

        if (amoled && darkTheme) {
            // Matches KernelSU's amoledBackground() — force every background/surface role to
            // pure black regardless of which path generated `base`, on top of it.
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceDim = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color.Black,
                surfaceContainer = Color.Black,
                surfaceContainerHigh = Color.Black,
                surfaceContainerHighest = Color.Black,
            )
        } else {
            base
        }
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = SpecterShapes,
            typography = SpecterTypography,
            content = content,
        )
    }
}
