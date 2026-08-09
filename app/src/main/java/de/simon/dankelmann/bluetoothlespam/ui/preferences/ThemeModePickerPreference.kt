package de.simon.dankelmann.bluetoothlespam.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTheme
import de.simon.dankelmann.bluetoothlespam.ui.theme.ThemeModeOption
import de.simon.dankelmann.bluetoothlespam.ui.theme.ThemeModePicker

/** Embeds the Compose [ThemeModePicker] segmented control, same pattern as [ThemePickerPreference]. */
class ThemeModePickerPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    init {
        layoutResource = R.layout.preference_theme_mode_picker
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val composeView = holder.findViewById(R.id.themeModePickerComposeView) as ComposeView
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            val currentMode = ThemeManager.getInstance().getTheme(context)
            var selected by remember {
                mutableStateOf(ThemeModeOption.entries.find { it.prefValue == currentMode } ?: ThemeModeOption.DEVICE)
            }
            val isOled = selected == ThemeModeOption.OLED
            val seedColorArgb = ThemeManager.getInstance().getSeedColor(context)

            SpecterTheme(
                darkTheme = isOled || isSystemInDarkTheme(),
                seedColorArgb = seedColorArgb,
                amoled = isOled,
            ) {
                ThemeModePicker(
                    selected = selected,
                    onSelected = { option ->
                        selected = option
                        ThemeManager.getInstance().setTheme(context, option.prefValue)
                    },
                )
            }
        }
    }
}
