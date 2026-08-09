package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme

// Brand seed, mirrors app/src/main/res/values/colors.xml blue_normal.
val BrandSeed = Color(0xFF2D5EED)

// Dark-mode background identity, mirrors app/src/main/res/values-night/colors.xml background_color.
// Applied on BOTH the dynamic-color path and the static fallback path (see Theme.kt) so the
// brand's navy dark mode survives regardless of API level / dynamic color availability.
val NavyBackground = Color(0xFF1A1D29)

private val LightColorsBase: ColorScheme by lazy {
    dynamicColorScheme(seedColor = BrandSeed, isDark = false)
}

private val DarkColorsBase: ColorScheme by lazy {
    dynamicColorScheme(seedColor = BrandSeed, isDark = true).copy(
        background = NavyBackground,
        surfaceDim = NavyBackground,
    )
}

/** Static fallback ColorScheme for API < 31, where real Material You dynamic color is unavailable. */
val LightColors: ColorScheme get() = LightColorsBase

/** Static fallback ColorScheme for API < 31. Navy background/surfaceDim baked in. */
val DarkColors: ColorScheme get() = DarkColorsBase

/**
 * Semantic colors that don't map cleanly onto MD3's core roles (see plan §2) — kept outside
 * MaterialTheme.colorScheme so they never get silently reinterpreted by dynamic color.
 */
@Immutable
data class ExtendedColors(
    val warning: Color,
    val onWarning: Color,
    val success: Color,
    val onSuccess: Color,
)

val LightExtendedColors = ExtendedColors(
    warning = Color(0xFFC19C00),
    onWarning = Color(0xFF000000),
    success = Color(0xFF13A10E),
    onSuccess = Color(0xFFFFFFFF),
)

val DarkExtendedColors = ExtendedColors(
    warning = Color(0xFFC19C00),
    onWarning = Color(0xFF000000),
    success = Color(0xFF13A10E),
    onSuccess = Color(0xFFFFFFFF),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
