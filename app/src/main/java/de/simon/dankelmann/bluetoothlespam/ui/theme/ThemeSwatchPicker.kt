package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.materialkolor.dynamicColorScheme
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager
import de.simon.dankelmann.bluetoothlespam.R

/**
 * Theme picker (design borrowed from KernelSU's Color Palette screen — a horizontal row of
 * circular swatches, each a live preview of what that seed color's MD3 scheme looks like, with
 * a checkmark on the selected one). The first swatch is "Device" — the app's default — which
 * keeps real Material You dynamic color; every other swatch pins a fixed seed color instead.
 *
 * [selectedSeedColor] is [ThemeManager.THEME_SEED_COLOR_DEVICE] (0) when following the device.
 */
@Composable
fun ThemeSwatchPicker(
    selectedSeedColor: Int,
    onSeedColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DeviceColorSwatch(
                isSelected = selectedSeedColor == ThemeManager.THEME_SEED_COLOR_DEVICE,
                onClick = { onSeedColorSelected(ThemeManager.THEME_SEED_COLOR_DEVICE) },
            )
        }

        items(themeSeedColorOptions) { seedColorArgb ->
            ColorSwatch(
                seedColor = Color(seedColorArgb),
                isDark = isDark,
                isSelected = selectedSeedColor == seedColorArgb,
                onClick = { onSeedColorSelected(seedColorArgb) },
            )
        }
    }
}

@Composable
private fun DeviceColorSwatch(isSelected: Boolean, onClick: () -> Unit) {
    // Reflects whatever the CURRENTLY active resolved scheme is (real device dynamic color on
    // API 31+, the static brand fallback below that) — accurate rather than a guess.
    val colorScheme = MaterialTheme.colorScheme
    SwatchSurface(
        primaryContainer = colorScheme.primaryContainer,
        tertiaryContainer = colorScheme.tertiaryContainer,
        selectedIndicatorColor = colorScheme.primary,
        onIndicatorColor = colorScheme.onPrimary,
        isSelected = isSelected,
        onClick = onClick,
        centerContent = {
            if (!isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Smartphone,
                    contentDescription = stringResource(R.string.theme_color_device),
                    tint = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    )
}

@Composable
private fun ColorSwatch(seedColor: Color, isDark: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val colorScheme = dynamicColorScheme(seedColor = seedColor, isDark = isDark)
    SwatchSurface(
        primaryContainer = colorScheme.primaryContainer,
        tertiaryContainer = colorScheme.tertiaryContainer,
        selectedIndicatorColor = colorScheme.primary,
        onIndicatorColor = colorScheme.onPrimary,
        isSelected = isSelected,
        onClick = onClick,
        centerContent = {},
    )
}

@Composable
private fun SwatchSurface(
    primaryContainer: Color,
    tertiaryContainer: Color,
    selectedIndicatorColor: Color,
    onIndicatorColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    centerContent: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(64.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(64.dp)) {
                drawArc(color = primaryContainer, startAngle = 180f, sweepAngle = 180f, useCenter = true)
                drawArc(color = tertiaryContainer, startAngle = 0f, sweepAngle = 180f, useCenter = true)
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1f, label = "swatch-scale")
            Box(
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(2.dp, selectedIndicatorColor, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(selectedIndicatorColor, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = onIndicatorColor,
                                modifier = Modifier.align(Alignment.Center).size(14.dp),
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    centerContent()
                }
            }
        }
    }
}
