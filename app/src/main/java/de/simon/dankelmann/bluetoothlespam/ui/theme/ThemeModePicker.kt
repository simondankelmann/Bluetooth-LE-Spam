package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class ThemeModeOption(val prefValue: String, val icon: ImageVector) {
    DARK("dark", Icons.Filled.Brightness3),
    DEVICE("default", Icons.Filled.PhoneAndroid),
    LIGHT("light", Icons.Filled.Brightness7),
    OLED("oled", Icons.Filled.Brightness1),
}

/**
 * Connected segmented control for theme mode (Dark / Device / Light / OLED) — visual style
 * requested to match a reference screenshot: one pill-shaped outer container, flat shared edges
 * between segments, selected segment filled with a checkmark next to its icon.
 */
@Composable
fun ThemeModePicker(
    selected: ThemeModeOption,
    onSelected: (ThemeModeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outerShape = RoundedCornerShape(28.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(outerShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, outerShape),
    ) {
        ThemeModeOption.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    )
                    .clickable { onSelected(option) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                } else {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
