package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class StatusTone { WARNING, SUCCESS }

/**
 * "Something is happening right now" indicator (active attack, live scan) — shared by the
 * Advertisement screen and Spam Detector so the ExtendedColors wiring and a11y semantics live
 * in one place instead of two ad-hoc implementations (eng review DRY finding).
 *
 * Always renders ExtendedColors, never a dynamic-color scheme role — an active-attack/live-scan
 * signal must read as "something is happening" regardless of what pastel a wallpaper generated.
 */
@Composable
fun StatusBadge(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val extendedColors = LocalExtendedColors.current
    val (background, onColor) = when (tone) {
        StatusTone.WARNING -> extendedColors.warning to extendedColors.onWarning
        StatusTone.SUCCESS -> extendedColors.success to extendedColors.onSuccess
    }

    Text(
        text = label,
        color = onColor,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .semantics { contentDescription = label }
            .background(color = background, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
