package de.simon.dankelmann.bluetoothlespam.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTheme
import de.simon.dankelmann.bluetoothlespam.ui.theme.ThemeSwatchPicker

/**
 * Embeds the Compose [ThemeSwatchPicker] inside the classic AndroidX Preference screen — the
 * swatch row (live two-tone previews, selection animation) doesn't map onto a plain list-item
 * Preference, so this hosts it via ComposeView the same way MainActivity hosts Compose content
 * inside the still-XML screen shell.
 */
class ThemePickerPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    init {
        layoutResource = R.layout.preference_theme_picker
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val composeView = holder.findViewById(R.id.themePickerComposeView) as ComposeView
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            var selectedSeedColor by remember {
                mutableIntStateOf(ThemeManager.getInstance().getSeedColor(context))
            }

            SpecterTheme {
                ThemeSwatchPicker(
                    selectedSeedColor = selectedSeedColor,
                    onSeedColorSelected = { argb ->
                        selectedSeedColor = argb
                        ThemeManager.getInstance().setSeedColor(context, argb)
                    },
                )
            }
        }
    }
}
