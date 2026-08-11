package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Preset seed colors for the theme picker (plan: theme picker, KernelSU-style). The brand blue
 * leads the list since it's this app's own identity; the rest is a standard Material palette
 * spread so there's a recognizable option in every hue.
 */
val themeSeedColorOptions: List<Int> = listOf(
    BrandSeed,
    Color(0xFFF44336), // red
    Color(0xFFE91E63), // pink
    Color(0xFF9C27B0), // purple
    Color(0xFF3F51B5), // indigo
    Color(0xFF2196F3), // blue
    Color(0xFF00BCD4), // cyan
    Color(0xFF009688), // teal
    Color(0xFF4CAF50), // green
    Color(0xFFFFC107), // amber
    Color(0xFFFF9800), // orange
    Color(0xFF795548), // brown
).map { it.toArgb() }
